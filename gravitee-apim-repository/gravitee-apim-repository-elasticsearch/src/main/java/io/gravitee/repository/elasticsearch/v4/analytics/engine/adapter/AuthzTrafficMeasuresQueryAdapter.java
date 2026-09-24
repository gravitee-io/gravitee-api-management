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
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuthzTrafficMeasuresQueryAdapter implements AuthzMeasuresAdapter {

    private final AuthzTrafficFieldResolver fieldResolver = new AuthzTrafficFieldResolver();
    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final SimpleCountBuilder countBuilder = new SimpleCountBuilder();

    public String adapt(MeasuresQuery query) {
        return json(query).toString();
    }

    private JsonObject json(MeasuresQuery query) {
        return new JsonObject().put("size", 0).put("query", adaptQuery(query)).put("aggs", adaptMetrics(query.metrics(), query.filters()));
    }

    @Override
    public FieldResolver fieldResolver() {
        return fieldResolver;
    }

    @Override
    public JsonObject adaptQuery(Query query) {
        return boolAdapter.adaptForAuthzTraffic(query);
    }

    @Override
    public JsonObject scopeFilter(Metric metric, List<Filter> filters) {
        if (fieldResolver.countsNothing(metric, filters)) {
            return new JsonObject().put("match_none", new JsonObject());
        }
        return fieldResolver.scopeTerm(metric).map(AuthzTrafficMeasuresQueryAdapter::term).orElse(null);
    }

    @Override
    public JsonObject adaptMetrics(List<MetricMeasuresQuery> metrics, List<Filter> filters) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            var measureAggs = buildMeasureAggs(metric);
            var scope = scopeFilter(metric.metric(), filters);
            if (scope == null) {
                aggs.mergeIn(measureAggs);
            } else {
                var name = AuthzMeasuresQueryAdapter.scopeAggName(metric.metric());
                aggs.put(name, new JsonObject().put("filter", scope).put("aggs", measureAggs));
            }
        }
        return aggs;
    }

    private static JsonObject term(AuthzTrafficFieldResolver.ScopeTerm scopeTerm) {
        return new JsonObject().put("term", new JsonObject().put(scopeTerm.field(), scopeTerm.value()));
    }

    @Override
    public JsonObject buildMeasureAggs(MetricMeasuresQuery metric) {
        if (metric.filters() != null && !metric.filters().isEmpty()) {
            throw new UnsupportedOperationException(
                "Authz traffic does not support per-metric filters yet, got " + metric.filters() + " on " + metric.metric()
            );
        }
        var aggs = new JsonObject();
        var field = fieldResolver.fromMetric(metric.metric());
        for (var measure : metric.measures()) {
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            aggregate(aggName, field, measure).ifPresent(agg -> {
                var aggregationName = agg.keySet().iterator().next();
                var aggregationValue = agg.values().iterator().next();
                aggs.put(aggregationName, aggregationValue);
            });
        }
        return aggs;
    }

    private Optional<Map<String, JsonObject>> aggregate(String aggName, String field, Measure measure) {
        return switch (measure) {
            case COUNT -> Optional.of(countBuilder.build(aggName, field));
            default -> throw new UnsupportedOperationException("AuthzTrafficMeasuresQueryAdapter does not support measure " + measure);
        };
    }
}
