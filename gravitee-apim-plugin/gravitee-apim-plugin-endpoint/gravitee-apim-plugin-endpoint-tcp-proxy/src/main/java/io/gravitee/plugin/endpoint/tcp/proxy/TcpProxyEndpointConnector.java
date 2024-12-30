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
package io.gravitee.plugin.endpoint.tcp.proxy;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.connector.endpoint.TcpEndpointConnector;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.gateway.reactive.tcp.VertxReadStreamUtil;
import io.gravitee.plugin.endpoint.tcp.proxy.client.TcpClientFactory;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.tcp.proxy.configuration.TcpProxyEndpointConnectorSharedConfiguration;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TcpProxyEndpointConnector extends AbstractService<Connector> implements TcpEndpointConnector {

    private static final String ENDPOINT_ID = "tcp-proxy";
    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SOCKET);
    private final TcpProxyEndpointConnectorConfiguration configuration;
    private final TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private final TcpClientFactory tcpClientFactory;

    TcpProxyEndpointConnector(
        TcpProxyEndpointConnectorConfiguration configuration,
        TcpProxyEndpointConnectorSharedConfiguration sharedConfiguration
    ) {
        this.configuration = configuration;
        this.sharedConfiguration = sharedConfiguration;
        this.tcpClientFactory = new TcpClientFactory();
    }

    @Override
    public String id() {
        return ENDPOINT_ID;
    }

    @Override
    public ApiType supportedApi() {
        return ApiType.PROXY;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public Completable connect(TcpExecutionContext ctx) {
        return tcpClientFactory
            .getOrBuildTcpClient(ctx, configuration, sharedConfiguration)
            .rxConnect(this.configuration.getTcpTarget().getPort(), this.configuration.getTcpTarget().getHost())
            .doOnSuccess(backendSocket -> {
                // pause as soon as possible
                backendSocket.pause();
                // configure response as all will happen in end() method
                ctx.response().chunks(backendSocket.toFlowable().map(Buffer::buffer));
                // Read request chunks and write to backendSocket
                ctx.request().pipeUpstream(VertxReadStreamUtil.toVertxRxReadStream(ctx.request().chunks()).rxPipeTo(backendSocket));
            })
            .ignoreElement();
    }

    TcpProxyEndpointConnectorConfiguration getConfiguration() {
        return configuration;
    }

    TcpProxyEndpointConnectorSharedConfiguration getSharedConfiguration() {
        return sharedConfiguration;
    }
}
