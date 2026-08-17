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
import java.util.HashMap;
import java.util.Map;

/**
 * Index reproducing the order of {@code DefaultHttpAcceptor}, the acceptor used when overlapping API
 * contexts are not allowed.
 *
 * <p>That order is host ascending with no host last, then path ascending. Host matching is an exact,
 * case-insensitive equality, so at most one host bucket can match a given request and a plain map is
 * enough. Path ascending means the shortest matching context path is the one the scan returns first,
 * hence {@link PathSegmentTrie#resolveShortest}.
 *
 * @author GraviteeSource Team
 */
public class DefaultHttpAcceptorIndex extends AbstractHttpAcceptorIndex {

    private final Map<String, PathSegmentTrie> byHost = new HashMap<>();

    @Override
    protected void indexHosted(final HttpAcceptor acceptor) {
        byHost.computeIfAbsent(acceptor.host().toLowerCase(), host -> new PathSegmentTrie()).add(acceptor.path(), acceptor);
    }

    @Override
    public HttpAcceptor resolve(final String host, final String path, final String serverId) {
        if (unmatchable(path)) {
            return null;
        }
        if (host != null) {
            PathSegmentTrie hosted = byHost.get(host.toLowerCase());
            if (hosted != null) {
                HttpAcceptor accepted = hosted.resolveShortest(host, path, serverId);
                if (accepted != null) {
                    return accepted;
                }
            }
        }
        return hostless.resolveShortest(host, path, serverId);
    }
}
