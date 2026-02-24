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

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesBucketResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AggregationAdapter {

    static final String ES_BUCKETS_PROP = "buckets";
    static final String ES_KEY_AS_STRING_PROP = "key_as_string";
    static final String ES_KEY_PROP = "key";
    static final String ES_COUNT_PROP = "count";
    static final String ES_DOC_COUNT_PROP = "doc_count";
    static final String ES_VALUE_PROP = "value";
    static final String ES_VALUES_PROP = "values";

    static final String AGG_NAME_SEPARATOR = "#";

    static final String TIME_SERIES_AGG_NAME = "TIME_SERIES";

    private AggregationAdapter() {}

    /**
     *  Because queries are supposed to handle several metrics at once, we use
     *  an internal formalism for aggregation names to avoid duplicates, appending
     *  the name of the aggregation to the metric name, using a '#' as a separator.
     */
    public static String adaptName(Metric metric, Measure measure) {
        return metric.name() + AGG_NAME_SEPARATOR + measure.name();
    }

    public static String adaptName(Metric metric, Facet facet) {
        return metric.name() + AGG_NAME_SEPARATOR + facet.name();
    }

    public static String adaptName(Metric metric, String customAggName) {
        return metric.name() + AGG_NAME_SEPARATOR + customAggName;
    }

    public static List<MetricTimeSeriesResult> toMetricsAndTimeSeries(Map<String, Aggregation> aggregations, TimeSeriesQuery query) {
        var result = new ArrayList<MetricTimeSeriesResult>();
        for (var metric : query.metrics()) {
            var aggName = adaptName(metric.metric(), TIME_SERIES_AGG_NAME);
            var agg = aggregations.get(aggName);
            result.add(new MetricTimeSeriesResult(metric.metric(), toTimeSeriesBucketResults(metric.metric(), agg, query)));
        }
        return result;
    }

    public static List<MetricFacetsResult> toMetricsAndBuckets(Map<String, Aggregation> aggregations, FacetsQuery query) {
        var result = new ArrayList<MetricFacetsResult>();
        for (var metric : query.metrics()) {
            var aggNames = metric
                .measures()
                .stream()
                .map(measure -> adaptName(metric.metric(), measure))
                .toList();
            var facets = new ArrayList<>(query.facets());
            result.add(
                new MetricFacetsResult(metric.metric(), toFacetBucketResults(metric.metric(), aggregations, facets, aggNames, query))
            );
        }
        return result;
    }

    public static Map<Metric, Map<Measure, Number>> toMetricsAndMeasures(Map<String, Aggregation> aggregations, Query query) {
        var metricsAndMeasures = new HashMap<Metric, Map<Measure, Number>>();
        var aggNames = adaptNames(query);

        for (var entry : aggregations.entrySet()) {
            var aggName = entry.getKey();
            var agg = entry.getValue();

            if (aggNames.contains(aggName)) {
                var metric = getMetricFromName(aggName);
                var measures = metricsAndMeasures.computeIfAbsent(metric, m -> new HashMap<>());
                var measure = getMeasureFromName(aggName);
                measures.put(measure, getValue(agg));
                continue;
            }

            if (agg.getAggregations() != null && !agg.getAggregations().isEmpty()) {
                var nested = toMetricsAndMeasures(agg.getAggregations(), query);
                for (var nestedEntry : nested.entrySet()) {
                    metricsAndMeasures.computeIfAbsent(nestedEntry.getKey(), m -> new HashMap<>()).putAll(nestedEntry.getValue());
                }
                continue;
            }

            if (agg.getBuckets() != null) {
                for (var bucket : agg.getBuckets()) {
                    for (var name : aggNames) {
                        if (bucket.hasNonNull(name)) {
                            var metric = getMetricFromName(name);
                            var measures = metricsAndMeasures.computeIfAbsent(metric, m -> new HashMap<>());
                            var measure = getMeasureFromName(name);
                            measures.put(measure, getValue(bucket.get(name)));
                        }
                    }
                }
            }
        }
        return metricsAndMeasures;
    }

    private static Map<Measure, Number> getMeasuresWithDefaults(
        Metric metric,
        Map<Metric, Map<Measure, Number>> metricAndMeasures,
        Query query
    ) {
        var measures = metricAndMeasures.get(metric);
        if (measures != null) {
            return measures;
        }

        var expectedMeasures = query
            .metrics()
            .stream()
            .filter(m -> m.metric() == metric)
            .flatMap(m -> m.measures().stream())
            .toList();

        var defaultMeasures = new HashMap<Measure, Number>();
        for (var measure : expectedMeasures) {
            defaultMeasures.put(measure, 0);
        }
        return defaultMeasures;
    }

    private static List<TimeSeriesBucketResult> toTimeSeriesBucketResults(Metric metric, Aggregation aggregation, TimeSeriesQuery query) {
        var result = new ArrayList<TimeSeriesBucketResult>();
        var buckets = aggregation.getBuckets();

        var aggNames = query
            .metrics()
            .stream()
            .filter(m -> m.metric() == metric)
            .flatMap(m -> m.measures().stream())
            .map(measure -> adaptName(metric, measure))
            .toList();

        for (var bucket : buckets) {
            var key = bucket.get(ES_KEY_AS_STRING_PROP).asText();
            var timestamp = bucket.get(ES_KEY_PROP).asLong();

            if (query.facets() != null && !query.facets().isEmpty()) {
                var facets = new ArrayList<>(query.facets());
                var facetBucketResults = toFacetBucketResults(metric, bucket, facets, aggNames, query.toFacetQuery());
                result.add(TimeSeriesBucketResult.ofBuckets(key, timestamp, facetBucketResults));
            } else {
                var aggregations = toAggregations(bucket, aggNames);
                var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
                var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
                result.add(TimeSeriesBucketResult.ofMeasures(key, timestamp, measures));
            }
        }

        return result;
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        Metric metric,
        JsonNode bucket,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        if (facets.isEmpty()) {
            var aggregations = toAggregations(bucket, aggNames);
            var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
            var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
            var key = bucket.get(ES_KEY_PROP).asText();
            return List.of(FacetBucketResult.ofMeasures(key, measures));
        }

        var nextFacets = new ArrayList<>(facets);
        var nextFacet = nextFacets.removeFirst();
        var nextAggName = adaptName(metric, nextFacet);
        var next = bucket.get(nextAggName);

        if (next == null) {
            return List.of();
        }

        var facetBuckets = toBucketList(next.get(ES_BUCKETS_PROP));
        var results = new ArrayList<FacetBucketResult>();

        for (var facetBucket : facetBuckets) {
            var key = facetBucket.get(ES_KEY_PROP).asText();
            var nestedResults = toFacetBucketResults(metric, facetBucket, new ArrayList<>(nextFacets), aggNames, query);

            if (nextFacets.isEmpty()) {
                results.addAll(nestedResults);
            } else {
                results.add(FacetBucketResult.ofBuckets(key, nestedResults));
            }
        }

        return results;
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        Metric metric,
        Map<String, Aggregation> aggregations,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        List<FacetBucketResult> results = new ArrayList<>();
        var facet = facets.removeFirst();
        var aggName = adaptName(metric, facet);
        var agg = aggregations.get(aggName);
        var buckets = agg.getBuckets();
        for (var bucket : buckets) {
            results.add(toFacetBucketResult(metric, bucket, facets, aggNames, query));
        }
        return results;
    }

    private static FacetBucketResult toFacetBucketResult(
        Metric metric,
        JsonNode bucket,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        var key = bucket.get(ES_KEY_PROP).asText();
        if (facets.isEmpty()) {
            var aggregations = toAggregations(bucket, aggNames);
            var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
            var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
            return FacetBucketResult.ofMeasures(key, measures);
        }
        var nextFacets = new ArrayList<>(facets);
        var nextFacet = nextFacets.removeFirst();
        var nextAggName = adaptName(metric, nextFacet);
        var next = bucket.get(nextAggName);
        var buckets = next.get(ES_BUCKETS_PROP);
        return FacetBucketResult.ofBuckets(key, toFacetBucketResults(metric, toBucketList(buckets), nextFacets, aggNames, query));
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        Metric metric,
        List<JsonNode> buckets,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        var results = new ArrayList<FacetBucketResult>();
        if (facets.isEmpty()) {
            for (var bucket : buckets) {
                var key = bucket.get(ES_KEY_PROP).asText();
                var aggregations = toAggregations(bucket, aggNames);
                var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
                var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
                results.add(FacetBucketResult.ofMeasures(key, measures));
            }
            return results;
        }

        var nextFacets = new ArrayList<>(facets);
        var nextFacet = nextFacets.removeFirst();
        var nextAggName = adaptName(metric, nextFacet);

        for (var bucket : buckets) {
            var key = bucket.get(ES_KEY_PROP).asText();
            var next = bucket.get(nextAggName);
            if (next == null) {
                continue;
            }
            var nextBuckets = next.get(ES_BUCKETS_PROP);
            results.add(
                FacetBucketResult.ofBuckets(
                    key,
                    toFacetBucketResults(metric, toBucketList(nextBuckets), new ArrayList<>(nextFacets), aggNames, query)
                )
            );
        }

        return results;
    }

    private static Map<String, Aggregation> toAggregations(JsonNode bucket, List<String> aggNames) {
        var aggregations = new HashMap<String, Aggregation>();
        for (var name : aggNames) {
            if (bucket.hasNonNull(name)) {
                aggregations.put(name, toAggregation(bucket.get(name)));
            }
            if (bucket.hasNonNull("_" + name)) {
                aggregations.put("_" + name, toAggregation(bucket.get("_" + name)));
            }
        }
        return aggregations;
    }

    private static Aggregation toAggregation(JsonNode aggNode) {
        var aggregation = new Aggregation();
        if (aggNode.hasNonNull(ES_VALUE_PROP) && aggNode.get(ES_VALUE_PROP).isNumber()) {
            aggregation.setValue(aggNode.get(ES_VALUE_PROP).floatValue());
        }
        if (aggNode.hasNonNull(ES_COUNT_PROP) && aggNode.get(ES_COUNT_PROP).isNumber()) {
            aggregation.setCount(aggNode.get(ES_COUNT_PROP).floatValue());
        }
        if (aggNode.hasNonNull(ES_VALUES_PROP) && aggNode.get(ES_VALUES_PROP).isObject()) {
            var values = new HashMap<String, Float>();
            var jsonValues = aggNode.get(ES_VALUES_PROP);
            jsonValues
                .fields()
                .forEachRemaining(entry -> {
                    if (entry.getValue().isNumber()) {
                        values.put(entry.getKey(), entry.getValue().floatValue());
                    }
                });
            aggregation.setValues(values);
        }
        if (aggNode.get(ES_BUCKETS_PROP) != null && aggNode.get(ES_BUCKETS_PROP).isArray()) {
            aggregation.setBuckets(toBucketList(aggNode.get(ES_BUCKETS_PROP)));
        }
        return aggregation;
    }

    private static List<JsonNode> toBucketList(JsonNode buckets) {
        return StreamSupport.stream(buckets.spliterator(), false).toList();
    }

    private static List<String> adaptNames(Query query) {
        var names = new ArrayList<String>();
        for (var metric : query.metrics()) {
            for (var measure : metric.measures()) {
                names.add(adaptName(metric.metric(), measure));
            }
        }
        return names;
    }

    private static Number getValue(Aggregation agg) {
        return lookupForCount(agg)
            .or(() -> lookupForValue(agg))
            .or(() -> lookupForValues(agg))
            .orElse(0);
    }

    private static Number getValue(JsonNode bucketNode) {
        var value = bucketNode.get(ES_VALUE_PROP);
        if (value != null && value.isNumber()) {
            return value.asDouble();
        }
        var values = bucketNode.get(ES_VALUES_PROP);
        if (values != null && values.isArray()) {
            return values.get(0).asDouble();
        }
        return 0;
    }

    private static Optional<Number> lookupForCount(Aggregation agg) {
        return ofNullable(agg.getCount());
    }

    private static Optional<Number> lookupForValue(Aggregation agg) {
        return ofNullable(agg.getValue());
    }

    private static Optional<Number> lookupForValues(Aggregation agg) {
        return ofNullable(agg.getValues())
            .flatMap(keyValues -> ofNullable(keyValues.values()))
            .flatMap(values -> values.stream().filter(Objects::nonNull).findFirst())
            .map(Float::floatValue);
    }

    private static Metric getMetricFromName(String aggregationName) {
        return Metric.valueOf(aggregationName.split(AGG_NAME_SEPARATOR)[0]);
    }

    private static Measure getMeasureFromName(String aggregationName) {
        return Measure.valueOf(aggregationName.split(AGG_NAME_SEPARATOR)[1]);
    }
}
