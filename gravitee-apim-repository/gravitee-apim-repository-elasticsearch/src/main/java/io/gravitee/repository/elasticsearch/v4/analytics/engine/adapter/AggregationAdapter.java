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
import io.gravitee.repository.analytics.engine.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AggregationAdapter {

    public static String adaptName(Metric metric, Measure measure) {
        return metric.name() + "#" + measure.name();
    }

    public static Map<Metric, Map<Measure, Number>> toMetricsAndMeasures(Map<String, Aggregation> aggregations, MeasuresQuery query) {
        var metricMeasuresResults = new HashMap<Metric, Map<Measure, Number>>();
        var aggNames = getAggregationNames(query);

        aggregations.forEach((name, agg) -> {
            if (aggNames.contains(name)) {
                var metric = getMetricFromName(name);
                var measures = metricMeasuresResults.computeIfAbsent(metric, m -> new HashMap<>());
                var measure = getMeasureFromName(name);
                measures.put(measure, getValue(agg));
                aggNames.remove(name);
            }

            if (agg.getBuckets() != null) {
                for (var bucket : agg.getBuckets()) {
                    for (var aggName : aggNames) {
                        if (bucket.hasNonNull(aggName)) {
                            var measure = getMeasureFromName(aggName);
                            var metric = getMetricFromName(aggName);
                            var measures = metricMeasuresResults.computeIfAbsent(metric, m -> new HashMap<>());
                            measures.put(measure, getValue(bucket.get(aggName)));
                            aggNames.remove(aggName);
                            break;
                        }
                    }
                }
            }
        });
        return metricMeasuresResults;
    }

    private static Set<String> getAggregationNames(MeasuresQuery query) {
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
