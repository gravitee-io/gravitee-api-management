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
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.parameters.GatewayDynamicConfig;
import io.gravitee.definition.model.ExecutionMode;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * End-to-end coverage that a client abort on a v2 API running with the v4 emulation engine releases the upstream
 * connection-pool slot held (or awaited) by the aborted request, instead of keeping the backend call running until
 * the backend answers or the endpoint read timeout fires.
 *
 * The API under test uses a single-connection pool so one leaked slot is directly observable as latency on the next
 * request. Aborts are raw TCP socket closes, matching what the gateway sees when a real caller gives up.
 */
@GatewayTest(v2ExecutionMode = ExecutionMode.V4_EMULATION_ENGINE)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClientAbortPropagationV4EmulationIntegrationTest extends AbstractGatewayTest {

    private static final String API = "/apis/http/clientabort/api-client-abort-single-connection.json";
    private static final String BACKEND_PATH = "/endpoint";
    private static final long PROBE_LATENCY_BUDGET_MS = 2500;

    @Test
    @DeployApi(API)
    void should_release_pool_slot_when_in_flight_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
        stubSlowThenFastBackend(6000);

        // Dispatched to the backend (single connection acquired), then aborted while the backend is still answering.
        abortClientRequest(gateway.httpPort(), 500);

        // The abort must cancel the backend call: the probe must not wait for the backend's 6s answer to the aborted
        // request (nor for the 8s read timeout).
        assertProbeAcquiresSlotWithin(httpClient, PROBE_LATENCY_BUDGET_MS);
    }

    @Test
    @DeployApi(API)
    void should_release_pool_slot_when_queued_request_is_aborted(HttpClient httpClient, GatewayDynamicConfig.HttpConfig gateway)
        throws Exception {
        // First backend call is slow (3s) to keep the single connection busy; every later call answers immediately, so
        // any latency observed on the probe is pool-acquisition wait, not backend time.
        stubSlowThenFastBackend(3000);

        var slowRequest = httpClient
            .rxRequest(HttpMethod.GET, "/test")
            .flatMap(HttpClientRequest::rxSend)
            .doOnSuccess(response -> assertThat(response.statusCode()).isEqualTo(200))
            .flatMap(HttpClientResponse::rxBody)
            .test();
        Thread.sleep(500);

        // Aborted while queued for the connection: it must never hold the slot once granted.
        abortClientRequest(gateway.httpPort(), 300);

        slowRequest.awaitDone(10, TimeUnit.SECONDS).assertComplete();

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
            .as("probe latency — the backend call of the aborted request must be cancelled, not kept until it answers")
            .isLessThan(budgetMillis);
    }
}
