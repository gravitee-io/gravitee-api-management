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
 * Index reproducing the order of {@code OverlappingHttpAcceptor}, the acceptor used when overlapping API
 * contexts are allowed.
 *
 * <p>That order is reversed host descending, then path descending, which reads as: the longest matching
 * host first, then the longest matching context path. Hence {@link ReverseHostTrie} over the hosts and
 * {@link PathSegmentTrie#resolveLongest} within a host bucket.
 *
 * @author GraviteeSource Team
 */
public class OverlappingHttpAcceptorIndex extends AbstractHttpAcceptorIndex {

    private static final int MAX_PORT_DIGITS = 5;
    private static final char PORT_SEPARATOR = ':';

    private final ReverseHostTrie byHost = new ReverseHostTrie();

    @Override
    protected void indexHosted(final HttpAcceptor acceptor) {
        // host() already has the wildcard marker removed by the acceptor's constructor.
        byHost.computeIfAbsent(acceptor.host()).add(acceptor.path(), acceptor);
    }

    @Override
    public HttpAcceptor resolve(final String host, final String path, final String serverId) {
        if (unmatchable(path)) {
            return null;
        }
        if (host != null) {
            // Stripping the port once per request is the whole point: the acceptors do it themselves,
            // through a regex recompiled on every call, once per acceptor tested.
            String lookup = stripPort(host).toLowerCase();
            HttpAcceptor accepted = byHost.resolve(lookup, host, path, serverId);
            if (accepted != null) {
                return accepted;
            }
        }
        return hostless.resolveLongest(host, path, serverId);
    }

    /**
     * Equivalent of the {@code :(\d{1,5})$} the acceptors apply, without the regex.
     */
    static String stripPort(final String host) {
        int separator = host.lastIndexOf(PORT_SEPARATOR);
        if (separator < 0) {
            return host;
        }
        int digits = host.length() - separator - 1;
        if (digits < 1 || digits > MAX_PORT_DIGITS) {
            return host;
        }
        for (int i = separator + 1; i < host.length(); i++) {
            char character = host.charAt(i);
            if (character < '0' || character > '9') {
                return host;
            }
        }
        return host.substring(0, separator);
    }
}
