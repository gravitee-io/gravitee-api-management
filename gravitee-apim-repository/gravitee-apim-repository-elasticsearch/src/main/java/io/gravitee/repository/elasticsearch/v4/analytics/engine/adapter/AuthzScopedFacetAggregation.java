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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.List;

public final class AuthzScopedFacetAggregation {

    private AuthzScopedFacetAggregation() {}

    static JsonObject facet(MetricMeasuresQuery metric, Facet facet, String field, Integer limit, JsonObject measures, JsonObject scope) {
        var terms = new JsonObject().put("field", field);
        if (limit != null) {
            terms.put("size", limit);
        }
        if (metric.sorts() != null && !metric.sorts().isEmpty()) {
            terms.put("order", order(metric));
        }
        var facetAggName = AggregationAdapter.adaptName(metric.metric(), facet);
        var facetAgg = new JsonObject().put(facetAggName, new JsonObject().put("terms", terms).put("aggs", measures));
        if (scope == null) {
            return facetAgg;
        }
        var scopedFacetAgg = new JsonObject().put("filter", scope).put("aggs", facetAgg);
        return new JsonObject().put(AuthzMeasuresQueryAdapter.scopeAggName(metric.metric()), scopedFacetAgg);
    }

    private static JsonObject order(MetricMeasuresQuery metric) {
        var order = new JsonObject();
        for (var sort : metric.sorts()) {
            order.put(AggregationAdapter.adaptName(metric.metric(), sort.measure()), sort.order().name().toLowerCase());
        }
        return order;
    }

    public static SearchResponse unwrap(SearchResponse response, FacetsQuery query) {
        if (response == null || response.getAggregations() == null || !hasFacet(query.facets())) {
            return response;
        }
        var aggregations = new HashMap<>(response.getAggregations());
        var facet = query.facets().getFirst();
        for (var metric : metrics(query.metrics())) {
            var scopeAgg = aggregations.get(AuthzMeasuresQueryAdapter.scopeAggName(metric));
            var facetAggName = AggregationAdapter.adaptName(metric, facet);
            if (scopeAgg != null && scopeAgg.getAggregations().get(facetAggName) instanceof Aggregation facetAgg) {
                aggregations.remove(AuthzMeasuresQueryAdapter.scopeAggName(metric));
                aggregations.put(facetAggName, facetAgg);
            }
        }
        response.setAggregations(aggregations);
        return response;
    }

    public static SearchResponse unwrap(SearchResponse response, TimeSeriesQuery query) {
        if (response == null || response.getAggregations() == null || !hasFacet(query.facets())) {
            return response;
        }
        var facet = query.facets().getFirst();
        for (var metric : metrics(query.metrics())) {
            var histogram = response.getAggregations().get(AggregationAdapter.adaptName(metric, AggregationAdapter.TIME_SERIES_AGG_NAME));
            if (histogram != null && histogram.getBuckets() != null) {
                histogram.getBuckets().forEach(timeBucket -> unwrapTimeBucket(timeBucket, metric, facet));
            }
        }
        return response;
    }

    private static void unwrapTimeBucket(JsonNode timeBucket, Metric metric, Facet facet) {
        var scopeAggName = AuthzMeasuresQueryAdapter.scopeAggName(metric);
        var facetAggName = AggregationAdapter.adaptName(metric, facet);
        if (
            timeBucket instanceof ObjectNode bucket &&
            bucket.get(scopeAggName) instanceof ObjectNode scopeAgg &&
            scopeAgg.get(facetAggName) instanceof ObjectNode facetAgg
        ) {
            bucket.remove(scopeAggName);
            bucket.set(facetAggName, facetAgg);
        }
    }

    private static boolean hasFacet(List<Facet> facets) {
        return facets != null && !facets.isEmpty();
    }

    private static List<Metric> metrics(List<MetricMeasuresQuery> metrics) {
        return metrics.stream().map(MetricMeasuresQuery::metric).toList();
    }
}
