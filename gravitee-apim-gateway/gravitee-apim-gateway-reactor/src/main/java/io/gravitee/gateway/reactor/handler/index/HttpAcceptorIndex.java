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
package io.gravitee.gateway.reactor.handler.index;

import io.gravitee.gateway.reactor.handler.HttpAcceptor;

/**
 * Resolves the {@link HttpAcceptor} that handles a request without walking every deployed acceptor.
 *
 * <p>Implementations answer the same question as the sorted-list scan they replace, and return the same
 * acceptor: the one that scan would have returned first. Which implementation applies depends on the
 * {@code allowOverlappingApiContexts} setting, because that setting is what decides the sort order the
 * index has to reproduce.
 *
 * @author GraviteeSource Team
 */
public interface HttpAcceptorIndex {
    /**
     * Indexes an acceptor. An acceptor holding several inner acceptors, one per access point, is indexed
     * through its inner acceptors so that each is placed under its own host.
     */
    void add(HttpAcceptor acceptor);

    /**
     * @return the acceptor handling this request, or {@code null} when none does
     */
    HttpAcceptor resolve(String host, String path, String serverId);

    /**
     * @return the number of indexed acceptors, counting inner acceptors individually
     */
    int size();
}
