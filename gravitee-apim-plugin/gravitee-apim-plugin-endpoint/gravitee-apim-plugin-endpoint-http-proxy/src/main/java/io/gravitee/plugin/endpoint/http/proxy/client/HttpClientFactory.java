/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.http.vertx.client.VertxHttpClient;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
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

    public HttpClient getOrBuildHttpClient(final ExecutionContext ctx, final HttpProxyEndpointConnectorConfiguration configuration) {
        if (httpClient == null) {
            synchronized (this) {
                // Double-checked locking.
                if (httpClientCreated.compareAndSet(false, true)) {
                    httpClient = buildHttpClient(ctx, configuration).build().createHttpClient();
                }
            }
        }
        return httpClient;
    }

    protected VertxHttpClient.VertxHttpClientBuilder buildHttpClient(
        final ExecutionContext ctx,
        final HttpProxyEndpointConnectorConfiguration configuration
    ) {
        return VertxHttpClient
            .builder()
            .vertx(ctx.getComponent(Vertx.class))
            .nodeConfiguration(ctx.getComponent(Configuration.class))
            .defaultTarget(configuration.getTarget())
            .httpOptions(configuration.getHttpOptions())
            .sslOptions(configuration.getSslOptions())
            .proxyOptions(configuration.getProxyOptions());
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
