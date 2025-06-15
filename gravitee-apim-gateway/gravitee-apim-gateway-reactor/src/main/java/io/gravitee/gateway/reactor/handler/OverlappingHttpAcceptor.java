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
import javax.annotation.Nonnull;
import lombok.EqualsAndHashCode;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class OverlappingHttpAcceptor extends AbstractHttpAcceptor implements HttpAcceptor {

    private static final String WILDCARD_MATCHER = "^\\*";
    private static final String HOST_HEADER_PORT_MATCHER = ":(\\d{1,5})$";

    private boolean hasWildCardHost;

    public OverlappingHttpAcceptor(String host, String path, Collection<String> serverIds) {
        super(host, path, serverIds);
        if (this.host != null) {
            this.host = host.replaceFirst(WILDCARD_MATCHER, "");
            this.hasWildCardHost = !this.host.equals(host);
        }
    }

    public OverlappingHttpAcceptor(String host, String path, ReactorHandler reactor, Collection<String> serverIds) {
        this(host, path, serverIds);
        this.reactor = reactor;
    }

    public OverlappingHttpAcceptor(String host, String path, ReactorHandler reactor) {
        this(host, path, reactor, null);
    }

    public OverlappingHttpAcceptor(String host, String path) {
        this(host, path, (Set<String>) null);
    }

    public OverlappingHttpAcceptor(String path) {
        this(null, path);
    }

    private String reverse(String value) {
        if (value != null) {
            return new StringBuilder(value).reverse().toString();
        }
        return null;
    }

    /*
     * From the Kubernetes gateway API specification:
     *
     * Implementations MUST ignore any port value specified in the HTTP Host header while
     * performing a match.
     */
    @Override
    boolean matchHost(String host) {
        if (this.host == null) {
            return true;
        }
        var hostWithoutPort = host.replaceAll(HOST_HEADER_PORT_MATCHER, "");
        return (this.hasWildCardHost ? hostWithoutPort.endsWith(this.host) : this.host.equalsIgnoreCase(hostWithoutPort));
    }

    @Override
    public int compareTo(@Nonnull HttpAcceptor o2) {
        if (this.equals(o2)) {
            return 0;
        }

        // Order from the most specific to the least
        // by reversing domain name and reversing order
        // Example:
        // 1) foo.bar.acme.com
        // 2) .bar.acme.com
        // 3) bar.acme.com
        // 4) .acme.com
        // 5) acme.com
        final int hostCompare = Objects.compare(
            toLower(reverse(this.host())),
            toLower(reverse(o2.host())),
            (thisHost, otherHost) -> {
                if (thisHost == null) {
                    return 1;
                } else if (otherHost == null) {
                    return -1;
                }
                // allow wildcard to be after any non-wild card for the same name
                return thisHost.compareTo(otherHost) * -1;
            }
        );

        if (hostCompare == 0) {
            // allow sub-path to be first /a/b/c is then before /a/b
            final int pathCompare = this.path().compareTo(o2.path()) * -1;

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

    @Override
    public String toString() {
        if (hasWildCardHost) {
            return "host[*" + this.host + "] - path[" + this.path + "/*]";
        }
        return "host[" + this.host + "] - path[" + this.path + "/*]";
    }
}
