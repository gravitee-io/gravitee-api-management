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

import io.reactivex.rxjava3.core.Completable;
import io.vertx.rxjava3.core.net.NetSocket;

/**
 * Contract to dispatch {@link io.vertx.rxjava3.core.net.NetServer} connections.
 *
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TcpSocketDispatcher {
    /**
     * Dispatch an incoming TCP connection to resolved {@link ApiReactor}
     * @param proxySocket the socket to which the client is connected to.
     * @param serverId the server id that processes that connection
     * @return a Completable to subscribe to in order to process incoming and outgoing buffers as well as connecting to a backend.
     */
    Completable dispatch(NetSocket proxySocket, String serverId);
}
