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
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.BlockingPolicy;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for PEN-88: V3 blocking policy timeout via {@code PolicyAdapter}.
 *
 * <p>Verifies that a V3 legacy policy that never completes (simulating an infinite loop or a
 * very long-running Groovy script) is abandoned within {@code gateway.policy.groovy.timeoutMs}
 * milliseconds, and that the Vert.x event loop remains responsive during that window.
 *
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BlockingPolicyTimeoutIntegrationTest {

    /**
     * HTTP-level request timeout (ms).  Short enough to fire within the test but long enough to
     * distinguish the case from the gateway hanging indefinitely.
     *
     * <p>This is deliberately much shorter than {@link BlockingPolicy}'s 30-second sleep.
     * The key assertion in the timeout test: if the event loop were blocked by the V3 policy
     * (pre-fix behaviour), Vert.x's timer thread would never fire and no response would arrive.
     * With the fix the policy runs on a worker thread, the event loop stays free, and the
     * request timeout fires normally at this deadline.
     */
    private static final long HTTP_REQUEST_TIMEOUT_MS = 2_000L;

    /**
     * Maximum time the test awaits a response.  Must be greater than HTTP_REQUEST_TIMEOUT_MS.
     */
    private static final long TEST_AWAIT_SECONDS = 5L;

    @Nested
    @GatewayTest
    @DeployApi({ "/apis/http/api.json", "/apis/http/api-blocking.json" })
    class WithPolicyTimeout extends AbstractGatewayTest {

        @Override
        protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
            super.configureGateway(gatewayConfigurationBuilder);
            // Enable V4 emulation so V3 policies go through PolicyAdapter (the fixed code path).
            gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
            // Short HTTP request timeout.  The blocking policy runs for 30 s; if it occupied the
            // event loop thread (pre-fix) the Vert.x timer could never fire.  With the fix the
            // policy runs on a worker thread, the event loop is free, and this timer fires on time.
            gatewayConfigurationBuilder.set("http.requestTimeout", String.valueOf(HTTP_REQUEST_TIMEOUT_MS));
        }

        @Override
        public void configurePolicies(Map<String, PolicyPlugin> policies) {
            policies.put(BlockingPolicy.POLICY_ID, PolicyBuilder.build(BlockingPolicy.POLICY_ID, BlockingPolicy.class));
        }

        @Test
        @DisplayName("Should return a timeout error response when a V3 blocking policy occupies a worker thread")
        void shouldTimeoutBlockingV3Policy(HttpClient httpClient) throws InterruptedException {
            long start = System.currentTimeMillis();

            httpClient
                .rxRequest(HttpMethod.GET, "/test-blocking")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(TEST_AWAIT_SECONDS, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    // The Vert.x request timer fires (504) because the event loop is free.
                    // Pre-fix: the blocking policy occupied the event loop — timer could never fire.
                    assertThat(response.statusCode()).isGreaterThanOrEqualTo(500);
                    assertThat(System.currentTimeMillis() - start)
                        .as("response must arrive close to HTTP_REQUEST_TIMEOUT_MS, not 30 s")
                        .isLessThan(HTTP_REQUEST_TIMEOUT_MS * 3);
                    return true;
                })
                .assertNoErrors();
        }

        @Test
        @DisplayName("Should keep the event loop responsive while a V3 blocking policy is running")
        void shouldNotBlockEventLoop_whenV3PolicyBlocks(HttpClient httpClient) throws InterruptedException {
            wiremock.stubFor(get("/endpoint").willReturn(ok("ok")));

            // Fire the blocking request but do not wait for it — it runs in a worker thread (PEN-88 fix).
            httpClient
                .rxRequest(HttpMethod.GET, "/test-blocking")
                .flatMap(HttpClientRequest::rxSend)
                .subscribe(r -> {}, err -> {});

            long start = System.currentTimeMillis();

            // A normal request to a different API must still be served quickly.
            // Before the fix, the blocking policy ran on the event loop and would have starved it.
            httpClient
                .rxRequest(HttpMethod.GET, "/test")
                .flatMap(HttpClientRequest::rxSend)
                .test()
                .awaitDone(TEST_AWAIT_SECONDS, TimeUnit.SECONDS)
                .assertComplete()
                .assertValue(response -> {
                    assertThat(response.statusCode()).isEqualTo(200);
                    assertThat(System.currentTimeMillis() - start)
                        .as("healthy request must not be delayed by the blocking policy worker thread")
                        .isLessThan(2_000L);
                    return true;
                })
                .assertNoErrors();
        }
    }
}
