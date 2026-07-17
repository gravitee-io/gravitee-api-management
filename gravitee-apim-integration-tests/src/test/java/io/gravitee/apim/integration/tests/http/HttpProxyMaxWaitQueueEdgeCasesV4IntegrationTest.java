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
 * Edge-case smoke coverage for the v4 HTTP proxy {@code maxWaitQueueSize} across its meaningful values, all with a
 * single-connection pool ({@code maxConcurrentConnections = 1}) and a slow backend so requests contend for the pool.
 */
@GatewayTest
@DeployApi(
    {
        "/apis/v4/http/api-mwq-unbounded.json",
        "/apis/v4/http/api-mwq-absent.json",
        "/apis/v4/http/api-mwq-null.json",
        "/apis/v4/http/api-mwq-bounded.json",
    }
)
class HttpProxyMaxWaitQueueEdgeCasesV4IntegrationTest extends AbstractGatewayTest {

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    private List<Integer> fireConcurrent(HttpClient httpClient, String path, int count) {
        return Flowable.range(0, count)
            .flatMap(i -> httpClient.rxRequest(HttpMethod.GET, path).flatMap(HttpClientRequest::rxSend).toFlowable(), false, count)
            .map(response -> response.statusCode())
            .toList()
            .blockingGet();
    }

    @Test
    @DisplayName("Unbounded (-1): all contending requests are queued and served, none shed")
    void unbounded_queue_serves_all(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("ok").withFixedDelay(300)));
        assertThat(fireConcurrent(httpClient, "/mwq-unbounded", 4)).containsOnly(200);
    }

    @Test
    @DisplayName("Absent field: defaults to unbounded, all requests served")
    void absent_defaults_to_unbounded(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("ok").withFixedDelay(300)));
        assertThat(fireConcurrent(httpClient, "/mwq-absent", 4)).containsOnly(200);
    }

    @Test
    @DisplayName("Explicit null: treated as absent (unbounded), all requests served")
    void explicit_null_defaults_to_unbounded(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("ok").withFixedDelay(300)));
        assertThat(fireConcurrent(httpClient, "/mwq-null", 4)).containsOnly(200);
    }

    @Test
    @DisplayName("Bounded (1): the pool + one queued request are served, the rest are shed with 503")
    void bounded_queue_sheds_overflow(HttpClient httpClient) {
        wiremock.stubFor(get("/endpoint").willReturn(ok("ok").withFixedDelay(1500)));
        List<Integer> statuses = fireConcurrent(httpClient, "/mwq-bounded", 6);
        assertThat(statuses).contains(200);
        assertThat(statuses).contains(503);
    }
}
