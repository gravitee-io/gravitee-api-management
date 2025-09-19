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
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;

/**
 * Comparator used to sort {@link HttpAcceptor} in a centralized acceptor collection.
 *
 * Http acceptors are first sorted by host (lower-cased), then in case of equality, by path and, in case of
 * equality in path, the http acceptor priority is used (higher priority first).
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DefaultHttpAcceptor implements HttpAcceptor {

    private static final int HOST_MASK = 1000;

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    private final int weight;

    @EqualsAndHashCode.Include
    private final String host;

    @EqualsAndHashCode.Include
    private final Set<String> serverIds;

    private final String pathWithoutTrailingSlash;

    @EqualsAndHashCode.Include
    private final String path;

    @EqualsAndHashCode.Include
    private ReactorHandler reactor;

    public DefaultHttpAcceptor(String host, String path, ReactorHandler reactor) {
        this(host, path, reactor, null);
    }

    public DefaultHttpAcceptor(String host, String path, ReactorHandler reactor, Collection<String> serverIds) {
        this(host, path, serverIds);
        this.reactor = reactor;
    }

    public DefaultHttpAcceptor(String host, String path) {
        this(host, path, (Set<String>) null);
    }

    public DefaultHttpAcceptor(String host, String path, Collection<String> serverIds) {
        this.host = host;

        // Sanitize
        if (path == null || path.isEmpty()) {
            path = URI_PATH_SEPARATOR;
        }

        if (path.length() > 1 && path.lastIndexOf(URI_PATH_SEPARATOR_CHAR) == path.length() - 1) {
            pathWithoutTrailingSlash = path.substring(0, path.length() - 1);
        } else {
            pathWithoutTrailingSlash = path;
        }

        if (path.lastIndexOf(URI_PATH_SEPARATOR_CHAR) != path.length() - 1) {
            path += URI_PATH_SEPARATOR;
        }

        this.path = DUPLICATE_SLASH_REMOVER.matcher(path).replaceAll(URI_PATH_SEPARATOR);

        if (host != null && !host.isEmpty()) {
            weight =
                HOST_MASK +
                (int) this.path.chars()
                    .filter(ch -> ch == URI_PATH_SEPARATOR_CHAR)
                    .count();
        } else {
            weight = (int) this.path.chars()
                .filter(ch -> ch == URI_PATH_SEPARATOR_CHAR)
                .count();
        }

        this.serverIds = serverIds != null ? new HashSet<>(serverIds) : null;
    }

    public DefaultHttpAcceptor(String path) {
        this(null, path);
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int priority() {
        return weight;
    }

    @Override
    public boolean accept(Request request) {
        return accept(request.host(), request.path(), null);
    }

    @Override
    public boolean accept(String host, String path, String serverId) {
        return matchServer(serverId) && matchHost(host) && matchPath(path);
    }

    private boolean matchServer(String serverId) {
        return serverIds == null || serverIds.isEmpty() || (serverId != null && serverIds.contains(serverId));
    }

    private boolean matchHost(String host) {
        return this.host == null || this.host.equalsIgnoreCase(host);
    }

    private boolean matchPath(String path) {
        return path.startsWith(this.path) || path.equals(pathWithoutTrailingSlash);
    }

    @Override
    public String toString() {
        return "host[" + this.host + "] - path[" + this.path + "/*]";
    }

    public void reactor(ReactorHandler reactor) {
        this.reactor = reactor;
    }

    @Override
    public ReactorHandler reactor() {
        return reactor;
    }

    @Override
    public int compareTo(HttpAcceptor o2) {
        if (this.equals(o2)) {
            return 0;
        }

        final int hostCompare = Objects.compare(toLower(this.host()), toLower(o2.host()), (host1, host2) -> {
            if (host1 == null) {
                return 1;
            } else if (host2 == null) {
                return -1;
            }
            return host1.compareTo(host2);
        });

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

    private String toLower(String value) {
        if (value != null) {
            return value.toLowerCase();
        }
        return value;
    }
}
