/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactor.handler;

import io.gravitee.gateway.api.Request;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VirtualHost implements HttpAcceptor {

    private static final int HOST_MASK = 1000;

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("[//]+");

    private static final String URI_PATH_SEPARATOR = "/";

    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    private final int weight;

    private final String host;

    private final String pathWithoutTrailingSlash;

    private final String path;

    public VirtualHost(String host, String path) {
        this.host = host;

        // Sanitize
        if (path == null || path.isEmpty()) {
            path = URI_PATH_SEPARATOR;
        }

        pathWithoutTrailingSlash = path;

        if (path.lastIndexOf(URI_PATH_SEPARATOR_CHAR) != path.length() - 1) {
            path += URI_PATH_SEPARATOR;
        }

        this.path = DUPLICATE_SLASH_REMOVER.matcher(path).replaceAll(URI_PATH_SEPARATOR);

        if (host != null && !host.isEmpty()) {
            weight = HOST_MASK + (int) this.path.chars().filter(ch -> ch == URI_PATH_SEPARATOR_CHAR).count();
        } else {
            weight = (int) this.path.chars().filter(ch -> ch == URI_PATH_SEPARATOR_CHAR).count();
        }
    }

    public VirtualHost(String path) {
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
        return accept(request.host(), request.path());
    }

    @Override
    public boolean accept(String host, String path) {
        return matchHost(host) && matchPath(path);
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
}
