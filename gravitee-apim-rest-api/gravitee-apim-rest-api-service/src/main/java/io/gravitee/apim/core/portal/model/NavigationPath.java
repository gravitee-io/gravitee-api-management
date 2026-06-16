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

import io.gravitee.apim.core.portal_page.model.Slug;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.stream.Collectors;

public record NavigationPath(@Nonnull String path, @Nullable String displayName, @Nullable Integer order) {
    public NavigationPath {
        path = normalizePath(path);
    }

    public NavigationPath(@Nonnull String path, @Nullable String displayName) {
        this(path, displayName, null);
    }

    public NavigationPath descend(String childSegment) {
        return new NavigationPath(this.path + "/" + childSegment, null);
    }

    private static String normalizePath(String p) {
        String s = stripTrailingSlash(p);
        if (s.isEmpty()) return s;
        return Arrays.stream(s.split("/", -1))
            .map(part -> part.isEmpty() ? part : slugify(part).value())
            .collect(Collectors.joining("/"));
    }

    private static String stripTrailingSlash(String p) {
        return p.length() > 1 && p.endsWith("/") ? p.substring(0, p.length() - 1) : p;
    }

    public static Slug slugify(String value) {
        return Slug.from(value);
    }
}
