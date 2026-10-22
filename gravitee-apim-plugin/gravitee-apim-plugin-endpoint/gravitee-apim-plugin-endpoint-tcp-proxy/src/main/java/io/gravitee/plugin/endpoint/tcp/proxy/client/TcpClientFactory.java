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
package io.gravitee.plugin.endpoint.tcp.proxy.client;

import io.gravitee.apim.common.lifecycle.LazyCloseable;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.tcp.VertxTcpClientFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.mappers.SslOptionsMapper;
import io.gravitee.plugin.mappers.TcpClientOptionsMapper;
import io.gravitee.plugin.mappers.TcpProxyOptionsMapper;
import io.gravitee.plugin.mappers.TcpTargetMapper;
import io.vertx.core.internal.CloseFuture;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.VertxInternal;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.impl.CleanableNetClient;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetClient;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class TcpClientFactory {

    // Built on an event loop by the first connection, closed by the thread that stops the endpoint.
    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    private final LazyCloseable<NetClient> tcpClient = new LazyCloseable<>(client -> client.close());

    public NetClient getOrBuildTcpClient(
        final TcpExecutionContext ctx,
        final TcpProxyEndpointConnectorConfiguration configuration,
        final TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return tcpClient.getOrBuild(() -> {
            NetClient client = buildTcpClient(ctx, configuration, sharedConfiguration).build().createTcpClient();
            releaseFromOwnerCloseFuture(ctx, client);
            return client;
        });
    }

    /**
     * Take over the lifecycle of a freshly created {@link NetClient} by unregistering it from the Vert.x close future it
     * was just added to.
     *
     * <p>[{@code vertx-5.0-close-future-leak}] Vert.x 5.0 registers every client returned by {@code vertx.createNetClient()} on the close future of the
     * creating context and never removes it: {@code close()} only tears the connections down, so the client of every
     * redeployed endpoint piles up until the node stops. {@link NetClientOptions} has no shared mode that would avoid it,
     * unlike the HTTP and WebSocket clients. Unregistering makes this factory the owner: {@link #close()} must be called
     * when the endpoint stops, which the connector does. Vert.x still shuts the client down through its {@code Cleaner}
     * once it becomes unreachable.
     *
     * <p>This runs on the thread that created the client, so {@code getContext()} resolves the same close future that
     * {@code createNetClient} used. It is pinned to the Vert.x 5.0.x layout ({@code CleanableNetClient} moves in 5.1) and
     * the leak is fixed upstream in 5.2: delete it when upgrading, {@code TcpClientFactoryCloseFutureTest} fails once the
     * workaround is no longer needed.
     */
    private static void releaseFromOwnerCloseFuture(TcpExecutionContext ctx, NetClient client) {
        if (!(client.getDelegate() instanceof CleanableNetClient cleanable)) {
            ctx.withLogger(log).debug("Vert.x no longer returns a CleanableNetClient, skipping close future unregistration");
            return;
        }

        VertxInternal vertxInternal = (VertxInternal) ctx.getComponent(Vertx.class).getDelegate();
        ContextInternal context = vertxInternal.getContext();
        CloseFuture owner = context != null ? context.closeFuture() : vertxInternal.closeFuture();
        if (!owner.remove(cleanable.unwrap()) && !owner.isClosed()) {
            ctx
                .withLogger(log)
                .warn("NetClient was not registered on the resolved close future; the Vert.x 5 leak workaround is ineffective");
        }
    }

    protected VertxTcpClientFactory.VertxTcpClientFactoryBuilder buildTcpClient(
        final TcpExecutionContext ctx,
        final TcpProxyEndpointConnectorConfiguration configuration,
        final TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return VertxTcpClientFactory.builder()
            .vertx(ctx.getComponent(Vertx.class))
            .tcpTarget(TcpTargetMapper.INSTANCE.map(configuration.getTcpTarget()))
            .sslOptions(SslOptionsMapper.INSTANCE.map(sharedConfiguration.getSslOptions()))
            .proxyOptions(TcpProxyOptionsMapper.INSTANCE.map(sharedConfiguration.getProxyOptions()))
            .tcpOptions(TcpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getTcpClientOptions()))
            .nodeConfiguration(ctx.getComponent(Configuration.class));
    }

    public void close() {
        tcpClient.close();
    }
}
