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
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage that a client abort releases the upstream connection-pool slot held (or awaited) by the aborted
 * request instead of burning it until the endpoint read timeout fires.
 *
 * The API under test uses a single-connection pool so one leaked slot is directly observable as latency on the next
 * request. Aborts are raw TCP socket closes, matching what the gateway sees when a real caller gives up.
 *
 * Without abort propagation, an aborted-while-queued request is still granted the connection once it frees up and
 * holds it idle until the read timeout (8s here) — the mechanism behind pool-collapse incidents where abandoned
 * requests shed no load and the collapse outlives the backend recovery.
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientAbortPropagationV4IntegrationTest extends AbstractGatewayTest {

    private static final String BACKEND_PATH = "/endpoint";
    private static final long PROBE_LATENCY_BUDGET_MS = 2500;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    @Test
    @DeployApi("/apis/v4/http/clientabort/api-client-abort-single-connection.json")
    void should_release_pool_slot_when_queued_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
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

    @Test
    @DeployApi("/apis/v4/http/clientabort/api-client-abort-single-connection.json")
    void should_release_pool_slot_when_in_flight_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
        stubSlowThenFastBackend(6000);

        // Dispatched to the backend (single connection acquired), then aborted while the backend is still answering.
        abortClientRequest(gateway.httpPort(), 500);

        // The abort must release the connection: the probe must not wait for the backend's 6s answer to the aborted
        // request (nor for the 8s read timeout).
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);
    }

    private void stubSlowThenFastBackend(int slowDelayMillis) {
        wiremock.stubFor(
            get(BACKEND_PATH)
                .inScenario("single-slot")
                .whenScenarioStateIs(STARTED)
                .willReturn(ok("slow").withFixedDelay(slowDelayMillis))
                .willSetStateTo("fast")
        );
        wiremock.stubFor(get(BACKEND_PATH).inScenario("single-slot").whenScenarioStateIs("fast").willReturn(ok("fast")));
    }

    /**
     * Sends a request as a raw TCP client and closes the socket after the given delay — a genuine client abort, seen
     * by the gateway as the downstream connection closing mid-request.
     */
    private void abortClientRequest(int gatewayPort, long holdMillis) throws Exception {
        try (Socket socket = new Socket("localhost", gatewayPort)) {
            socket.getOutputStream().write("GET /test HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();
            Thread.sleep(holdMillis);
        }
        // Leaves the gateway a beat to observe the close before the test moves on.
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
