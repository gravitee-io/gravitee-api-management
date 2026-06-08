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
package io.gravitee.apim.core.portal_page.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Value object for a URL-safe navigation segment slug.
 * Produced by lowercasing and replacing non-alphanumeric runs with {@code -}.
 */
public record Slug(String value) {
    public static Slug from(String title) {
        if (title == null) return new Slug("");
        String slug = title.toLowerCase().replaceAll("[^a-z0-9]++", "-").replaceFirst("^-++", "").replaceFirst("-++$", "");
        return new Slug(slug);
    }

    /**
     * Produces a slug unique among {@code used} siblings by appending {@code -2}, {@code -3}, … as needed.
     */
    public static Slug from(String title, Collection<Slug> used) {
        final Set<Slug> occupied = used instanceof Set<Slug> s ? s : Set.copyOf(used);
        final Slug base = from(title);
        if (!occupied.contains(base)) return base;
        return IntStream.iterate(2, n -> n + 1)
            .mapToObj(n -> new Slug(base.value + "-" + n))
            .filter(candidate -> !occupied.contains(candidate))
            .findFirst()
            .orElseThrow();
    }

    @Override
    public String toString() {
        return value;
    }
}
