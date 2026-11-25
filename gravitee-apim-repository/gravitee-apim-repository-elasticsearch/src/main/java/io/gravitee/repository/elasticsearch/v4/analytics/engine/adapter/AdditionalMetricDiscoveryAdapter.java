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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.Root;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Aggs;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Keys;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens;
import io.gravitee.repository.log.v4.model.analytics.AdditionalMetricLastSeen;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The two steps of an additional metric discovery on the v4 metrics indexes: the keys their mapping holds under the
 * {@code additional-metrics} and {@code custom} maps ({@code _field_caps}, shared by every environment of the cluster),
 * then which of them the documents of one environment carry ({@code exists}, one filter per key) and when they last did.
 * Keys are handled by their wire name, the path of the field, so a custom key spelt like an additional raw key is a key
 * of its own.
 */
public final class AdditionalMetricDiscoveryAdapter {

    /**
     * A safety bound on the keys one environment's pass probes, for a mapping far above the default
     * {@code index.mapping.total_fields.limit} (1 000 per index). Below it every key of the mapping is probed, so no
     * environment loses a key it carries to another environment's keys; the pass's deadline bounds the cost. Above it the
     * pass says it is truncated. The cap on listed entries (200) is applied by the caller after the per-environment
     * pass, never here.
     */
    public static final int MAX_PROBED_KEYS = 5_000;

    /** The type each prefix is stored under by the v4 metrics index templates (es7x, es8x, es9x, opensearch). */
    private static final Map<String, String> ES_TYPE_BY_PREFIX = Map.of(
        "keyword_",
        "keyword",
        "long_",
        "long",
        "int_",
        "integer",
        "double_",
        "double",
        "bool_",
        "boolean",
        "string_",
        "text",
        "json_",
        "text"
    );

    /** The type the v4 metrics index templates store every string of the {@code custom} map under. */
    private static final String CUSTOM_ES_TYPE = "keyword";

    private static final String ENV_ID_FIELD = "env-id";
    private static final String EXISTS = "exists";
    private static final String FILTERS = "filters";
    private static final String TIMEOUT = "timeout";
    private static final String KEYS_AGG = "keys";
    private static final String LAST_SEEN_AGG = "last_seen";

    private AdditionalMetricDiscoveryAdapter() {}

    /**
     * @param names     the wire names to probe
     * @param truncated true when the mapping had more keys than {@link #MAX_PROBED_KEYS}
     */
    public record Candidates(List<String> names, boolean truncated) {}

