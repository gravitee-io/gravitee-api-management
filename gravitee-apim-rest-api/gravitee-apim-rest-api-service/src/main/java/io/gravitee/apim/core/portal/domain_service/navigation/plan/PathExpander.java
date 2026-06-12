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
package io.gravitee.apim.core.portal.domain_service.navigation.plan;

import io.gravitee.apim.core.portal_page.model.Slug;
import java.util.ArrayList;
import java.util.List;

/** Assumes path is already validated by {@code NavigationPath}. */
final class PathExpander {

    private PathExpander() {}

    record PathEntry(Slug segment, String parentPath, String fullPath) {}

    static List<PathEntry> expand(String normalizedPath) {
        final var entries = new ArrayList<PathEntry>();
        String parent = null;
        for (String segment : normalizedPath.substring(1).split("/")) {
            final var fullPath = childPath(parent, segment);
            entries.add(new PathEntry(Slug.from(segment), parent, fullPath));
            parent = fullPath;
        }
        return entries;
    }

    static int depth(String path) {
        return (int) path
            .chars()
            .filter(c -> c == '/')
            .count();
    }

    private static String childPath(String parent, String segment) {
        return (parent == null ? "" : parent) + "/" + segment;
    }
}
