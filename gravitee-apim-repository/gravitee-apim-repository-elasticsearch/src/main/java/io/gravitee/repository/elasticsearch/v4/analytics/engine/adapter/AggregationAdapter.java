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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.repository.analytics.engine.api.metric.AdditionalMetric;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.metric.MetricName;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesBucketResult;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimplePercentileBuilder;
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

    static final String FILTER_AGG_SUFFIX = "__FILTER__";

    static final String TIME_SERIES_AGG_NAME = "TIME_SERIES";

    private AggregationAdapter() {}

    /**
     *  Because queries are supposed to handle several metrics at once, we use
     *  an internal formalism for aggregation names to avoid duplicates, appending
     *  the name of the aggregation to the metric name, using a '#' as a separator.
     */
    public static String adaptName(MetricName metric, Measure measure) {
        return metric.name() + AGG_NAME_SEPARATOR + measure.name();
    }

    public static String adaptName(MetricName metric, FacetName facet) {
        return metric.name() + AGG_NAME_SEPARATOR + facet.name();
    }

    public static String adaptName(MetricName metric, String customAggName) {
        return metric.name() + AGG_NAME_SEPARATOR + customAggName;
    }

    /**
     * The path a terms aggregation orders its buckets by. A percentile is a multi-value aggregation, read by its percent
     * in brackets; so is the value of an additional metric's aggregation, whose name holds dots Elasticsearch would
     * otherwise read as the separator of a metric key.
     */
    public static String orderPath(MetricName metric, Measure measure) {
        var aggName = adaptName(metric, measure);
        return SimplePercentileBuilder.percent(measure)
            .map(percent -> aggName + "[" + percent + "]")
            .orElseGet(() -> metric instanceof AdditionalMetric ? aggName + "[" + ES_VALUE_PROP + "]" : aggName);
    }

    /**
     * What a measure reads where no document matches. A built-in reads 0. An additional metric reads what
     * Elasticsearch answers over no document carrying its field: 0 for a sum or a count, no value otherwise.
     */
    public static Number emptyValue(MetricName metric, Measure measure) {
        return switch (metric) {
            case Metric _ -> 0;
            case AdditionalMetric _ -> measure == Measure.SUM || measure == Measure.COUNT ? 0 : null;
        };
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

    public static Map<MetricName, Map<Measure, Number>> toMetricsAndMeasures(Map<String, Aggregation> aggregations, Query query) {
        var metricsAndMeasures = new HashMap<MetricName, Map<Measure, Number>>();
        var aggNames = adaptNames(query);

        for (var entry : aggregations.entrySet()) {
            var aggName = entry.getKey();
            var agg = entry.getValue();

            if (aggNames.containsKey(aggName)) {
                var measured = aggNames.get(aggName);
                var measures = metricsAndMeasures.computeIfAbsent(measured.metric(), m -> new HashMap<>());
                measures.put(measured.measure(), getValue(measured.metric(), agg));
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
                    aggNames.forEach((name, measured) -> {
                        if (bucket.hasNonNull(name)) {
                            var measures = metricsAndMeasures.computeIfAbsent(measured.metric(), m -> new HashMap<>());
                            measures.put(measured.measure(), getValue(measured.metric(), bucket.get(name)));
                        }
                    });
                }
            }
        }
        return metricsAndMeasures;
    }

    private static Map<Measure, Number> getMeasuresWithDefaults(
        MetricName metric,
        Map<MetricName, Map<Measure, Number>> metricAndMeasures,
        Query query
    ) {
        var measures = metricAndMeasures.get(metric);
        if (measures != null) {
            return measures;
        }

        var expectedMeasures = query
            .metrics()
            .stream()
            .filter(m -> m.metric().equals(metric))
            .flatMap(m -> m.measures().stream())
            .toList();

        var defaultMeasures = new HashMap<Measure, Number>();
        for (var measure : expectedMeasures) {
            defaultMeasures.put(measure, emptyValue(metric, measure));
        }
        return defaultMeasures;
    }

    private static List<TimeSeriesBucketResult> toTimeSeriesBucketResults(
        MetricName metric,
        Aggregation aggregation,
        TimeSeriesQuery query
    ) {
        var result = new ArrayList<TimeSeriesBucketResult>();
        var buckets = aggregation.getBuckets();

        var aggNames = query
            .metrics()
            .stream()
            .filter(m -> m.metric().equals(metric))
            .flatMap(m -> m.measures().stream())
            .map(measure -> adaptName(metric, measure))
            .toList();

        for (var bucket : buckets) {
            var key = bucket.get(ES_KEY_AS_STRING_PROP).asText();
            var timestamp = bucket.get(ES_KEY_PROP).asLong();

            if (query.facets() != null && !query.facets().isEmpty()) {
                var facets = new ArrayList<>(query.facets());
                var facetBucketResults = toFacetBucketResults(metric, bucket, null, facets, aggNames, query.toFacetQuery());
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

    /**
     * @param bucketFacet the facet {@code bucket} belongs to; {@code null} for a date histogram bucket, whose key is
     *                    never read here
     */
    private static List<FacetBucketResult> toFacetBucketResults(
        MetricName metric,
        JsonNode bucket,
        FacetName bucketFacet,
        List<FacetName> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        if (facets.isEmpty()) {
            var aggregations = toAggregations(bucket, aggNames);
            var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
            var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
            return List.of(FacetBucketResult.ofMeasures(bucketKey(bucketFacet, bucket), measures));
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
            var nestedResults = toFacetBucketResults(metric, facetBucket, nextFacet, new ArrayList<>(nextFacets), aggNames, query);

            if (nextFacets.isEmpty()) {
                results.addAll(nestedResults);
            } else {
                results.add(FacetBucketResult.ofBuckets(bucketKey(nextFacet, facetBucket), nestedResults));
            }
        }

        return results;
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        MetricName metric,
        Map<String, Aggregation> aggregations,
        List<FacetName> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        List<FacetBucketResult> results = new ArrayList<>();
        var facet = facets.removeFirst();
        var aggName = adaptName(metric, facet);
        var agg = aggregations.get(aggName);
        var buckets = agg.getBuckets();
        for (var bucket : buckets) {
            results.add(toFacetBucketResult(metric, facet, bucket, facets, aggNames, query));
        }
        return results;
    }

    private static FacetBucketResult toFacetBucketResult(
        MetricName metric,
        FacetName facet,
        JsonNode bucket,
        List<FacetName> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        var key = bucketKey(facet, bucket);
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
        return FacetBucketResult.ofBuckets(
            key,
            toFacetBucketResults(metric, toBucketList(buckets), nextFacet, nextFacets, aggNames, query)
        );
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        MetricName metric,
        List<JsonNode> buckets,
        FacetName bucketsFacet,
        List<FacetName> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        var results = new ArrayList<FacetBucketResult>();
        if (facets.isEmpty()) {
            for (var bucket : buckets) {
                var aggregations = toAggregations(bucket, aggNames);
                var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
                var measures = getMeasuresWithDefaults(metric, metricAndMeasures, query);
                results.add(FacetBucketResult.ofMeasures(bucketKey(bucketsFacet, bucket), measures));
            }
            return results;
        }

        var nextFacets = new ArrayList<>(facets);
        var nextFacet = nextFacets.removeFirst();
        var nextAggName = adaptName(metric, nextFacet);

        for (var bucket : buckets) {
            var key = bucketKey(bucketsFacet, bucket);
            var next = bucket.get(nextAggName);
            if (next == null) {
                continue;
            }
            var nextBuckets = next.get(ES_BUCKETS_PROP);
            results.add(
                FacetBucketResult.ofBuckets(
                    key,
                    toFacetBucketResults(metric, toBucketList(nextBuckets), nextFacet, new ArrayList<>(nextFacets), aggNames, query)
                )
            );
        }

        return results;
    }

    /** The gateway reports the HTTP method as its {@link HttpMethod#code()}; the catalog names it. */
    private static String bucketKey(FacetName facet, JsonNode bucket) {
        var key = bucket.get(ES_KEY_PROP);
        if (facet == Facet.HTTP_METHOD && key.canConvertToInt()) {
            return ofNullable(HttpMethod.get(key.asInt())).map(HttpMethod::name).orElseGet(key::asText);
        }
        // A terms aggregation keys a boolean bucket 1 or 0, and names it true or false in key_as_string.
        if (isBoolean(facet) && bucket.hasNonNull(ES_KEY_AS_STRING_PROP)) {
            return bucket.get(ES_KEY_AS_STRING_PROP).asText();
        }
        return key.asText();
    }

    private static boolean isBoolean(FacetName facet) {
        return facet instanceof AdditionalMetricField field && field.valueType() == AdditionalMetricField.ValueType.BOOL;
    }

    private static Map<String, Aggregation> toAggregations(JsonNode bucket, List<String> aggNames) {
        var aggregations = new HashMap<String, Aggregation>();
        for (var name : aggNames) {
            if (bucket.hasNonNull(name)) {
                aggregations.put(name, toAggregation(bucket.get(name)));
            } else {
                var filterAggName = getMetricPrefix(name) + AGG_NAME_SEPARATOR + FILTER_AGG_SUFFIX;
                var filterAggNode = bucket.path(filterAggName).path(name);
                if (!filterAggNode.isMissingNode()) {
                    aggregations.put(name, toAggregation(filterAggNode));
                }
            }
            if (bucket.hasNonNull("_" + name)) {
                aggregations.put("_" + name, toAggregation(bucket.get("_" + name)));
            }
        }
        return aggregations;
    }

    private static String getMetricPrefix(String aggName) {
        return aggName.split(AGG_NAME_SEPARATOR)[0];
    }

    private static Aggregation toAggregation(JsonNode aggNode) {
        var aggregation = new Aggregation();
        if (aggNode.hasNonNull(ES_VALUE_PROP) && aggNode.get(ES_VALUE_PROP).isNumber()) {
            aggregation.setValue(aggNode.get(ES_VALUE_PROP).doubleValue());
        }
        if (aggNode.hasNonNull(ES_COUNT_PROP) && aggNode.get(ES_COUNT_PROP).isNumber()) {
            aggregation.setCount(aggNode.get(ES_COUNT_PROP).doubleValue());
        }
        if (aggNode.hasNonNull(ES_VALUES_PROP) && aggNode.get(ES_VALUES_PROP).isObject()) {
            var values = new HashMap<String, Double>();
            var jsonValues = aggNode.get(ES_VALUES_PROP);
            jsonValues
                .fields()
                .forEachRemaining(entry -> {
                    if (entry.getValue().isNumber()) {
                        values.put(entry.getKey(), entry.getValue().doubleValue());
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

    /**
     * The metric and measure of each aggregation the query asks for, by name. The name is read from the query rather
     * than parsed back: an additional metric's name is its wire name, which only the core parses.
     */
    private static Map<String, MeasuredMetric> adaptNames(Query query) {
        var names = new HashMap<String, MeasuredMetric>();
        for (var metric : query.metrics()) {
            for (var measure : metric.measures()) {
                names.put(adaptName(metric.metric(), measure), new MeasuredMetric(metric.metric(), measure));
            }
        }
        return names;
    }

    private record MeasuredMetric(MetricName metric, Measure measure) {}

    /** An additional metric keeps Elasticsearch's absent value, and reads NaN as one too: it is not 0. */
    private static Number getValue(MetricName metric, Aggregation agg) {
        var value = lookupForCount(agg)
            .or(() -> lookupForValue(agg))
            .or(() -> lookupForValues(agg))
            .map(Number::doubleValue);
        return switch (metric) {
            case Metric _ -> value.orElse(0.0);
            case AdditionalMetric _ -> value.filter(number -> !Double.isNaN(number)).orElse(null);
        };
    }

    private static Number getValue(MetricName metric, JsonNode bucketNode) {
        var value = bucketNode.get(ES_VALUE_PROP);
        if (value != null && value.isNumber()) {
            return metric instanceof AdditionalMetric && Double.isNaN(value.asDouble()) ? null : value.asDouble();
        }
        var values = bucketNode.get(ES_VALUES_PROP);
        if (values != null && values.isArray()) {
            return values.get(0).asDouble();
        }
        return metric instanceof AdditionalMetric ? null : 0;
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
            .map(Double::doubleValue);
    }
}
