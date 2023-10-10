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
package io.gravitee.gateway.reactor.handler;

/**
 * This class represents a listening entrypoint to a {@link ReactorHandler}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface TcpAcceptor extends Acceptor<TcpAcceptor> {
    /**
     * Acceptor's host
     * @return the host of this acceptor, there one acceptor per host defined in the API
     */
    String host();

    /**
     * Allows to test the entrypoint against the specified <code>sni</code> to check if it can handle the socket or not.
     *
     * @param sni the incoming sni.
     * @param serverId the id of the server handling the request.
     *
     * @return <code>true</code> if the entrypoint is able to accept the socket, <code>false</code> else.
     */
    boolean accept(String sni, String serverId);
}
