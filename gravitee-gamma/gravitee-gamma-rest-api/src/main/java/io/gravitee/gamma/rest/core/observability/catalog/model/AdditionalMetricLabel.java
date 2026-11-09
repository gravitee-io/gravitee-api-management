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
 * label, on every surface that labels the key: the filter catalog, the analytics definition and the metric
 * definitions agree. The observability library's {@code deriveAdditionalMetricLabel} applies the same rules but the
 * namespace one, as a fallback when no label is served.
 *
 * @author GraviteeSource Team
 */
public final class AdditionalMetricLabel {

    private static final Pattern SEPARATOR_RUNS = Pattern.compile("[_-]+");
    private static final Pattern WHITESPACE_RUNS = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private AdditionalMetricLabel() {}

    /**
     * The key without its type prefix, each run of {@code _} / {@code -} read as one space, trimmed, first character
     * upper case and the rest as written; the key as given when nothing is left. A namespace naming the one API kind
     * the key is listed under is left out as well, the group naming it ({@link AdditionalMetricNamespace#namesOneKind}):
     * {@code long_llm-proxy_tokens-cached} reads "Tokens cached". Any other namespace is kept: {@code long_failover_count}
     * reads "Failover count", {@code keyword_fraud-check_tier} "Fraud check tier". The field's value type is the one
     * the grammar read, so no prefix is parsed here.
     */
    public static String derive(AdditionalMetricField field) {
        String name = nameOf(field);
        String words = AdditionalMetricNamespace.segmentOf(field)
            .filter(AdditionalMetricNamespace::namesOneKind)
            .map(namespace -> words(name.substring(namespace.length() + 1)))
            .filter(shortened -> !shortened.isEmpty())
            .orElseGet(() -> words(name));
        return capitalized(words, field.rawKey());
    }

    /**
     * {@link #derive} with every namespace kept: the label of a built-in entry nothing describes, which is scoped by
     * its own catalog, not by its key, so no group names the namespace for it.
     */
    public static String deriveFromWholeKey(AdditionalMetricField field) {
        return capitalized(words(nameOf(field)), field.rawKey());
    }

    private static String nameOf(AdditionalMetricField field) {
        String rawKey = field.rawKey();
        return rawKey.startsWith(field.valueType().prefix()) ? rawKey.substring(field.valueType().prefix().length()) : rawKey;
    }

    private static String capitalized(String words, String rawKey) {
        if (words.isEmpty()) {
            return rawKey;
        }
        return words.substring(0, 1).toUpperCase(Locale.ROOT) + words.substring(1);
    }

    private static String words(String name) {
        return WHITESPACE_RUNS.matcher(SEPARATOR_RUNS.matcher(name).replaceAll(" ").strip()).replaceAll(" ");
    }
}
