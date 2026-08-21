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

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleAVGBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMaxBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMinBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP50Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP90Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP95Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP99Builder;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthzMeasuresQueryAdapter {

    private final AuthzFieldResolver fieldResolver = new AuthzFieldResolver();
    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final SimpleCountBuilder countBuilder = new SimpleCountBuilder();
    private final SimpleAVGBuilder avgBuilder = new SimpleAVGBuilder();
    private final SimpleMinBuilder minBuilder = new SimpleMinBuilder();
    private final SimpleMaxBuilder maxBuilder = new SimpleMaxBuilder();
    private final SimpleP50Builder p50Builder = new SimpleP50Builder();
    private final SimpleP90Builder p90Builder = new SimpleP90Builder();
    private final SimpleP95Builder p95Builder = new SimpleP95Builder();
    private final SimpleP99Builder p99Builder = new SimpleP99Builder();

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", adaptQuery(query)).put("aggs", adaptMetrics(query.metrics()));
    }

    JsonObject adaptQuery(Query query) {
        var bool = boolAdapter.adaptForAuthz(query);
        bool.getJsonObject("bool").getJsonArray("filter").add(docTypeTerm());
        return bool;
    }

    boolean isScoped(Metric metric) {
        return scopeFilter(metric) != null;
    }

    static JsonObject docTypeTerm() {
        return new JsonObject().put("term", new JsonObject().put(AuthzFieldResolver.DOC_TYPE_FIELD, AuthzFieldResolver.DOC_TYPE_AUTHZ));
    }

    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            var measureAggs = buildMeasureAggs(metric);
            var scope = scopeFilter(metric.metric());
            if (scope == null) {
                aggs.mergeIn(measureAggs);
            } else {
                var name = metric.metric().name() + AggregationAdapter.AGG_NAME_SEPARATOR + AggregationAdapter.FILTER_AGG_SUFFIX;
                aggs.put(name, new JsonObject().put("filter", scope).put("aggs", measureAggs));
            }
        }
        return aggs;
    }

    private JsonObject scopeFilter(Metric metric) {
        if (fieldResolver.isDecisionScoped(metric)) {
            return new JsonObject().put("term", new JsonObject().put("decision", fieldResolver.decisionValue(metric)));
        }
        if (fieldResolver.isFailureScoped(metric)) {
            var successTerm = new JsonObject().put("term", new JsonObject().put("status", fieldResolver.successStatus()));
            return new JsonObject().put("bool", new JsonObject().put("must_not", new JsonArray().add(successTerm)));
        }
        if (metric == Metric.AUTHZ_EVAL_DURATION) {
            var operationTerm = new JsonObject().put("term", new JsonObject().put("operation", "evaluate"));
            var durationExists = new JsonObject().put("exists", new JsonObject().put("field", "duration-nanos"));
            return new JsonObject().put("bool", new JsonObject().put("filter", new JsonArray().add(operationTerm).add(durationExists)));
        }
        return null;
    }

    JsonObject buildMeasureAggs(MetricMeasuresQuery metric) {
        if (metric.filters() != null && !metric.filters().isEmpty()) {
            throw new UnsupportedOperationException(
                "Authz decisions do not support per-metric filters yet, got " + metric.filters() + " on " + metric.metric()
            );
        }
        var aggs = new JsonObject();
        var field = fieldResolver.fromMetric(metric.metric());
        for (var measure : metric.measures()) {
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            aggregate(aggName, field, measure).ifPresent(agg -> {
                var aggBody = agg.values().iterator().next();
                if (metric.metric() == Metric.AUTHZ_EVAL_DURATION && measure != Measure.COUNT) {
                    applyNanosToMillisScript(aggBody, measure);
                }
                aggs.put(agg.keySet().iterator().next(), aggBody);
            });
        }
        return aggs;
    }

    // duration-nanos is stored in nanoseconds; the dashboard renders milliseconds.
    private static void applyNanosToMillisScript(JsonObject aggBody, Measure measure) {
        aggBody.getJsonObject(esAggType(measure)).put("script", new JsonObject().put("source", "_value / 1000000.0"));
    }

    private static String esAggType(Measure measure) {
        return switch (measure) {
            case COUNT -> "value_count";
            case AVG -> "avg";
            case MIN -> "min";
            case MAX -> "max";
            case P50, P90, P95, P99 -> "percentiles";
            case PERCENTAGE, SUM -> throw new UnsupportedOperationException(
                "AuthzMeasuresQueryAdapter does not rescale measure " + measure
            );
        };
    }

    private Optional<Map<String, JsonObject>> aggregate(String aggName, String field, Measure measure) {
        return switch (measure) {
            case COUNT -> Optional.of(countBuilder.build(aggName, field));
            case AVG -> Optional.of(avgBuilder.build(aggName, field));
            case MIN -> Optional.of(minBuilder.build(aggName, field));
            case MAX -> Optional.of(maxBuilder.build(aggName, field));
            case P50 -> Optional.of(p50Builder.build(aggName, field));
            case P90 -> Optional.of(p90Builder.build(aggName, field));
            case P95 -> Optional.of(p95Builder.build(aggName, field));
            case P99 -> Optional.of(p99Builder.build(aggName, field));
            case PERCENTAGE, SUM -> throw new UnsupportedOperationException(
                "AuthzMeasuresQueryAdapter does not support measure " + measure
            );
        };
    }
}
