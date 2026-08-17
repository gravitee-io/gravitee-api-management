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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The resolution as it works today, kept as the reference the index is measured and checked against.
 *
 * <p>This is a transcription, not a reimplementation: {@code DefaultReactorHandlerRegistry} keeps one
 * immutable list sorted by the acceptors' own comparator, and {@code DefaultHttpAcceptorResolver} walks
 * it and returns the first acceptor that accepts. Both are reproduced verbatim so that any divergence
 * found is a divergence of the index, never of the reference.
 *
 * @author GraviteeSource Team
 */
final class LinearHttpAcceptorResolver {

    private final List<HttpAcceptor> sorted;

    LinearHttpAcceptorResolver(Collection<HttpAcceptor> acceptors) {
        List<HttpAcceptor> copy = new ArrayList<>(acceptors);
        copy.sort(null);
        this.sorted = Collections.unmodifiableList(copy);
    }

    HttpAcceptor resolve(String host, String path, String serverId) {
        for (HttpAcceptor httpAcceptor : sorted) {
            if (httpAcceptor.accept(host, path, serverId)) {
                return httpAcceptor;
            }
        }
        return null;
    }

    List<HttpAcceptor> sorted() {
        return sorted;
    }
}
