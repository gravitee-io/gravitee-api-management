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
package io.gravitee.gateway.reactive.reactor;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.core.context.DefaultExecutionContext;
import io.gravitee.gateway.reactive.reactor.handler.TcpAcceptorResolver;
import io.gravitee.gateway.reactive.tcp.VertxTcpRequest;
import io.gravitee.gateway.reactive.tcp.VertxTcpResponse;
import io.gravitee.gateway.reactor.handler.TcpAcceptor;
import io.reactiverse.contextual.logging.ContextualData;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.net.NetSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultTcpSocketDispatcher implements TcpSocketDispatcher {

    private final TcpAcceptorResolver tcpAcceptorResolver;
    private final ComponentProvider componentProvider;
    private final IdGenerator idGenerator;

    @Override
    public Completable dispatch(NetSocket proxySocket, String serverId) {
        String sni = proxySocket.indicatedServerName();

        try {
            TcpAcceptor acceptor = tcpAcceptorResolver.resolve(sni, serverId);
            if (acceptor == null) {
                log.info("No reactor found for SNI {} and serverId {}", sni, serverId);
                return Completable.error(new IllegalStateException("No TCP acceptor found for SNI: %s".formatted(sni)));
            }

            if (acceptor.reactor() instanceof ApiReactor<?> tcpReactor) {
                ContextualData.put("envId", tcpReactor.api().getEnvironmentId());
                ContextualData.put("orgId", tcpReactor.api().getOrganizationId());

                // pause the socket as soon as possible
                proxySocket.pause();

                // create context and req/res
                VertxTcpRequest tcpRequest = new VertxTcpRequest(proxySocket, idGenerator);
                var ctx = new DefaultExecutionContext(tcpRequest, new VertxTcpResponse(tcpRequest));
                ctx.componentProvider(componentProvider);
                ctx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API, tcpReactor.api());
                ctx.setInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE, ListenerType.TCP);
                // TODO execute specific platform chain factory that will initialize the MetricsReporter
                return tcpReactor.handle(ctx);
                // TODO execute processor
            }

            return Completable.error(new IllegalArgumentException("Could not dispatch TCP socket, internal error"));
        } catch (Exception e) {
            return Completable.error(e);
        }
    }
}
