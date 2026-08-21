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
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class AuthzFacetsQueryAdapter {

    private final AuthzFieldResolver fieldResolver = new AuthzFieldResolver();
    private final AuthzMeasuresQueryAdapter measuresAdapter = new AuthzMeasuresQueryAdapter();

    public String adapt(FacetsQuery query) {
        rejectRanges(query.ranges());
        return new JsonObject()
            .put("size", 0)
            .put("query", measuresAdapter.adaptQuery(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit()))
            .toString();
    }

    public JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> facets, Integer limit) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(adaptFacets(metric, facets, limit));
        }
        return aggs;
    }

    JsonObject adaptFacets(MetricMeasuresQuery metric, List<Facet> facets, Integer limit) {
        if (facets != null && facets.size() > 1) {
            throw new UnsupportedOperationException("Authz decisions support a single facet, got: " + facets);
        }
        var measures = measuresAdapter.adaptMetrics(List.of(metric));
        if (facets == null || facets.isEmpty()) {
            return measures;
        }
        var facet = facets.getFirst();
        var terms = new JsonObject().put("field", fieldResolver.fromFacet(facet));
        if (limit != null) {
            terms.put("size", limit);
        }
        applySorts(metric, terms);
        var aggName = AggregationAdapter.adaptName(metric.metric(), facet);
        return new JsonObject().put(aggName, new JsonObject().put("terms", terms).put("aggs", measures));
    }

    static void rejectRanges(List<NumberRange> ranges) {
        if (ranges != null && !ranges.isEmpty()) {
            throw new UnsupportedOperationException("Authz decisions do not support range facets, got: " + ranges);
        }
    }

    private void applySorts(MetricMeasuresQuery metric, JsonObject terms) {
        if (metric.sorts() == null || metric.sorts().isEmpty()) {
            return;
        }
        var order = new JsonObject();
        for (var sort : metric.sorts()) {
            order.put(sortPath(metric.metric(), sort.measure()), sort.order().name().toLowerCase());
        }
        terms.put("order", order);
    }

    // A scoped metric nests its measure under __FILTER__, so Elasticsearch needs the full '>' path.
    private String sortPath(Metric metric, Measure measure) {
        var measureAggName = AggregationAdapter.adaptName(metric, measure);
        if (measuresAdapter.isScoped(metric)) {
            var filterAggName = metric.name() + AggregationAdapter.AGG_NAME_SEPARATOR + AggregationAdapter.FILTER_AGG_SUFFIX;
            return filterAggName + ">" + measureAggName;
        }
        return measureAggName;
    }
}
