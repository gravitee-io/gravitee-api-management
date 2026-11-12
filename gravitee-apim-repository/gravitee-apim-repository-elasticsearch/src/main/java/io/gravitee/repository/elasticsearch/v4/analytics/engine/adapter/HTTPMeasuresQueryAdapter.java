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
import io.gravitee.repository.analytics.engine.api.metric.MetricName;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Query;
import io.gravitee.repository.elasticsearch.utils.ElasticsearchDsl.Tokens;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.HttpErrorRateBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.LLMTotalBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleAVGBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMaxBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleMinBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP50Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP90Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP95Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleP99Builder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleSUMBuilder;
import io.gravitee.repository.elasticsearch.v4.shared.ConnectionPhaseClause;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The measures of the HTTP request documents. A built-in reading a field and an additional metric field are aggregated
 * alike, over the requests carrying the field.
 *
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
    private final SimpleSUMBuilder sumBuilder = new SimpleSUMBuilder();
    private final HttpErrorRateBuilder errorRateBuilder = new HttpErrorRateBuilder();

    private final FieldResolver fieldResolver = new HTTPFieldResolver();

    private final LLMTotalBuilder llmTotalTokenBuilder = new LLMTotalBuilder(
        fieldResolver.fromMetric(Metric.LLM_PROMPT_TOKEN_SENT),
        fieldResolver.fromMetric(Metric.LLM_PROMPT_TOKEN_RECEIVED)
    );
    private final LLMTotalBuilder llmTotalCostBuilder = new LLMTotalBuilder(
        fieldResolver.fromMetric(Metric.LLM_PROMPT_TOKEN_SENT_COST),
        fieldResolver.fromMetric(Metric.LLM_PROMPT_TOKEN_RECEIVED_COST)
    );

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter queryAdapter = new BoolQueryAdapter(filterAdapter);

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", queryAdapter.adaptForHTTP(query)).put("aggs", adaptMetrics(query.metrics()));
    }

    /**
     * A count reads every connection as it opens, leaving the end of a message API connection out. A value — a
     * duration, a size — is only known once the connection has ended, and leaves its opening out. Each kind sits under
     * a filter of its own, which also carries the filters of the metric. The error rate leaves ends out by itself, and
     * keeps its place: its buckets are read beside the measure they compute.
     */
    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            var rateAggs = buildMeasureAggs(
                metric,
                metric
                    .measures()
                    .stream()
                    .filter(measure -> measure == Measure.PERCENTAGE)
                    .toList()
            );
            var countAggs = buildMeasureAggs(metric, metric.measures().stream().filter(HTTPMeasuresQueryAdapter::isCount).toList());
            var valueAggs = buildMeasureAggs(metric, metric.measures().stream().filter(HTTPMeasuresQueryAdapter::isValue).toList());
            if (!rateAggs.isEmpty()) {
                if (metric.filters() != null && !metric.filters().isEmpty()) {
                    var filterName = metric.metric().name() + AggregationAdapter.AGG_NAME_SEPARATOR + AggregationAdapter.FILTER_AGG_SUFFIX;
                    aggs.put(
                        filterName,
                        new JsonObject().put("filter", filterAdapter.adaptMetricFilters(metric.filters())).put("aggs", rateAggs)
                    );
                } else {
                    aggs.mergeIn(rateAggs);
                }
            }
            if (!countAggs.isEmpty()) {
                aggs.put(AggregationAdapter.countedName(metric.metric()), envelope(ConnectionPhaseClause.notEnd(), metric, countAggs));
            }
            if (!valueAggs.isEmpty()) {
                aggs.put(AggregationAdapter.measuredName(metric.metric()), envelope(ConnectionPhaseClause.notOpening(), metric, valueAggs));
            }
        }
        return aggs;
    }

    /** The path a terms aggregation orders its buckets by, through the envelope the measure sits under. */
    static String orderPath(MetricName metric, Measure measure) {
        var path = AggregationAdapter.orderPath(metric, measure);
        if (isValue(measure)) {
            return AggregationAdapter.measuredName(metric) + ">" + path;
        }
        if (isCount(measure)) {
            return AggregationAdapter.countedName(metric) + ">" + path;
        }
        return path;
    }

    private static boolean isCount(Measure measure) {
        return measure != Measure.PERCENTAGE && !isValue(measure);
    }

    private JsonObject envelope(JsonObject phase, MetricMeasuresQuery metric, JsonObject measureAggs) {
        if (metric.filters() != null && !metric.filters().isEmpty()) {
            phase.getJsonObject("bool").put("filter", JsonArray.of(filterAdapter.adaptMetricFilters(metric.filters())));
        }
        return new JsonObject().put("filter", phase).put("aggs", measureAggs);
    }

    static boolean isValue(Measure measure) {
        return switch (measure) {
            case AVG, MIN, MAX, P50, P90, P95, P99, SUM -> true;
            default -> false;
        };
    }

    /**
     * The requests carrying a value of the metric, which a facet ranked by a measure that reads no value on a bucket
     * without one is restricted to; empty for the error rate, a percentage of every request.
     */
    Optional<JsonObject> carrying(MetricName metric) {
        return switch (metric) {
            case Metric.LLM_PROMPT_TOTAL_TOKEN -> Optional.of(llmTotalTokenBuilder.carrying());
            case Metric.LLM_PROMPT_TOKEN_TOTAL_COST -> Optional.of(llmTotalCostBuilder.carrying());
            case Metric.HTTP_ERROR_RATE -> Optional.empty();
            default -> Optional.of(JsonObject.of(Query.EXISTS, JsonObject.of(Tokens.FIELD, fieldResolver.fromMetric(metric))));
        };
    }

    private JsonObject buildMeasureAggs(MetricMeasuresQuery metric, List<Measure> measures) {
        var aggs = new JsonObject();
        for (var measure : measures) {
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            aggregate(aggName, metric.metric(), measure).ifPresent(agg -> {
                aggs.put(agg.keySet().iterator().next(), agg.values().iterator().next());
            });
        }
        return aggs;
    }

    private Optional<Map<String, JsonObject>> aggregate(String aggName, MetricName metric, Measure measure) {
        return switch (metric) {
            case Metric.LLM_PROMPT_TOTAL_TOKEN -> llmTotalTokenBuilder.build(aggName, measure);
            case Metric.LLM_PROMPT_TOKEN_TOTAL_COST -> llmTotalCostBuilder.build(aggName, measure);
            case Metric.HTTP_ERROR_RATE -> aggregateHTTPErrorRate(aggName, measure);
            default -> aggregateByMeasure(aggName, fieldResolver.fromMetric(metric), measure);
        };
    }

    private Optional<Map<String, JsonObject>> aggregateHTTPErrorRate(String aggName, Measure measure) {
        return switch (measure) {
            case PERCENTAGE -> Optional.of(errorRateBuilder.build(aggName, null));
            default -> Optional.empty();
        };
    }

    private Optional<Map<String, JsonObject>> aggregateByMeasure(String aggName, String field, Measure measure) {
        return switch (measure) {
            case AVG -> Optional.of(avgBuilder.build(aggName, field));
            case COUNT -> Optional.of(countBuilder.build(aggName, field));
            case MAX -> Optional.of(maxBuilder.build(aggName, field));
            case MIN -> Optional.of(minBuilder.build(aggName, field));
            case P50 -> Optional.of(p50Builder.build(aggName, field));
            case P90 -> Optional.of(p90Builder.build(aggName, field));
            case P95 -> Optional.of(p95Builder.build(aggName, field));
            case P99 -> Optional.of(p99Builder.build(aggName, field));
            case SUM -> Optional.of(sumBuilder.build(aggName, field));
            case PERCENTAGE -> Optional.empty();
        };
    }
}
