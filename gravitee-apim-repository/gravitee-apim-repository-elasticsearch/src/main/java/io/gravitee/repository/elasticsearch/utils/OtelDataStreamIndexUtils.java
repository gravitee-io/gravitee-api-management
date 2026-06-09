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

/**
 * Resolves an OTel ES data stream index template by substituting {@code {placeholder}} tokens with
 * their caller-supplied values, applying the same normalisation the OpenTelemetry collector's
 * {@code elasticsearchexporter} applies before writing.
 *
 * <p>Why this isn't {@code IndexNameUtils.format}: that utility lowercases substituted values but
 * leaves their other characters intact, which is the right behaviour for legacy analytics indices
 * that the gateway writes directly. The OTel collector ES exporter, in contrast, runs each data
 * stream component through {@code sanitizeDataStreamField} — replacing characters in a "disallowed
 * runes" set with {@code _} and then lowercasing — before forming the final data stream name.
 *
 * <p><strong>Position matters.</strong> The exporter uses two different disallowed sets:
 * <ul>
 *   <li><b>Dataset</b>: {@code DISALLOWED_DATASET_RUNES = "-\\/*?\"<>| ,#:"} — hyphen included.
 *   So an org id like {@code my-org-1} stored as the dataset becomes {@code my_org_1}.</li>
 *   <li><b>Namespace</b>: {@code DISALLOWED_NAMESPACE_RUNES = "\\/*?\"<>| ,#:"} — hyphen <em>not</em>
 *   included. The same {@code my-org-1} stored as the namespace stays {@code my-org-1}.</li>
 * </ul>
 *
 * <p>Templates follow the OTel ES exporter shape {@code <type>-<dataset>.otel-<namespace>}. The
 * literal {@code .otel-} marker splits the two halves: placeholders appearing before it land in the
 * dataset slot (strict rule, hyphens stripped); placeholders after it land in the namespace slot
 * (looser rule, hyphens preserved). Applying the dataset rule uniformly would silently strip hyphens
 * from namespace-position placeholders, producing index names that don't match what the collector
 * actually writes — every look-up misses.
 *
 * <p>Templates with no {@code .otel-} marker fall back to the dataset rule. Same-placeholder-in-both-
 * slots (unusual but possible) uses the stricter dataset rule so the value is safe everywhere.
 *
 * <p>Source of the rune sets + algorithm:
 * {@code https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/elasticsearchexporter/data_stream_router.go}
 * — see {@code sanitizeDataStreamField}, {@code disallowedDatasetRunes},
 * {@code disallowedNamespaceRunes}, and {@code maxDataStreamBytes}.
 *
 * @author GraviteeSource Team
 */
public final class OtelDataStreamIndexUtils {

    /**
     * Characters disallowed in OTel data stream dataset components — superset of the namespace
     * disallowed set (it adds the hyphen). The OTel ES exporter replaces these with {@code _}.
     */
    private static final String DISALLOWED_DATASET_RUNES = "-\\/*?\"<>| ,#:";

    /**
     * Characters disallowed in OTel data stream namespace components — same set as dataset minus the
     * hyphen. Lets per-tenant ids that contain hyphens (Cockpit-style HRIDs, UUIDs) survive the
     * substitution and match what the exporter actually writes.
     */
    private static final String DISALLOWED_NAMESPACE_RUNES = "\\/*?\"<>| ,#:";

    /** Max byte length of a sanitised dataset / namespace component, matching the exporter constant. */
    private static final int MAX_DATA_STREAM_BYTES = 100;

    /** Literal marker separating the dataset (left) and namespace (right) halves of an OTel template. */
    private static final String OTEL_TEMPLATE_MARKER = ".otel-";

    private OtelDataStreamIndexUtils() {}

    /**
     * Substitutes {@code {placeholder}} tokens in {@code template}, sanitising each replacement
     * value with the OTel rule that matches the placeholder's position in the template — dataset for
     * placeholders left of {@code .otel-}, namespace for placeholders right of it. Both rules
     * lowercase the result and truncate to {@value #MAX_DATA_STREAM_BYTES} bytes; the difference is
     * whether the hyphen survives.
     */
    public static String format(String template, Map<String, String> parameters) {
        int markerAt = template.indexOf(OTEL_TEMPLATE_MARKER);
        String datasetSlot = markerAt >= 0 ? template.substring(0, markerAt) : template;

        var resolved = template;
        for (var entry : parameters.entrySet()) {
            var placeholder = "{" + entry.getKey() + "}";
            // A placeholder living in both slots is unusual but legal — use the stricter dataset rule
            // so the substituted value is also safe in the dataset slot (a hyphen would otherwise be
            // illegal there). Pure namespace-only placeholders keep their hyphens.
            boolean inDataset = datasetSlot.contains(placeholder);
            var sanitised = inDataset ? sanitiseDatasetComponent(entry.getValue()) : sanitiseNamespaceComponent(entry.getValue());
            resolved = resolved.replace(placeholder, sanitised);
        }
        return resolved;
    }

    /** Visible for test. Mirrors the OTel exporter's {@code sanitizeDataStreamField} for dataset components. */
    static String sanitiseDatasetComponent(String value) {
        return sanitiseAgainst(value, DISALLOWED_DATASET_RUNES);
    }

    /** Visible for test. Mirrors the OTel exporter's {@code sanitizeDataStreamField} for namespace components. */
    static String sanitiseNamespaceComponent(String value) {
        return sanitiseAgainst(value, DISALLOWED_NAMESPACE_RUNES);
    }

    private static String sanitiseAgainst(String value, String disallowedRunes) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        var sb = new StringBuilder(value.length());
        int idx = 0;
        while (idx < value.length()) {
            int cp = value.codePointAt(idx);
            if (cp < 128 && disallowedRunes.indexOf(cp) >= 0) {
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
