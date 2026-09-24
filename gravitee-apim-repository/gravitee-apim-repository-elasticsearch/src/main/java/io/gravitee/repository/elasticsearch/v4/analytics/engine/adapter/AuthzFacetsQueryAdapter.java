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

import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class AuthzFacetsQueryAdapter {

    private final AuthzMeasuresAdapter measuresAdapter;

    public AuthzFacetsQueryAdapter(AuthzMeasuresAdapter measuresAdapter) {
        this.measuresAdapter = measuresAdapter;
    }

    public String adapt(FacetsQuery query) {
        rejectRanges(query.ranges());
        return new JsonObject()
            .put("size", 0)
            .put("query", measuresAdapter.adaptQuery(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit(), query.filters()))
            .toString();
    }

    public JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> facets, Integer limit, List<Filter> filters) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(adaptFacets(metric, facets, limit, filters));
        }
        return aggs;
    }

    JsonObject adaptFacets(MetricMeasuresQuery metric, List<Facet> facets, Integer limit, List<Filter> filters) {
        if (facets != null && facets.size() > 1) {
            throw new UnsupportedOperationException("Authz analytics support a single facet, got: " + facets);
        }
        if (facets == null || facets.isEmpty()) {
            return measuresAdapter.adaptMetrics(List.of(metric), filters);
        }
        var facet = facets.getFirst();
        return AuthzScopedFacetAggregation.facet(
            metric,
            facet,
            measuresAdapter.fieldResolver().fromFacet(facet),
            limit,
            measuresAdapter.buildMeasureAggs(metric),
            measuresAdapter.scopeFilter(metric.metric(), filters)
        );
    }

    static void rejectRanges(List<NumberRange> ranges) {
        if (ranges != null && !ranges.isEmpty()) {
            throw new UnsupportedOperationException("Authz analytics do not support range facets, got: " + ranges);
        }
    }
}
