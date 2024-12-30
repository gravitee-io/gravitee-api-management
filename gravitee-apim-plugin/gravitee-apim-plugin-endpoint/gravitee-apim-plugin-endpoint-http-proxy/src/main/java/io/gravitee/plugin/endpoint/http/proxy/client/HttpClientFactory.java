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

import io.gravitee.apim.common.mapper.HttpClientOptionsMapper;
import io.gravitee.apim.common.mapper.HttpProxyOptionsMapper;
import io.gravitee.apim.common.mapper.SslOptionsMapper;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientFactory {

    private HttpClient httpClient;
    private final AtomicBoolean httpClientCreated = new AtomicBoolean(false);

    public HttpClient getOrBuildHttpClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        if (httpClient == null) {
            synchronized (this) {
                // Double-checked locking.
                if (httpClientCreated.compareAndSet(false, true)) {
                    httpClient = buildHttpClient(ctx, configuration, sharedConfiguration).build().createHttpClient();
                }
            }
        }
        return httpClient;
    }

    protected VertxHttpClientFactory.VertxHttpClientFactoryBuilder buildHttpClient(
        final HttpExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return VertxHttpClientFactory
            .builder()
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .defaultTarget(configuration.getTarget())
            .httpOptions(HttpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getHttpOptions()))
            .sslOptions(SslOptionsMapper.INSTANCE.map(sharedConfiguration.getSslOptions()))
            .proxyOptions(HttpProxyOptionsMapper.INSTANCE.map(sharedConfiguration.getProxyOptions()));
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    // Use NOOP Subscriber on websocket close so return completable could be ignored
    public void close() {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }
}