    /**
     * The keys worth probing among the fields of the mapping, by wire name: the {@code additional-metrics} and the
     * {@code custom} keys, each sorted, taken in turn so that a deadline cutting the probes batch by batch leaves both
     * maps probed, then cut to {@link #MAX_PROBED_KEYS}. A key is left out when it holds a {@code *}, which
     * an {@code exists} query reads as a pattern; an additional metric key when its prefix is unknown or when any index
     * stores it under another type than its prefix declares: only the {@code long_} template is bound to the JSON type of
     * the value, so a {@code long_} key written as a decimal is mapped as a floating-point field; a custom key when any
     * index stores it as something else than a keyword: a date the template detected, or the object parent of a dotted
     * key.
     *
     * @param typesByField the types of each field, across the indexes
     */
    public static Candidates candidates(Map<String, Set<String>> typesByField) {
        var names = typesByField
            .entrySet()
            .stream()
            .filter(field -> !field.getKey().contains("*"))
            .filter(field -> isTypedAsDeclared(field.getKey(), field.getValue()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
        var additional = names
            .stream()
            .filter(name -> Root.of(name).orElseThrow() == Root.ADDITIONAL_METRICS)
            .toList();
        var custom = names
            .stream()
            .filter(name -> Root.of(name).orElseThrow() == Root.CUSTOM)
            .toList();
        var alternated = new ArrayList<String>(names.size());
        for (int i = 0; i < Math.max(additional.size(), custom.size()); i++) {
            if (i < additional.size()) {
                alternated.add(additional.get(i));
            }
            if (i < custom.size()) {
                alternated.add(custom.get(i));
            }
        }
        return alternated.size() > MAX_PROBED_KEYS
            ? new Candidates(List.copyOf(alternated.subList(0, MAX_PROBED_KEYS)), true)
            : new Candidates(List.copyOf(alternated), false);
    }

    private static boolean isTypedAsDeclared(String name, Set<String> esTypes) {
        return Root.of(name)
            .map(root -> {
                var rawKey = name.substring(root.prefix().length());
                return switch (root) {
                    case ADDITIONAL_METRICS -> ES_TYPE_BY_PREFIX.entrySet()
                        .stream()
                        .filter(prefix -> rawKey.startsWith(prefix.getKey()))
                        .anyMatch(prefix -> Set.of(prefix.getValue()).equals(esTypes));
                    case CUSTOM -> !rawKey.isEmpty() && Set.of(CUSTOM_ES_TYPE).equals(esTypes);
                };
            })
            .orElse(false);
    }

    /** One search counting nothing: a filter per key on the documents of the environment over the window. */
    public static String query(String environmentId, Instant from, Instant to, List<String> names, Duration timeout) {
        var exists = new JsonArray();
        names.forEach(name -> exists.add(JsonObject.of(EXISTS, JsonObject.of(Tokens.FIELD, name))));
        return JsonObject.of(
            Keys.SIZE,
            0,
            Keys.TRACK_TOTAL_HITS,
            false,
            TIMEOUT,
            timeout.toMillis() + "ms",
            Keys.QUERY,
            JsonObject.of(
                Keys.BOOL,
                JsonObject.of(
                    Keys.FILTER,
                    JsonArray.of(
                        JsonObject.of(Query.TERM, JsonObject.of(ENV_ID_FIELD, environmentId)),
                        JsonObject.of(
                            Query.RANGE,
                            JsonObject.of(Keys.TIMESTAMP, JsonObject.of(Query.GTE, from.toEpochMilli(), Query.LTE, to.toEpochMilli()))
                        )
                    )
                )
            ),
            Keys.AGGS,
            JsonObject.of(
                KEYS_AGG,
                JsonObject.of(
                    FILTERS,
                    JsonObject.of(FILTERS, exists),
                    Keys.AGGS,
                    JsonObject.of(LAST_SEEN_AGG, JsonObject.of(Aggs.MAX, JsonObject.of(Tokens.FIELD, Keys.TIMESTAMP)))
                )
            )
        ).encode();
    }

    /**
     * The keys of {@link #query} carried by at least one document of the environment; the buckets come back in the order
     * of the keys. A key with no document is left out: the mapping lists the keys of every environment.
     */
    public static List<AdditionalMetricLastSeen> found(List<String> names, SearchResponse response) {
        if (response.getAggregations() == null || response.getAggregations().get(KEYS_AGG) == null) {
            return List.of();
        }
        List<JsonNode> buckets = response.getAggregations().get(KEYS_AGG).getBuckets();
        var found = new ArrayList<AdditionalMetricLastSeen>();
        for (int i = 0; i < buckets.size() && i < names.size(); i++) {
            var bucket = buckets.get(i);
            if (bucket.path(Keys.DOC_COUNT).asLong() > 0) {
                var lastSeen = Instant.ofEpochMilli(bucket.path(LAST_SEEN_AGG).path("value").asLong());
                var name = names.get(i);
                Root.of(name).ifPresent(root ->
                    found.add(new AdditionalMetricLastSeen(root, name.substring(root.prefix().length()), lastSeen))
                );
            }
        }
        return found;
    }

    /** Most recently seen first, then by wire name. */
    public static List<AdditionalMetricLastSeen> mostRecentFirst(List<AdditionalMetricLastSeen> metrics) {
        return metrics
            .stream()
            .sorted(Comparator.comparing(AdditionalMetricLastSeen::lastSeen).reversed().thenComparing(AdditionalMetricLastSeen::name))
            .toList();
    }
}
