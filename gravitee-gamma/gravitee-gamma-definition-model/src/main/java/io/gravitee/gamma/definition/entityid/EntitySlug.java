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
package io.gravitee.gamma.definition.entityid;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Canonical slug rule shared by every Gamma module (ADR-037), so the same name always yields the same slug.
 * NFD normalize, drop combining marks, lowercase, replace anything outside {@code [a-z0-9_-]} with {@code -},
 * then trim leading/trailing dashes.
 *
 * <p>The dot is reserved as the {@code <type>.<slug>} separator and is therefore <strong>not</strong> part of the
 * allow-set: {@code claude.ai} becomes {@code claude-ai} and {@code gpt-3.5} becomes {@code gpt-3-5}. This is the
 * single source of truth that the per-module sluggers (catalog, proxy, authz) delegate to.
 */
public final class EntitySlug {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9_-]+");
    private static final Pattern REPEATED_DASHES = Pattern.compile("-{2,}");
    private static final Pattern TRIM_DASHES = Pattern.compile("^-+|-+$");

    private EntitySlug() {}

    public static String toSlug(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = COMBINING_MARKS.matcher(normalized).replaceAll("");
        String slug = normalized.toLowerCase(Locale.ROOT);
        slug = NON_SLUG.matcher(slug).replaceAll("-");
        slug = REPEATED_DASHES.matcher(slug).replaceAll("-");
        slug = TRIM_DASHES.matcher(slug).replaceAll("");
        return slug.isEmpty() ? null : slug;
    }

    /** On create: an explicit slug wins, otherwise derive it from the name. */
    public static String orSlug(String explicit, String fallbackName) {
        return explicit != null ? toSlug(explicit) : toSlug(fallbackName);
    }

    /** On update: an explicit slug wins, otherwise keep the existing one, otherwise derive it from the name. */
    public static String orKeep(String explicit, String existing, String fallbackName) {
        if (explicit != null) {
            return toSlug(explicit);
        }
        if (existing != null) {
            return existing;
        }
        return toSlug(fallbackName);
    }
}
