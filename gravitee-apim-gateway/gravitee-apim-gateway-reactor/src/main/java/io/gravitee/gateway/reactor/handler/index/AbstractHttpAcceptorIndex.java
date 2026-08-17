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
import io.gravitee.gateway.reactor.handler.http.AccessPointHttpAcceptor;

/**
 * Holds what both index flavours share: the explosion of access point acceptors, the bucket of
 * acceptors that declare no host, and the guards on degenerate request paths.
 *
 * @author GraviteeSource Team
 */
abstract class AbstractHttpAcceptorIndex implements HttpAcceptorIndex {

    /**
     * Acceptors declaring no host match any host, and sort after every other acceptor in both modes, so
     * they live in their own index consulted last.
     */
    protected final PathSegmentTrie hostless = new PathSegmentTrie();

    private int size;

    @Override
    public void add(final HttpAcceptor acceptor) {
        if (acceptor instanceof AccessPointHttpAcceptor accessPointAcceptor) {
            // The composite delegates its host and its ordering to its first inner acceptor while
            // accepting requests for all of them. Indexing the inner acceptors puts each under the host
            // it actually answers for.
            accessPointAcceptor.innerHttpsAcceptors().forEach(this::add);
            return;
        }
        if (acceptor.host() == null) {
            hostless.add(acceptor.path(), acceptor);
        } else {
            indexHosted(acceptor);
        }
        size++;
    }

    protected abstract void indexHosted(HttpAcceptor acceptor);

    @Override
    public int size() {
        return size;
    }

    /**
     * A request path that is null or empty can never match: every normalised acceptor path is at least a
     * single slash. The scan this index replaces throws on a null path rather than answering, which is
     * reachable through {@code OPTIONS *}; answering "no acceptor" is the one behaviour this index
     * deliberately changes.
     */
    protected static boolean unmatchable(final String path) {
        return path == null || path.isEmpty();
    }
}
