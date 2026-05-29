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
package io.gravitee.repository.elasticsearch.utils;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Resolves an OTel ES data stream index template by substituting {@code {placeholder}} tokens with
 * their caller-supplied values, applying the same dataset normalisation the OpenTelemetry
 * collector's {@code elasticsearchexporter} applies before writing.
 *
 * <p>Why this isn't {@code IndexNameUtils.format}: that utility lowercases substituted values but
 * leaves their other characters intact, which is the right behaviour for legacy analytics indices
 * that the gateway writes directly. The OTel collector ES exporter, in contrast, runs each dataset
 * component through {@code sanitizeDataStreamField} — replacing characters in a "disallowed runes"
 * set with {@code _} and then lowercasing — before forming the final data stream name. The
 * disallowed set for dataset components is a superset of the namespace set; notably it includes the
 * hyphen, so a Gravitee org id of {@code "my-org-1"} is stored under
 * {@code traces-gamma_my_org_1.otel-<namespace>} but a naive {@code IndexNameUtils.format}
 * substitution would query for {@code traces-gamma_my-org-1.otel-<namespace>} — every result
 * misses.
 *
 * <p>Source of the rune sets + algorithm:
 * {@code https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/elasticsearchexporter/data_stream_router.go}
 * — see {@code sanitizeDataStreamField}, {@code disallowedDatasetRunes},
 * {@code disallowedNamespaceRunes}, and {@code maxDataStreamBytes}.
 *
 * <p>Design note: we apply the dataset rule (the stricter superset) to every placeholder, not just
 * those in dataset position within the template. Templates for the OTel scopes always target OTel
 * data streams by construction, and a placeholder in namespace position has hyphens preserved by
 * the namespace rule but they'd just be cosmetic — the look-up still works once both sides agree
 * on the substitution. The trade-off (losing potential hyphens in namespace-position placeholders)
 * is preferable to the risk of getting the position detection wrong.
 *
 * @author GraviteeSource Team
 */
public final class OtelDataStreamIndexUtils {

    /**
     * Characters disallowed in OTel data stream dataset components — superset of the namespace
     * disallowed set (it adds the hyphen). The OTel ES exporter replaces these with {@code _}.
     */
    private static final String DISALLOWED_DATASET_RUNES = "-\\/*?\"<>| ,#:";

    /** Max byte length of a sanitised dataset / namespace component, matching the exporter constant. */
    private static final int MAX_DATA_STREAM_BYTES = 100;

    private OtelDataStreamIndexUtils() {}

    /**
     * Substitutes {@code {placeholder}} tokens in {@code template}, sanitising each replacement
     * value per OTel data stream dataset rules (disallowed runes → {@code _}, others lowercased,
     * truncated to {@value #MAX_DATA_STREAM_BYTES} bytes). The template's own structural characters
     * are left untouched — only the substituted values are sanitised.
     */
    public static String format(String template, Map<String, String> parameters) {
        var resolved = template;
        for (var entry : parameters.entrySet()) {
            var sanitised = sanitiseDatasetComponent(entry.getValue());
            resolved = resolved.replaceAll(String.format("\\{%s}", entry.getKey()), Matcher.quoteReplacement(sanitised));
        }
        return resolved;
    }

    /** Visible for test. Mirrors the OTel exporter's {@code sanitizeDataStreamField} for dataset components. */
    static String sanitiseDatasetComponent(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        var sb = new StringBuilder(value.length());
        int idx = 0;
        while (idx < value.length()) {
            int cp = value.codePointAt(idx);
            if (cp < 128 && DISALLOWED_DATASET_RUNES.indexOf(cp) >= 0) {
                sb.append('_');
            } else {
                sb.appendCodePoint(Character.toLowerCase(cp));
            }
            idx += Character.charCount(cp);
        }
        var sanitised = sb.toString();
        // Truncate to the byte budget — defensively done on char length to avoid splitting a multi-byte
        // codepoint at the boundary. For ASCII inputs (the realistic case for Gravitee org / env ids) the
        // two are equivalent; for non-ASCII we may undershoot, never overshoot.
        if (sanitised.getBytes(StandardCharsets.UTF_8).length > MAX_DATA_STREAM_BYTES) {
            return sanitised.substring(0, Math.min(sanitised.length(), MAX_DATA_STREAM_BYTES));
        }
        return sanitised;
    }
}
