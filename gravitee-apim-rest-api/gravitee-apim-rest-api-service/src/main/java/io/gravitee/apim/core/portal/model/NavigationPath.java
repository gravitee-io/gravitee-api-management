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
package io.gravitee.apim.core.portal.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record NavigationPath(@Nonnull String path, @Nullable String displayName) {
    public NavigationPath {
        if (!isValid(path)) {
            throw new IllegalArgumentException("Navigation path must start with '/' and contain at least one non-empty segment: " + path);
        }
        path = stripTrailingSlash(path);
    }

    private static boolean isValid(String path) {
        if (path == null || !path.startsWith("/")) return false;
        if (path.contains("//")) return false;
        return stripTrailingSlash(path).length() > 1;
    }

    private static String stripTrailingSlash(String p) {
        return p.length() > 1 && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }
}
