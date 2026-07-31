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
package io.gravitee.apim.integration.tests.grpc.v4;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EndpointBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnectorFactory;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.http.proxy.HttpProxyEntrypointConnectorFactory;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.Map;

/**
 * Shared setup for the tests that send gRPC-flagged requests to a plain {@code http://} target.
 *
 * <p>Each of those tests lives in its own class on purpose. The gateway is deployed once per class and
 * {@code HttpClientFactory} caches the client it builds for the lifetime of the endpoint, so a plain request made by
 * an earlier test would build a clean client and hide what a later test is trying to observe.
 */
abstract class AbstractHttpTargetGrpcV4GatewayTest extends AbstractGatewayTest {

    protected static final String GRPC_CONTENT_TYPE = "application/grpc";

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("http-proxy", EntrypointBuilder.build("http-proxy", HttpProxyEntrypointConnectorFactory.class));
    }

    @Override
    public void configureEndpoints(Map<String, EndpointConnectorPlugin<?, ?>> endpoints) {
        endpoints.putIfAbsent("http-proxy", EndpointBuilder.build("http-proxy", HttpProxyEndpointConnectorFactory.class));
    }

    protected void callBackend(HttpClient httpClient, String contentType) throws InterruptedException {
        httpClient
            .rxRequest(HttpMethod.POST, "/test")
            .flatMap(request -> {
                if (contentType != null) {
                    request.putHeader("Content-Type", contentType);
                }
                return request.rxSend("hello");
            })
            .flatMapPublisher(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                return response.body().toFlowable();
            })
            .test()
            .await()
            .assertComplete()
            .assertNoErrors();
    }
}
