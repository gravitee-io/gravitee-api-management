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
package io.gravitee.apim.core.api.model;

import io.gravitee.apim.core.api.exception.InvalidPathsException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.With;

@Data
@With
public class Path {

    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("[//]+");
    private static final Pattern VALID_PATH = Pattern.compile("^[/.a-zA-Z0-9-_]*$"); //[-a-zA-Z0-9@:%_\+.~#?&//=]
    private static final String URI_PATH_SEPARATOR = "/";
    private static final char URI_PATH_SEPARATOR_CHAR = '/';

    String host;
    String path;
    boolean overrideAccess;

    Path(String host, String path, boolean overrideAccess) {
        this.host = host;
        this.path = path;
        this.overrideAccess = overrideAccess;
    }

    public static PathBuilder builder() {
        return new PathBuilder();
    }

    public boolean hasHost() {
        return host != null && !host.isEmpty();
    }

    public Path sanitize() {
        return this.withPath(sanitizePath(this.getPath()));
    }

    public static String sanitizePath(String path) {
        String sanitizedPath = path;
        if (sanitizedPath == null || sanitizedPath.isEmpty()) {
            sanitizedPath = URI_PATH_SEPARATOR;
        }

        if (!sanitizedPath.startsWith(URI_PATH_SEPARATOR)) {
            sanitizedPath = URI_PATH_SEPARATOR + sanitizedPath;
        }

        if (sanitizedPath.lastIndexOf(URI_PATH_SEPARATOR_CHAR) != sanitizedPath.length() - 1) {
            sanitizedPath += URI_PATH_SEPARATOR;
        }

        sanitizedPath = DUPLICATE_SLASH_REMOVER.matcher(sanitizedPath).replaceAll(URI_PATH_SEPARATOR);

        if (sanitizedPath != null) {
            try {
                new URL("https", "localhost", 80, sanitizedPath).toURI();
            } catch (MalformedURLException | URISyntaxException e) {
                throw new InvalidPathsException(e.getMessage());
            }
        }
        return sanitizedPath;
    }

    public PathBuilder toBuilder() {
        return new PathBuilder().host(this.host).path(this.path).overrideAccess(this.overrideAccess);
    }

    public static class PathBuilder {

        private String host;
        private String path;
        private boolean overrideAccess;

        PathBuilder() {}

        public PathBuilder host(String host) {
            this.host = host;
            return this;
        }

        public PathBuilder path(String path) {
            this.path = path;
            return this;
        }

        public PathBuilder overrideAccess(boolean overrideAccess) {
            this.overrideAccess = overrideAccess;
            return this;
        }

        public Path build() {
            return new Path(this.host, this.path, this.overrideAccess);
        }

        public String toString() {
            return "Path.PathBuilder(host=" + this.host + ", path=" + this.path + ", overrideAccess=" + this.overrideAccess + ")";
        }
    }
}
