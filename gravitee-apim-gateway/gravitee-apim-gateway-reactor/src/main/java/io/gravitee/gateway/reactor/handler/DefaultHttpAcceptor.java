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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Comparator used to sort {@link HttpAcceptor} in a centralized acceptor collection.
 *
 * Http acceptors are first sorted by host (lower-cased), then in case of equality, by path and, in case of
 * equality in path, the http acceptor priority is used (higher priority first).
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class DefaultHttpAcceptor extends AbstractHttpAcceptor implements HttpAcceptor {

    public DefaultHttpAcceptor(String host, String path, Collection<String> serverIds) {
        super(host, path, serverIds);
    }

    public DefaultHttpAcceptor(String host, String path, ReactorHandler reactor, Collection<String> serverIds) {
        super(host, path, reactor, serverIds);
    }

    public DefaultHttpAcceptor(String host, String path, ReactorHandler reactor) {
        this(host, path, reactor, null);
    }

    public DefaultHttpAcceptor(String host, String path) {
        this(host, path, (Set<String>) null);
    }

    public DefaultHttpAcceptor(String path) {
        this(null, path);
    }

    @Override
    public boolean matchHost(String host) {
        return this.host == null || this.host.equalsIgnoreCase(host);
    }

    @Override
    public String toString() {
        return "host[" + this.host + "] - path[" + this.path + "/*]";
    }

    @Override
    public int compareTo(@NonNull HttpAcceptor o2) {
        if (this.equals(o2)) {
            return 0;
        }

        final int hostCompare = Objects.compare(
            toLower(this.host()),
            toLower(o2.host()),
            (host1, host2) -> {
                if (host1 == null) {
                    return 1;
                } else if (host2 == null) {
                    return -1;
                }
                return host1.compareTo(host2);
            }
        );

        if (hostCompare == 0) {
            final int pathCompare = this.path().compareTo(o2.path());

            if (pathCompare == 0) {
                if (this.priority() <= o2.priority()) {
                    return 1;
                }
                return -1;
            }

            return pathCompare;
        }
        return hostCompare;
    }
}
