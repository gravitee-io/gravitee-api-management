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
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class AbstractHttpAcceptor implements HttpAcceptor {

    private static final int HOST_MASK = 1000;

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("/+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    @EqualsAndHashCode.Include
    protected final String path;

    @EqualsAndHashCode.Include
    protected final Set<String> serverIds;

    @EqualsAndHashCode.Include
    protected ReactorHandler reactor;

    @EqualsAndHashCode.Include
    protected String host;

    protected final String pathWithoutTrailingSlash;

    protected final int priority;

    protected AbstractHttpAcceptor(String host, String path, Collection<String> serverIds) {
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
            priority =
                HOST_MASK +
                (int) this.path.chars()
                    .filter(ch -> ch == URI_PATH_SEPARATOR_CHAR)
                    .count();
        } else {
            priority = (int) this.path.chars()
                .filter(ch -> ch == URI_PATH_SEPARATOR_CHAR)
                .count();
        }

        this.serverIds = serverIds != null ? new HashSet<>(serverIds) : null;
        this.host = host;
    }

    protected AbstractHttpAcceptor(String host, String path, ReactorHandler reactor, Collection<String> serverIds) {
        this(host, path, serverIds);
        this.reactor = reactor;
    }

    abstract boolean matchHost(String host);

    @Override
    public String path() {
        return path;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public boolean accept(Request request) {
        return accept(request.host(), request.path(), null);
    }

    @Override
    public boolean accept(String host, String path, String serverId) {
        return matchServer(serverId) && matchHost(host) && matchPath(path);
    }

    public void reactor(ReactorHandler reactor) {
        this.reactor = reactor;
    }

    @Override
    public ReactorHandler reactor() {
        return reactor;
    }

    private boolean matchPath(String path) {
        return path.startsWith(this.path) || path.equals(pathWithoutTrailingSlash);
    }

    private boolean matchServer(String serverId) {
        return serverIds == null || serverIds.isEmpty() || (serverId != null && serverIds.contains(serverId));
    }

    @Override
    public String host() {
        return this.host;
    }

    protected String toLower(String value) {
        return value == null ? null : value.toLowerCase();
    }
}
