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

import io.gravitee.apim.common.mapper.SslOptionsMapper;
import io.gravitee.apim.common.mapper.TcpClientOptionsMapper;
import io.gravitee.apim.common.mapper.TcpProxyOptionsMapper;
import io.gravitee.apim.common.mapper.TcpTargetMapper;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.tcp.VertxTcpClientFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfiguration;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.net.NetClient;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TcpClientFactory {

    private NetClient tcpClient;
    private final AtomicBoolean tcpClientCreated = new AtomicBoolean(false);

    public NetClient getOrBuildTcpClient(
        final TcpExecutionContext ctx,
        final TcpProxyEndpointConnectorConfiguration configuration,
        final TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        if (tcpClient == null) {
            synchronized (this) {
                // Double-checked locking.
                if (tcpClientCreated.compareAndSet(false, true)) {
                    tcpClient = buildTcpClient(ctx, configuration, sharedConfiguration).build().createTcpClient();
                }
            }
        }
        return tcpClient;
    }

    protected VertxTcpClientFactory.VertxTcpClientFactoryBuilder buildTcpClient(
        final TcpExecutionContext ctx,
        final TcpProxyEndpointConnectorConfiguration configuration,
        final TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        return VertxTcpClientFactory
            .builder()
            .vertx(ctx.getComponent(Vertx.class))
            .tcpTarget(TcpTargetMapper.INSTANCE.map(configuration.getTcpTarget()))
            .sslOptions(SslOptionsMapper.INSTANCE.map(sharedConfiguration.getSslOptions()))
            .proxyOptions(TcpProxyOptionsMapper.INSTANCE.map(sharedConfiguration.getProxyOptions()))
            .tcpOptions(TcpClientOptionsMapper.INSTANCE.map(sharedConfiguration.getTcpClientOptions()))
            .nodeConfiguration(ctx.getComponent(Configuration.class));
    }

    @SuppressWarnings("ReactiveStreamsUnusedPublisher")
    // Use NOOP Subscriber on websocket close so return completable could be ignored
    public void close() {
        if (tcpClient != null) {
            tcpClient.close();
            tcpClient = null;
        }
    }
}
