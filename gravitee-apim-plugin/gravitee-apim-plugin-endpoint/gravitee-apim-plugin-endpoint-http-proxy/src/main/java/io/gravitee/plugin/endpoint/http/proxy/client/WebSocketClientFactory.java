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

import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.mappers.HttpClientOptionsMapper;
import io.gravitee.plugin.mappers.HttpProxyOptionsMapper;
import io.gravitee.plugin.mappers.SslOptionsMapper;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.WebSocketClient;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
public class WebSocketClientFactory {

    private WebSocketClient webSocketClient;
    private final AtomicBoolean webSocketClientCreated = new AtomicBoolean(false);

    public WebSocketClient getOrBuildWebSocketClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        if (webSocketClient == null) {
            synchronized (this) {
                if (webSocketClientCreated.compareAndSet(false, true)) {
                    webSocketClient = buildWebSocketClient(ctx, configuration, sharedConfiguration).build().createWebSocketClient();
                }
            }
        }
        return webSocketClient;
    }

    protected VertxHttpClientFactory.VertxHttpClientFactoryBuilder buildWebSocketClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return VertxHttpClientFactory.builder()
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .defaultTarget(configuration.getTarget())
            .httpOptions(HttpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getHttpOptions()))
            .sslOptions(SslOptionsMapper.INSTANCE.map(sharedConfiguration.getSslOptions()))
            .proxyOptions(HttpProxyOptionsMapper.INSTANCE.map(sharedConfiguration.getProxyOptions()));
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
            webSocketClient = null;
        }
    }
}
