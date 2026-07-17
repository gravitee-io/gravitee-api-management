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
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code maxWaitQueueSize} shared configuration of the v4 HTTP proxy endpoint: when the connection
 * pool is full and the wait queue is disabled, a pending request is rejected immediately instead of being queued.
 *
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/v4/http/api-max-wait-queue.json")
class HttpProxyMaxWaitQueueV4IntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    private static final int CONCURRENT_REQUESTS = 5;

    @Test
    @DisplayName("Should reject overflowing requests with 503 load-shedding when the pool is full and the wait queue is disabled")
    void should_reject_requests_when_wait_queue_is_full(HttpClient httpClient) {
        // The backend keeps its single pooled connection (maxConcurrentConnections = 1) busy for a while, so the
        // concurrent requests pile up: with the wait queue disabled (maxWaitQueueSize = 0) they must be rejected
        // immediately instead of being queued.
        wiremock.stubFor(get("/endpoint").willReturn(ok("response from backend").withFixedDelay(2000)));

        final List<Integer> statuses = Flowable.range(0, CONCURRENT_REQUESTS)
            .flatMap(
                i -> httpClient.rxRequest(HttpMethod.GET, "/test").flatMap(HttpClientRequest::rxSend).toFlowable(),
                false,
                CONCURRENT_REQUESTS
            )
            .map(response -> response.statusCode())
            .toList()
            .blockingGet();

        // The single request that grabs the connection succeeds; every request that overflows the disabled queue
        // is shed with a retryable 503 (distinct from the 502 used for other backend connection errors).
        assertThat(statuses).contains(503);
        assertThat(statuses).doesNotContain(502);
        assertThat(statuses)
            .filteredOn(status -> status == 200)
            .hasSize(1);
    }
}
