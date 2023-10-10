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
package io.gravitee.gateway.reactive.reactor.handler;

import io.gravitee.gateway.reactor.handler.TcpAcceptor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TcpAcceptorResolver {
    /**
     * Try and resolve a {@link TcpAcceptor} based on provided SNI and server id.
     * @param sni the SNI value (must not be null)
     * @param serverId the server id that runs this resolver (must not be null)
     * @return an acceptor matching passed parameters or <code>null</code>
     */
    TcpAcceptor resolve(String sni, String serverId);
}
