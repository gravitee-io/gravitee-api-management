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
import io.gravitee.repository.analytics.engine.api.result.FacetBucketResult;
import io.gravitee.repository.analytics.engine.api.result.MetricFacetsResult;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AggregationAdapter {

    public static String adaptName(Metric metric, Measure measure) {
        return metric.name() + "#" + measure.name();
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

    private static List<FacetBucketResult> toFacetBucketResults(
        Metric metric,
        Map<String, Aggregation> aggregations,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        List<FacetBucketResult> results = new ArrayList<>();
        var facet = facets.removeFirst();
        var agg = aggregations.get(facet.name());
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
        var key = bucket.get("key").asText();
        if (facets.isEmpty()) {
            var aggregations = toAggregations(bucket, aggNames);
            var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
            return FacetBucketResult.ofMeasures(key, metricAndMeasures.get(metric));
        }
        var nextFacets = new ArrayList<>(facets);
        var nextFacet = nextFacets.removeFirst();
        var next = bucket.get(nextFacet.name());
        var buckets = next.get("buckets");
        return FacetBucketResult.ofBuckets(key, toFacetBucketResults(metric, toBucketList(buckets), nextFacets, aggNames, query));
    }

    private static List<FacetBucketResult> toFacetBucketResults(
        Metric metric,
        List<JsonNode> buckets,
        List<Facet> facets,
        List<String> aggNames,
        FacetsQuery query
    ) {
        List<FacetBucketResult> results = new ArrayList<>();
        if (facets.isEmpty()) {
            for (var bucket : buckets) {
                var key = bucket.get("key").asText();
                var aggregations = toAggregations(bucket, aggNames);
                var metricAndMeasures = toMetricsAndMeasures(aggregations, query);
                results.add(FacetBucketResult.ofMeasures(key, metricAndMeasures.get(metric)));
            }
            return results;
        }

        for (var bucket : buckets) {
            var key = bucket.get("key").asText();
            var next = bucket.get(facets.removeFirst().name());
            var nextBuckets = next.get("buckets");
            results.add(FacetBucketResult.ofBuckets(key, toFacetBucketResults(metric, toBucketList(nextBuckets), facets, aggNames, query)));
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
        if (aggNode.hasNonNull("value") && aggNode.get("value").isNumber()) {
            aggregation.setValue(aggNode.get("value").floatValue());
        }
        if (aggNode.hasNonNull("count") && aggNode.get("count").isNumber()) {
            aggregation.setCount(aggNode.get("count").floatValue());
        }
        if (aggNode.hasNonNull("values") && aggNode.get("values").isObject()) {
            var values = new HashMap<String, Float>();
            var jsonValues = aggNode.get("values");
            jsonValues
                .fields()
                .forEachRemaining(entry -> {
                    if (entry.getValue().isNumber()) {
                        values.put(entry.getKey(), entry.getValue().floatValue());
                    }
                });
            aggregation.setValues(values);
        }
        if (aggNode.get("buckets") != null && aggNode.get("buckets").isArray()) {
            aggregation.setBuckets(toBucketList(aggNode.get("buckets")));
        }
        return aggregation;
    }

    private static List<JsonNode> toBucketList(JsonNode buckets) {
        return StreamSupport.stream(buckets.spliterator(), false).toList();
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

    private static Set<String> adaptNames(Query query) {
        var names = new HashSet<String>();
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
            .or(() -> lookupForAggregation(agg))
            .orElse(0);
    }

    private static Number getValue(JsonNode bucketNode) {
        var value = bucketNode.get("value");
        if (value != null && value.isNumber()) {
            return value.asDouble();
        }
        var values = bucketNode.get("values");
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

    private static Optional<Number> lookupForAggregation(Aggregation agg) {
        return ofNullable(agg.getAggregations())
            .map(Map::values)
            .flatMap(values -> values.stream().findFirst())
            .flatMap(AggregationAdapter::lookupForValue);
    }

    private static Metric getMetricFromName(String aggregationName) {
        return Metric.valueOf(aggregationName.split("#")[0]);
    }

    private static Measure getMeasureFromName(String aggregationName) {
        return Measure.valueOf(aggregationName.split("#")[1]);
    }
}
