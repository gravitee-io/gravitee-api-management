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
package io.gravitee.plugin.entrypoint.tcp.proxy;

import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.connector.Connector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.TcpEntrypointConnector;
import io.gravitee.gateway.reactive.api.context.tcp.TcpExecutionContext;
import io.gravitee.plugin.entrypoint.tcp.proxy.configuration.TcpProxyEntrypointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@AllArgsConstructor
@Slf4j
public class TcpProxyEntrypointConnector extends AbstractService<Connector> implements TcpEntrypointConnector {

    static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.SOCKET);
    static final ListenerType SUPPORTED_LISTENER_TYPE = ListenerType.TCP;
    private static final String ENTRYPOINT_ID = "tcp-proxy";

    private final TcpProxyEntrypointConnectorConfiguration configuration;

    @Override
    public String id() {
        return ENTRYPOINT_ID;
    }

    @Override
    public ApiType supportedApi() {
        return ApiType.PROXY;
    }

    @Override
    public ListenerType supportedListenerType() {
        return SUPPORTED_LISTENER_TYPE;
    }

    @Override
    public Set<ConnectorMode> supportedModes() {
        return SUPPORTED_MODES;
    }

    @Override
    public int matchCriteriaCount() {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean matches(final TcpExecutionContext ctx) {
        // don't have much to match on top of listener type
        return true;
    }

    @Override
    public Completable handleRequest(final TcpExecutionContext ctx) {
        return Completable.complete();
    }

    @Override
    public Completable handleResponse(TcpExecutionContext ctx) {
        return Completable.fromRunnable(() -> ctx.response().pipeDownstream());
    }

    public TcpProxyEntrypointConnectorConfiguration configuration() {
        return configuration;
    }
}
