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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.DateHistoQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Adapts {@link DateHistoQuery} to an Elasticsearch {@code date_histogram} query and parses the response.
 *
 * <p>Follows the same pattern as {@link SearchGroupByQueryAdapter} and {@link SearchStatsQueryAdapter}:
 * Vert.x {@code JsonObject} for query building, {@code ElasticsearchInfo} for version-aware interval field names.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchDateHistoQueryAdapter {

    static final String AGG_BY_DATE = "by_date";
    static final String AGG_BY_FIELD = "by_field";

    private static final String TIME_FIELD = "@timestamp";

    /**
     * Serialises a {@link DateHistoQuery} to an Elasticsearch {@code date_histogram} JSON request.
     *
     * <p>{@code esInfo} is required to select the correct interval field name:
     * ES 7.2+ uses {@code "fixed_interval"} while older versions use the deprecated
     * {@code "interval"} parameter — see {@link io.gravitee.elasticsearch.version.ElasticsearchVersion#canUseDateHistogramFixedInterval()}.</p>
     *
     * @param query  the histogram parameters (field, interval, time bounds, API filter)
     * @param esInfo runtime Elasticsearch version info used for version-aware query building
     * @return a JSON string ready to be posted to the {@code _search} endpoint
     */
    public static String adaptQuery(DateHistoQuery query, ElasticsearchInfo esInfo) {
        var jsonContent = new HashMap<String, Object>();
        jsonContent.put("size", 0);
        var esQuery = buildElasticQuery(query);
        if (esQuery != null) {
            jsonContent.put("query", esQuery);
        }
        jsonContent.put("aggregations", buildAggregations(query, esInfo));
        return new JsonObject(jsonContent).encode();
    }

    /**
     * Parses an Elasticsearch {@code date_histogram} response into a {@link DateHistoAggregate}.
     *
     * <p>Design notes:
     * <ul>
     *   <li>A {@link TreeMap} is used for {@code timestampToFieldCounts} so that the epoch-ms
     *       keys are iterated in ascending chronological order without an explicit sort step.</li>
     *   <li>A {@link TreeSet} is used for {@code fieldValues} so that the series order is
     *       deterministic (natural string ordering of field values like HTTP status codes),
     *       making the parallel arrays stable across calls.</li>
     *   <li>The {@code counts} array for every bucket is aligned to the {@code timestamps}
     *       list — missing sub-buckets (field values with zero hits in a time slot) are
     *       filled with {@code 0L} via {@link Map#getOrDefault}.</li>
     * </ul></p>
     *
     * @param response the raw Elasticsearch search response
     * @return empty if the response contains no aggregation buckets
     */
    public static Optional<DateHistoAggregate> adaptResponse(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }

        final var byDateAgg = aggregations.get(AGG_BY_DATE);
        if (byDateAgg == null || byDateAgg.getBuckets() == null || byDateAgg.getBuckets().isEmpty()) {
            return Optional.empty();
        }

        // Collect timestamps and per-field counts in a TreeMap to preserve time order
        var timestampToFieldCounts = new TreeMap<Long, Map<String, Long>>();
        var fieldValues = new TreeSet<String>();

        for (var dateBucket : byDateAgg.getBuckets()) {
            var epochMs = dateBucket.get("key").asLong();
            var fieldCount = new HashMap<String, Long>();

            var byFieldNode = dateBucket.get(AGG_BY_FIELD);
            if (byFieldNode != null) {
                for (var fieldBucket : byFieldNode.get("buckets")) {
                    var fieldValue = fieldBucket.get("key").asText();
                    var count = fieldBucket.get("doc_count").asLong();
                    fieldCount.put(fieldValue, count);
                    fieldValues.add(fieldValue);
                }
            }
            timestampToFieldCounts.put(epochMs, fieldCount);
        }

        var timestamps = new ArrayList<>(timestampToFieldCounts.keySet());

        // Build one Bucket per distinct field value with counts parallel to timestamps
        var buckets = new ArrayList<DateHistoAggregate.Bucket>();
        for (var fieldValue : fieldValues) {
            var counts = new ArrayList<Long>();
            for (var entry : timestampToFieldCounts.entrySet()) {
                counts.add(entry.getValue().getOrDefault(fieldValue, 0L));
            }
            var metadata = new LinkedHashMap<String, String>();
            metadata.put("name", fieldValue);
            buckets.add(DateHistoAggregate.Bucket.builder().field(fieldValue).counts(counts).metadata(metadata).build());
        }

        return Optional.of(DateHistoAggregate.builder().timestamps(timestamps).buckets(buckets).build());
    }

    // -------------------------------------------------------------------------
    // Query builders — Vert.x JsonObject, consistent with SearchGroupByQueryAdapter / SearchStatsQueryAdapter
    // -------------------------------------------------------------------------

    private static JsonObject buildElasticQuery(DateHistoQuery query) {
        var terms = new ArrayList<JsonObject>();

        query.apiId().ifPresent(apiId -> terms.add(JsonObject.of("term", JsonObject.of("api-id", apiId))));

        // Only add the range clause when at least one bound is present.
        // A range query with only include_lower/include_upper and no gte/lte is rejected by ES 7+ with parse_exception.
        if (query.from().isPresent() || query.to().isPresent()) {
            var timestamp = new JsonObject();
            query.from().ifPresent(from -> timestamp.put("from", from.toEpochMilli()).put("include_lower", true));
            query.to().ifPresent(to -> timestamp.put("to", to.toEpochMilli()).put("include_upper", true));
            terms.add(JsonObject.of("range", JsonObject.of(TIME_FIELD, timestamp)));
        }

        if (terms.isEmpty()) {
            return null;
        }

        return JsonObject.of("bool", JsonObject.of("filter", JsonArray.of(terms.toArray())));
    }

    private static JsonObject buildAggregations(DateHistoQuery query, ElasticsearchInfo esInfo) {
        String intervalFieldName = esInfo.getVersion().canUseDateHistogramFixedInterval() ? "fixed_interval" : "interval";

        var histogram = new JsonObject()
            .put("field", TIME_FIELD)
            .put(intervalFieldName, query.interval().toMillis() + "ms")
            .put("min_doc_count", 0);

        query
            .from()
            .ifPresent(from ->
                query
                    .to()
                    .ifPresent(to ->
                        histogram.put("extended_bounds", JsonObject.of("min", from.toEpochMilli(), "max", to.toEpochMilli()))
                    )
            );

        var subAgg = JsonObject.of(AGG_BY_FIELD, JsonObject.of("terms", JsonObject.of("field", query.field())));

        return JsonObject.of(AGG_BY_DATE, new JsonObject().put("date_histogram", histogram).put("aggregations", subAgg));
    }
}
