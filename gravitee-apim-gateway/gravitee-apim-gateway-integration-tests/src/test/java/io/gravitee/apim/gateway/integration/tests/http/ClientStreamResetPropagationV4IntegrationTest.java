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
package io.gravitee.apim.gateway.integration.tests.http;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.PoolOptions;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;

/**
 * An HTTP/2 client cancels one request by resetting its stream (RST_STREAM) and keeps the connection open for its
 * other requests. The gateway must treat that reset like a client close and release the upstream pool slot, exactly
 * as {@code ClientAbortPropagationV4IntegrationTest} checks for HTTP/1.1 clients closing their connection.
 * <p>
 * The backend pool is capped at one connection so a slot held by an abandoned request shows up as probe latency.
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientStreamResetPropagationV4IntegrationTest extends AbstractGatewayTest {

    private static final String SINGLE_CONNECTION_API = "/apis/v4/http/clientabort/api-client-abort-single-connection.json";
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

    @Override
    protected void configureHttpClient(
        HttpClientOptions options,
        PoolOptions poolOptions,
        GatewayDynamicConfig.Config gatewayConfig,
        ParameterContext parameterContext
    ) {
        super.configureHttpClient(options, poolOptions, gatewayConfig, parameterContext);
        // HTTP/2 with prior knowledge: every request below is a stream on the same client connection.
        options.setProtocolVersion(HttpVersion.HTTP_2);
        options.setHttp2ClearTextUpgrade(false);
    }

    @Test
    @DeployApi(SINGLE_CONNECTION_API)
    void should_release_pool_slot_when_in_flight_stream_is_reset(HttpClient httpClient) {
        stubSlowThenFastBackend(6000);

        // Cancelled once dispatched to the backend (single connection acquired), while the backend is still answering.
        var request = sendRequest(httpClient);
        awaitBackendRequests(1);
        request.reset();

        // Without propagation the slot is held for the backend's 6s answer (or the 8s read timeout).
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);
    }

    @Test
    @DeployApi(SINGLE_CONNECTION_API)
    void should_not_dispatch_queued_stream_that_was_reset(HttpClient httpClient) throws Exception {
        stubSlowThenFastBackend(3000);

        var slowRequest = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .flatMap(HttpClientResponse::rxBody)
            .test();
        // The slow request owns the only connection before the next one is sent.
        awaitBackendRequests(1);

        // Cancelled while QUEUED for the only connection: must never be granted the slot nor reach the backend.
        var queuedRequest = sendRequest(httpClient);
        // Lets the stream reach the gateway before the reset. Too short only lets the reset win the race (the test then
        // passes without exercising the queue); it cannot fail the test, the slow request holds the slot for 3s.
        Thread.sleep(300);
        queuedRequest.reset();

        slowRequest.awaitDone(10, TimeUnit.SECONDS).assertComplete();
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);

        // Slow request + probe only.
        wiremock.verify(2, getRequestedFor(urlPathEqualTo(BACKEND_PATH)));
    }

    /** Sends a request over HTTP/2 and returns it once its stream is open; the client connection stays open. */
    private HttpClientRequest sendRequest(HttpClient httpClient) {
        final AtomicReference<HttpClientRequest> request = new AtomicReference<>();
        httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .doOnSuccess(request::set)
            .flatMap(HttpClientRequest::rxSend)
            .onErrorComplete()
            .subscribe();
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> request.get() != null);
        assertThat(request.get().version()).isEqualTo(HttpVersion.HTTP_2);
        return request.get();
    }

    private void awaitBackendRequests(int count) {
        await()
            .atMost(5, TimeUnit.SECONDS)
            .until(() -> wiremock.findAll(getRequestedFor(urlPathEqualTo(BACKEND_PATH))).size() >= count);
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
            .as("probe latency — pool slot of the reset request must be released, not held until the read timeout")
            .isLessThan(budgetMillis);
    }
}
