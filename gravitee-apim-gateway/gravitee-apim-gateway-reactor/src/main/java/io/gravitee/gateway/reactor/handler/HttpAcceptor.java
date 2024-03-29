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

import io.gravitee.gateway.api.Request;

/**
 * This class represents a listening entrypoint to a {@link ReactorHandler}.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface HttpAcceptor extends Acceptor<HttpAcceptor> {
    /**
     * @return the listening path of this http acceptor.
     */
    String path();

    /**
     * @return the optional listening host (can be <code>null</code>).
     */
    String host();

    /**
     * @return the priority of this http acceptor.
     */
    int priority();

    /**
     * @deprecated see {@link #accept(String, String, String)} instead.
     */
    @Deprecated(forRemoval = true)
    boolean accept(Request request);

    /**
     * Allows to test the entrypoint against the specified <code>host</code> and <code>path</code> to check if it can handle the request or not.
     *
     * @param host the request's host.
     * @param path the request's path.
     * @param serverId the id of the server handling the request.
     *
     * @return <code>true</code> if the entrypoint is able to accept the request, <code>false</code> else.
     */
    boolean accept(String host, String path, String serverId);
}
