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

import io.gravitee.repository.analytics.engine.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.HTTPRPSBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.HttpErrorRateBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleAVGBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMaxBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMinBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP50Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP90Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP95Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP99Builder;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPMeasuresQueryAdapter {

    private final SimpleP90Builder p90Builder = new SimpleP90Builder();
    private final SimpleP95Builder p95Builder = new SimpleP95Builder();
    private final SimpleP99Builder p99Builder = new SimpleP99Builder();
    private final SimpleP50Builder p50Builder = new SimpleP50Builder();
    private final SimpleMinBuilder minBuilder = new SimpleMinBuilder();
    private final SimpleMaxBuilder maxBuilder = new SimpleMaxBuilder();
    private final SimpleCountBuilder countBuilder = new SimpleCountBuilder();
    private final SimpleAVGBuilder avgBuilder = new SimpleAVGBuilder();
    private final HttpErrorRateBuilder errorRateBuilder = new HttpErrorRateBuilder();
    private final HTTPRPSBuilder rpsBuilder = new HTTPRPSBuilder();

    private final FieldResolver fieldResolver = new HTTPFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    public Optional<SimpleCountBuilder> count() {
        return Optional.of(countBuilder);
    }

    public Optional<SimpleMaxBuilder> max() {
        return Optional.of(maxBuilder);
    }

    public Optional<SimpleMinBuilder> min() {
        return Optional.of(minBuilder);
    }

    public Optional<SimpleAVGBuilder> avg() {
        return Optional.of(avgBuilder);
    }

    public Optional<SimpleP99Builder> p99() {
        return Optional.of(p99Builder);
    }

    public Optional<SimpleP95Builder> p95() {
        return Optional.of(p95Builder);
    }

    public Optional<SimpleP90Builder> p90() {
        return Optional.of(p90Builder);
    }

    public Optional<SimpleP50Builder> p50() {
        return Optional.of(p50Builder);
    }

    public Optional<HTTPRPSBuilder> rps() {
        return Optional.of(rpsBuilder);
    }

    public Optional<HttpErrorRateBuilder> errorRate() {
        return Optional.of(errorRateBuilder);
    }

    JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", query(query)).put("aggs", aggregations(query));
    }

    JsonObject query(MeasuresQuery query) {
        return JsonObject.of("bool", filter(query));
    }

    JsonObject filter(MeasuresQuery query) {
        return JsonObject.of("filter", filterAdapter.adapt(query));
    }

    JsonObject aggregations(MeasuresQuery query) {
        var aggs = new JsonObject();
        for (var metric : query.metrics()) {
            for (var measure : metric.measures()) {
                var field = fieldResolver.fromMetric(metric.metric());
                var aggName = AggregationAdapter.adaptName(metric.metric(), measure);

                aggregate(aggName, field, metric.metric(), measure).ifPresent(agg -> {
                    aggs.put(agg.keySet().iterator().next(), agg.values().iterator().next());
                });
            }
        }
        return aggs;
    }

    public Optional<Map<String, JsonObject>> aggregate(String aggName, String field, Metric metric, Measure measure) {
        if (metric == Metric.HTTP_ERRORS && measure == Measure.PERCENTAGE) {
            return errorRate().map(errorRate -> errorRate.build(aggName, field));
        }
        return switch (measure) {
            case Measure.AVG -> avg().map(avg -> avg.build(aggName, field));
            case Measure.COUNT -> count().map(count -> count.build(aggName, field));
            case Measure.MAX -> max().map(max -> max.build(aggName, field));
            case Measure.MIN -> min().map(min -> min.build(aggName, field));
            case Measure.P50 -> p50().map(p50 -> p50.build(aggName, field));
            case Measure.P90 -> p90().map(p90 -> p90.build(aggName, field));
            case Measure.P95 -> p95().map(p95 -> p95.build(aggName, field));
            case Measure.P99 -> p99().map(p99 -> p99.build(aggName, field));
            case Measure.RPS -> rps().map(rps -> rps.build(aggName, field));
            case Measure.PERCENTAGE -> Optional.empty();
        };
    }
}
