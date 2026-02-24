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
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.CountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.CountWithSumBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.HTTPRPSBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.HttpErrorRateBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.LLMTotalCostBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.LLMTotalTokenBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleAVGBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMaxBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMinBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP50Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP90Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP95Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP99Builder;
import io.vertx.core.json.JsonObject;
import java.util.List;
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
    private final SimpleCountBuilder simpleCountBuilder = new SimpleCountBuilder();
    private final CountBuilder countWithSumBuilder = new CountWithSumBuilder();
    private final SimpleAVGBuilder avgBuilder = new SimpleAVGBuilder();
    private final HttpErrorRateBuilder errorRateBuilder = new HttpErrorRateBuilder();
    private final HTTPRPSBuilder rpsBuilder = new HTTPRPSBuilder();
    private final LLMTotalTokenBuilder llmTotalTokenBuilder = new LLMTotalTokenBuilder();
    private final LLMTotalCostBuilder llmTotalCostBuilder = new LLMTotalCostBuilder();

    private final FieldResolver fieldResolver = new HTTPFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter queryAdapter = new BoolQueryAdapter(filterAdapter);

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", queryAdapter.adaptForHTTP(query)).put("aggs", adaptMetrics(query.metrics()));
    }

    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            for (var measure : metric.measures()) {
                var aggName = AggregationAdapter.adaptName(metric.metric(), measure);

                var field = isComputedMetric(metric.metric()) ? null : fieldResolver.fromMetric(metric.metric());

                aggregate(aggName, field, metric.metric(), measure).ifPresent(agg -> {
                    aggs.put(agg.keySet().iterator().next(), agg.values().iterator().next());
                });
            }
        }
        return aggs;
    }

    private boolean isComputedMetric(Metric metric) {
        return metric == Metric.LLM_PROMPT_TOTAL_TOKEN || metric == Metric.LLM_PROMPT_TOKEN_TOTAL_COST;
    }

    private Optional<Map<String, JsonObject>> aggregate(String aggName, String field, Metric metric, Measure measure) {
        return switch (metric) {
            case LLM_PROMPT_TOTAL_TOKEN -> aggregateLLMTotalToken(aggName, measure);
            case LLM_PROMPT_TOKEN_TOTAL_COST -> aggregateLLMTotalCost(aggName, measure);
            case HTTP_ERRORS -> aggregateHTTPErrors(aggName, field, metric, measure);
            default -> aggregateByMeasure(aggName, field, metric, measure);
        };
    }

    private Optional<Map<String, JsonObject>> aggregateLLMTotalToken(String aggName, Measure measure) {
        return switch (measure) {
            case COUNT -> Optional.of(llmTotalTokenBuilder.buildSum(aggName));
            case AVG -> Optional.of(llmTotalTokenBuilder.buildAvg(aggName));
            default -> Optional.empty();
        };
    }

    private Optional<Map<String, JsonObject>> aggregateLLMTotalCost(String aggName, Measure measure) {
        return switch (measure) {
            case COUNT -> Optional.of(llmTotalCostBuilder.buildSum(aggName));
            case AVG -> Optional.of(llmTotalCostBuilder.buildAvg(aggName));
            default -> Optional.empty();
        };
    }

    private Optional<Map<String, JsonObject>> aggregateHTTPErrors(String aggName, String field, Metric metric, Measure measure) {
        if (measure == Measure.PERCENTAGE) {
            return errorRate().map(errorRate -> errorRate.build(aggName, field));
        }
        return aggregateByMeasure(aggName, field, metric, measure);
    }

    private Optional<Map<String, JsonObject>> aggregateByMeasure(String aggName, String field, Metric metric, Measure measure) {
        return switch (measure) {
            case AVG -> avg().map(avg -> avg.build(aggName, field));
            case COUNT -> count(metric).map(count -> count.build(aggName, field));
            case MAX -> max().map(max -> max.build(aggName, field));
            case MIN -> min().map(min -> min.build(aggName, field));
            case P50 -> p50().map(p50 -> p50.build(aggName, field));
            case P90 -> p90().map(p90 -> p90.build(aggName, field));
            case P95 -> p95().map(p95 -> p95.build(aggName, field));
            case P99 -> p99().map(p99 -> p99.build(aggName, field));
            case RPS -> rps().map(rps -> rps.build(aggName, field));
            case PERCENTAGE -> Optional.empty();
        };
    }

    private Optional<CountBuilder> count(Metric metric) {
        return switch (metric) {
            case
                LLM_PROMPT_TOKEN_SENT,
                LLM_PROMPT_TOKEN_RECEIVED,
                LLM_PROMPT_TOKEN_SENT_COST,
                LLM_PROMPT_TOKEN_RECEIVED_COST -> Optional.of(countWithSumBuilder);
            default -> Optional.of(simpleCountBuilder);
        };
    }

    private Optional<SimpleMaxBuilder> max() {
        return Optional.of(maxBuilder);
    }

    private Optional<SimpleMinBuilder> min() {
        return Optional.of(minBuilder);
    }

    private Optional<SimpleAVGBuilder> avg() {
        return Optional.of(avgBuilder);
    }

    private Optional<SimpleP99Builder> p99() {
        return Optional.of(p99Builder);
    }

    private Optional<SimpleP95Builder> p95() {
        return Optional.of(p95Builder);
    }

    private Optional<SimpleP90Builder> p90() {
        return Optional.of(p90Builder);
    }

    private Optional<SimpleP50Builder> p50() {
        return Optional.of(p50Builder);
    }

    private Optional<HTTPRPSBuilder> rps() {
        return Optional.of(rpsBuilder);
    }

    private Optional<HttpErrorRateBuilder> errorRate() {
        return Optional.of(errorRateBuilder);
    }
}
