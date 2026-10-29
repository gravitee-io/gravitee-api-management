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
package io.gravitee.gamma.rest.core.observability.catalog.model;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The label derived from an additional metric key alone, served when the key has neither a declared nor a built-in
 * label. Identical to the observability library's {@code deriveAdditionalMetricLabel}, so a label reads the same
 * whether the server or the client derived it.
 *
 * @author GraviteeSource Team
 */
public final class AdditionalMetricLabel {

    private static final Pattern SEPARATOR_RUNS = Pattern.compile("[_-]+");
    private static final Pattern WHITESPACE_RUNS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private AdditionalMetricLabel() {}

    /**
     * {@code additional-metrics.} and the type prefix stripped, each run of {@code _} / {@code -} read as one space,
     * trimmed, first character upper case and the rest as written; the key as given when nothing is left. The family
     * segment is kept: {@code long_failover_count} reads "Failover count", not "Count".
     */
    public static String derive(String key) {
        String rawKey = key.startsWith(AdditionalMetricField.NAME_PREFIX) ? key.substring(AdditionalMetricField.NAME_PREFIX.length()) : key;
        String name = typedName(rawKey);
        String words = WHITESPACE_RUNS.matcher(SEPARATOR_RUNS.matcher(name).replaceAll(" ").strip()).replaceAll(" ");
        if (words.isEmpty()) {
            return key;
        }
        return words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    /** The key after its type prefix, or the whole key when the text before its first {@code _} is not a type. */
    private static String typedName(String rawKey) {
        return AdditionalMetricValueType.ofRawKey(rawKey)
            .map(type -> rawKey.substring(type.prefix().length()))
            .orElse(rawKey);
    }
}
