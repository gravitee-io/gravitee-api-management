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
package io.gravitee.apim.integration.tests.http.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.graviteesource.entrypoint.sse.SseEntrypointConnectorFactory;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.mock.MockEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.vertx.core.http.HttpMethod;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class SSEProxyV4EmulationIntegrationTest extends SSEProxyV3IntegrationTest {

    @Test
    @Override
    @DeployApi({ "/apis/http/sse/api-sse-proxy-v2.json", "/apis/v4/http/sse/api-mock-sse-backend.json" })
    void should_proxy_sse_and_close_backend_connection_when_client_closes_the_connection(HttpClient httpClient) {
        super.should_proxy_sse_and_close_backend_connection_when_client_closes_the_connection(httpClient);
    }
}
