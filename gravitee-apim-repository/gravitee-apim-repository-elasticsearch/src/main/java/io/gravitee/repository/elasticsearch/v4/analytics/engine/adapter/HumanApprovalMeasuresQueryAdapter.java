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
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.CountBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.CountWithSumBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleAVGBuilder;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;

/**
 * Measures over the human approval decisions of the {@code decisions} data stream.
 *
 * <p>The two terms that make a document belong to this family — the decision point and the resolved
 * phase, see {@link HumanApprovalFieldResolver} — are hoisted into the root query instead of wrapping
 * each metric in a {@code filter} sub-aggregation. They hold for every metric of the family, so
 * hoisting is both cheaper (documents that cannot contribute are never scored) and simpler: the
 * aggregation tree stays flat, which is what lets the facets adapter sort on a plain measure name.
 */
public class HumanApprovalMeasuresQueryAdapter {

    private final HumanApprovalFieldResolver fieldResolver = new HumanApprovalFieldResolver();
    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final SimpleCountBuilder countBuilder = new SimpleCountBuilder();
    private final CountWithSumBuilder countWithSumBuilder = new CountWithSumBuilder();
    private final SimpleAVGBuilder avgBuilder = new SimpleAVGBuilder();

    public String adapt(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", adaptQuery(query)).put("aggs", adaptMetrics(query.metrics())).toString();
    }

    JsonObject adaptQuery(Query query) {
        return boolAdapter.adaptForHumanApproval(query);
    }

    JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(scopedMeasures(metric));
        }
        return aggs;
    }

    /**
     * A metric that carries its own filters is wrapped in the {@code #__FILTER__} envelope the response
     * side already knows how to unwrap. The slot is free here: unlike the native event metrics, this
     * family's own scoping terms ride on the root query, so nothing else needs it.
     *
     * <p>A widget mixing metrics from several families sends these routinely — one tile scoped to an API
     * type, another to none — so refusing them takes the whole request down, not just the tile. The root
     * query already drops a filter outside {@link FilterAdapter#HUMAN_APPROVAL_FILTER_NAMES} rather than
     * reject it (see {@link FilterAdapter#adaptForHumanApproval}); a metric-level filter has to be dropped
     * the same way before it reaches {@link HumanApprovalFieldResolver}, which throws on anything else.
     */
    JsonObject scopedMeasures(MetricMeasuresQuery metric) {
        var measures = buildMeasureAggs(metric);
        var filters = allowedFilters(metric);
        if (filters.isEmpty()) {
            return measures;
        }
        return new JsonObject().put(
            filterAggName(metric.metric()),
            new JsonObject().put("filter", filterAdapter.adaptMetricFilters(filters)).put("aggs", measures)
        );
    }

    static boolean carriesFilters(MetricMeasuresQuery metric) {
        return !allowedFilters(metric).isEmpty();
    }

    private static List<Filter> allowedFilters(MetricMeasuresQuery metric) {
        if (metric.filters() == null) {
            return List.of();
        }
        return metric
            .filters()
            .stream()
            .filter(f -> FilterAdapter.HUMAN_APPROVAL_FILTER_NAMES.contains(f.name()))
            .toList();
    }

    static String filterAggName(Metric metric) {
        return metric.name() + AggregationAdapter.AGG_NAME_SEPARATOR + AggregationAdapter.FILTER_AGG_SUFFIX;
    }

    JsonObject buildMeasureAggs(MetricMeasuresQuery metric) {
        var aggs = new JsonObject();
        var field = fieldResolver.fromMetric(metric.metric());
        for (var measure : metric.measures()) {
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            var agg = aggregate(aggName, field, metric.metric(), measure);
            aggs.put(agg.keySet().iterator().next(), agg.values().iterator().next());
        }
        return aggs;
    }

    private Map<String, JsonObject> aggregate(String aggName, String field, Metric metric, Measure measure) {
        return switch (measure) {
            case COUNT -> count(metric).build(aggName, field);
            case AVG -> avgBuilder.build(aggName, field);
            default -> throw new UnsupportedOperationException("Human approval decisions support COUNT and AVG only, got: " + measure);
        };
    }

    /**
     * {@code COUNT} means "how many" on a countable metric and "how much" on a quantity, which is the
     * convention the token and tool-cost metrics already set on the request index: a widget stacking
     * model, tool and human spend asks all three for {@code COUNT} and gets three amounts. Reading a
     * money field with {@code value_count} would answer a different question in the same slot — the
     * number of priced decisions, rendered as dollars — with nothing to signal the swap.
     *
     * <p>{@link CountWithSumBuilder} sums with {@code missing: 0}, so a decision taken before the
     * environment had a rate counts as no charge rather than dropping out; {@code HUMAN_APPROVALS}
     * is what tells an empty period apart from an unpriced one.
     */
    private CountBuilder count(Metric metric) {
        return metric == Metric.HUMAN_APPROVAL_COST ? countWithSumBuilder : countBuilder;
    }
}
