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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateEvent;
import io.vertx.core.http.impl.HttpClientConnection;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpClientFactory {

    /** Name Vert.x uses for the {@code IdleStateHandler} it installs on a client connection when {@code idleTimeout} is set. */
    private static final String VERTX_IDLE_HANDLER_NAME = "idle";
    /** Name of the handler we add to convert the upstream idle event into a typed exception. */
    private static final String UPSTREAM_IDLE_HANDLER_NAME = "gravitee-upstream-idle";

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
                    // When the endpoint idleTimeout is configured, Vert.x closes an idle upstream connection
                    // silently (surfacing only a generic close). Convert that idle event into a typed exception
                    // so the connector can report UPSTREAM_IDLE_TIMEOUT distinctly (APIM-12769).
                    if (httpClient != null && sharedConfiguration.getHttpOptions().getIdleTimeout() > 0) {
                        httpClient.connectionHandler(HttpClientFactory::installUpstreamIdleConverter);
                    }
                }
            }
        }
        return httpClient;
    }

    /**
     * Add, once per upstream connection, a handler right after Vert.x's {@code IdleStateHandler} that turns the
     * {@link IdleStateEvent} into an {@link UpstreamIdleTimeoutException} failing the in-flight request — instead
     * of letting Vert.x close the connection silently. Best-effort: if the pipeline shape is unexpected we leave
     * it untouched and fall back to the generic connection-close classification.
     */
    private static void installUpstreamIdleConverter(final HttpConnection connection) {
        try {
            final Channel channel = ((HttpClientConnection) connection.getDelegate()).channel();
            final ChannelPipeline pipeline = channel.pipeline();
            if (pipeline.get(UPSTREAM_IDLE_HANDLER_NAME) == null && pipeline.get(VERTX_IDLE_HANDLER_NAME) != null) {
                pipeline.addAfter(VERTX_IDLE_HANDLER_NAME, UPSTREAM_IDLE_HANDLER_NAME, new UpstreamIdleStateConverter());
            }
        } catch (Exception e) {
            log.debug("Unable to install upstream idle converter; falling back to default close classification", e);
        }
    }

    /**
     * Inbound handler that converts the Netty idle event on an upstream connection into a typed
     * {@link UpstreamIdleTimeoutException} (which fails the in-flight request), rather than forwarding the event
     * to Vert.x which would close the connection silently.
     */
    private static class UpstreamIdleStateConverter extends ChannelInboundHandlerAdapter {

        @Override
        public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.fireExceptionCaught(new UpstreamIdleTimeoutException("Upstream idle timeout"));
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    protected VertxHttpClientFactory.VertxHttpClientFactoryBuilder buildHttpClient(
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
    // Use NOOP Subscriber on websocket close so return completable could be ignored
    public void close() {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }
}
