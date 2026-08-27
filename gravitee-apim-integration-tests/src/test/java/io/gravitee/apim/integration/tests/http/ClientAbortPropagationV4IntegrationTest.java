/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage that a client abort releases the upstream connection-pool slot held (or awaited) by the aborted
 * request instead of burning it until the endpoint read timeout fires.
 *
 * The APIs under test use a single-connection pool so one leaked slot is directly observable as latency on the next
 * request. Aborts are raw TCP socket closes, matching what the gateway sees when a real caller gives up.
 *
 * Without abort propagation, an aborted-while-queued request is still granted the connection once it frees up and
 * holds it idle until the read timeout (8s here) — the mechanism behind pool-collapse incidents where abandoned
 * requests shed no load and the collapse outlives the backend recovery.
 *
 * Each scenario runs against an HTTP/1.1 and an HTTP/2 endpoint. They are not the same test twice: an HTTP/1.1 slot is
 * a whole connection released by closing it, while an HTTP/2 slot is one stream of a multiplexed connection released
 * by an RST_STREAM, so the release path differs even though the observable contract does not. The HTTP/2 endpoint
 * additionally caps multiplexing at one stream, without which the spare streams of the same connection would hide
 * a leaked one.
 *
 * The three abort points cover the three owners of cancellation in the connector: while queued for the pool and while
 * in flight before the response head, both guarded by {@code doOnDispose} on the connect chain, and during response
 * streaming, guarded by {@code doOnCancel} on the response chunks.
 *
 * Five of the six combinations hold; the sixth is disabled and documents a gap on HTTP/2 — see
 * should_release_pool_slot_when_queued_request_is_aborted_on_an_http2_endpoint.
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientAbortPropagationV4IntegrationTest extends AbstractGatewayTest {

    private static final String HTTP_1_1_API = "/apis/v4/http/clientabort/api-client-abort-single-connection.json";
    private static final String HTTP_2_API = "/apis/v4/http/clientabort/api-client-abort-single-connection-http2.json";

    private static final String BACKEND_PATH = "/endpoint";
    private static final long PROBE_LATENCY_BUDGET_MS = 2500;

    /** Long enough that a slot held for the whole streamed response blows the probe budget several times over. */
    private static final int STREAMED_BODY_DURATION_MS = 6000;
    private static final int STREAMED_BODY_CHUNKS = 20;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi(HTTP_1_1_API)
    void should_release_pool_slot_when_queued_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
        assertQueuedAbortReleasesPoolSlot(httpClient, gateway);
    }

    /**
     * Reports a gap the APIM-14605 fix does not cover rather than asserting the defect: on an HTTP/2 endpoint the
     * release path does run for an abort that happens while the request is queued — the stream granted after the
     * abort is reset, and the connector logs it — but the multiplexing permit it consumed is never returned. The
     * pooled connection is then permanently short one stream: with the single stream this endpoint allows, every
     * later request answers 504 at the 8s readTimeout without ever reaching the backend, and the endpoint only
     * recovers when the 30s idleTimeout evicts the connection. HTTP/1.1 releases the very same slot correctly, and
     * an HTTP/2 abort after the request has been sent does too — it is the reset of a not-yet-sent stream that
     * leaks. Enable once the permit is released.
     */
    @Test
    @Disabled("Gap in APIM-14605 on HTTP/2 endpoints: an abort while queued leaks the stream's multiplexing permit")
    @DeployApi(HTTP_2_API)
    void should_release_pool_slot_when_queued_request_is_aborted_on_an_http2_endpoint(
        HttpClient httpClient,
        GatewayDynamicConfig.HttpConfig gateway
    ) throws Exception {
        assertQueuedAbortReleasesPoolSlot(httpClient, gateway);
    }

    @Test
    @DeployApi(HTTP_1_1_API)
    void should_release_pool_slot_when_in_flight_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
        assertInFlightAbortReleasesPoolSlot(httpClient, gateway);
    }

    @Test
    @DeployApi(HTTP_2_API)
    void should_release_pool_slot_when_in_flight_request_is_aborted_on_an_http2_endpoint(
        HttpClient httpClient,
        GatewayDynamicConfig.HttpConfig gateway
    ) throws Exception {
        assertInFlightAbortReleasesPoolSlot(httpClient, gateway);
    }

    @Test
    @DeployApi(HTTP_1_1_API)
    void should_release_pool_slot_when_request_is_aborted_during_response_streaming(
        HttpClient httpClient,
        GatewayDynamicConfig.HttpConfig gateway
    ) throws Exception {
        assertStreamingAbortReleasesPoolSlot(httpClient, gateway);
    }

    @Test
    @DeployApi(HTTP_2_API)
    void should_release_pool_slot_when_request_is_aborted_during_response_streaming_on_an_http2_endpoint(
        HttpClient httpClient,
        GatewayDynamicConfig.HttpConfig gateway
    ) throws Exception {
        assertStreamingAbortReleasesPoolSlot(httpClient, gateway);
    }

    private void assertQueuedAbortReleasesPoolSlot(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway) throws Exception {
        // First backend call is slow (3s) to keep the single connection busy; every later call answers immediately, so
        // any latency observed on the probe is pool-acquisition wait, not backend time.
        stubSlowThenFastBackend(3000);

        // Occupies the only upstream connection for ~3s.
        var slowRequest = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMap(HttpClientResponse::rxBody)
            .test();
        Thread.sleep(500);

        // Aborted while QUEUED for the connection: it must never be granted the slot (nor reach the backend).
        abortClientRequest(gateway.httpPort(), 300);

        slowRequest.awaitDone(10, TimeUnit.SECONDS).assertComplete();

        // The slot just freed by the slow request must be immediately available to the probe. Without abort
        // propagation the aborted request is granted the slot and holds it idle until the 8s read timeout.
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);

        // The aborted request must not have been dispatched to the backend: slow request + probe only.
        wiremock.verify(2, getRequestedFor(urlPathEqualTo(BACKEND_PATH)));
    }

    private void assertInFlightAbortReleasesPoolSlot(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway) throws Exception {
        stubSlowThenFastBackend(6000);

        // Dispatched to the backend (single connection acquired), then aborted while the backend is still answering.
        abortClientRequest(gateway.httpPort(), 500);

        // The abort must release the connection: the probe must not wait for the backend's 6s answer to the aborted
        // request (nor for the 8s read timeout).
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);
    }

    private void assertStreamingAbortReleasesPoolSlot(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway) throws Exception {
        stubStreamedThenFastBackend();

        // Aborted after the response head has been received, so the slot is held by a response being streamed. From
        // that point the connect chain is complete and only the response chunks' cancellation can release the slot.
        abortClientRequestOnceResponseIsStreaming(gateway.httpPort());

        // Without that release the slot stays busy for the rest of the 6s stream — the client it was being streamed to
        // having already gone.
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);

        // The aborted request did reach the backend here, unlike the queued case: streamed response + probe.
        wiremock.verify(2, getRequestedFor(urlPathEqualTo(BACKEND_PATH)));
    }

    private void stubSlowThenFastBackend(int slowDelayMillis) {
        wiremock.stubFor(
            get(BACKEND_PATH)
                .inScenario("single-slot")
                .whenScenarioStateIs(STARTED)
                .willReturn(ok("slow").withFixedDelay(slowDelayMillis))
                .willSetStateTo("fast")
        );
        stubFastBackend();
    }

    /**
     * First call answers its head immediately then dribbles the body over {@link #STREAMED_BODY_DURATION_MS}, which is
     * what puts the exchange in the response-streaming phase long enough to be aborted there.
     * <p>
     * The content type is what makes the response stream at all: the gateway only streams a body downstream for the
     * content types {@code RequestUtils.hasStreamingContentType} recognises, and aggregates every other response,
     * releasing it in one piece once the backend is done — which would leave no streaming window to abort in.
     */
    private void stubStreamedThenFastBackend() {
        wiremock.stubFor(
            get(BACKEND_PATH)
                .inScenario("single-slot")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    ok("x".repeat(STREAMED_BODY_CHUNKS * 64))
                        .withHeader(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM)
                        .withChunkedDribbleDelay(STREAMED_BODY_CHUNKS, STREAMED_BODY_DURATION_MS)
                )
                .willSetStateTo("fast")
        );
        stubFastBackend();
    }

    private void stubFastBackend() {
        wiremock.stubFor(get(BACKEND_PATH).inScenario("single-slot").whenScenarioStateIs("fast").willReturn(ok("fast")));
    }

    /**
     * Sends a request as a raw TCP client and closes the socket after the given delay — a genuine client abort, seen
     * by the gateway as the downstream connection closing mid-request.
     */
    private void abortClientRequest(int gatewayPort, long holdMillis) throws Exception {
        try (Socket socket = new Socket("localhost", gatewayPort)) {
            writeRequest(socket);
            Thread.sleep(holdMillis);
        }
        awaitCloseObservedByGateway();
    }

    /**
     * Same abort, but held until the response head plus a first body byte have come back, so the exchange is provably
     * past the response head and streaming its body. Sleeping a guessed duration instead would silently degrade into
     * the in-flight case whenever the machine is slow, and pass for the wrong reason.
     */
    private void abortClientRequestOnceResponseIsStreaming(int gatewayPort) throws Exception {
        try (Socket socket = new Socket("localhost", gatewayPort)) {
            socket.setSoTimeout(5000);
            writeRequest(socket);
            readResponseHead(socket);
            assertThat(socket.getInputStream().read()).as("first streamed body byte").isNotEqualTo(-1);
        }
        awaitCloseObservedByGateway();
    }

    private void writeRequest(Socket socket) throws Exception {
        socket.getOutputStream().write("GET /test HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        socket.getOutputStream().flush();
    }

    /** Reads up to and including the CRLFCRLF that ends the response head. */
    private void readResponseHead(Socket socket) throws Exception {
        final InputStream in = socket.getInputStream();
        int[] last = new int[4];
        while (!(last[0] == '\r' && last[1] == '\n' && last[2] == '\r' && last[3] == '\n')) {
            final int read = in.read();
            assertThat(read).as("response head must be received before the abort").isNotEqualTo(-1);
            last = new int[] { last[1], last[2], last[3], read };
        }
    }

    /** Leaves the gateway a beat to observe the close before the test moves on. */
    private void awaitCloseObservedByGateway() throws Exception {
        Thread.sleep(200);
    }

    private void assertProbeAcquiresSlotWithin(HttpClient httpClient, long budgetMillis) {
        long probeStart = System.currentTimeMillis();
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMap(HttpClientResponse::rxBody)
            .test()
            .awaitDone(15, TimeUnit.SECONDS)
            .assertComplete();
        long probeLatency = System.currentTimeMillis() - probeStart;
        assertThat(probeLatency)
            .as("probe latency — the pool slot of the aborted request must be released, not held until the read timeout")
            .isLessThan(budgetMillis);
    }
}
