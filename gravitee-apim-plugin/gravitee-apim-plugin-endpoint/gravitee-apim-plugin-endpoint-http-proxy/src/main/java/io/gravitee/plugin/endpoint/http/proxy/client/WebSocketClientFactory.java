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

package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.apim.common.lifecycle.LazyCloseable;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxWebSocketClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.mappers.HttpClientOptionsMapper;
import io.gravitee.plugin.mappers.HttpProxyOptionsMapper;
import io.gravitee.plugin.mappers.SslOptionsMapper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.WebSocketClient;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebSocketClientFactory {

    // Built on an event loop by the first request, closed by the thread that stops the endpoint.
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    private final LazyCloseable<WebSocketClient> webSocketClient = new LazyCloseable<>(client -> client.close());

    public WebSocketClient getOrBuildWebSocketClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return webSocketClient.getOrBuild(() ->
            buildWebSocketClient(ctx, configuration, sharedConfiguration).build().createWebSocketClient()
        );
    }

    protected VertxWebSocketClientFactory.VertxWebSocketClientFactoryBuilder buildWebSocketClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return VertxWebSocketClientFactory.builder()
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .defaultTarget(configuration.getTarget())
            .httpOptions(HttpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getHttpOptions()))
            .sslOptions(SslOptionsMapper.INSTANCE.map(sharedConfiguration.getSslOptions()))
            .proxyOptions(HttpProxyOptionsMapper.INSTANCE.map(sharedConfiguration.getProxyOptions()));
    }

    public void close() {
        webSocketClient.close();
    }
}
