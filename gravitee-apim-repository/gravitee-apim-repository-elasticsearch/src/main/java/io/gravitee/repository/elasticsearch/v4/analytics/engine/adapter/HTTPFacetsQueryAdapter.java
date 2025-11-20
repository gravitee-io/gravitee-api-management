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
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPFacetsQueryAdapter {

    private final FieldResolver fieldResolver = new HTTPFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final HTTPMeasuresQueryAdapter measuresAdapter = new HTTPMeasuresQueryAdapter();

    public String adapt(FacetsQuery query) {
        return json(query).toString();
    }

    private JsonObject json(FacetsQuery query) {
        return new JsonObject()
            .put("size", 0)
            .put("query", boolAdapter.adapt(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit(), query.ranges()));
    }

    private JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> queryFacets, Integer limit, List<NumberRange> ranges) {
        var aggs = new JsonObject();
        var facets = new ArrayList<>(queryFacets);
        for (var it = facets.iterator(); it.hasNext(); ) {
            var facet = it.next();
            var isLast = facets.size() == 1;
            aggs.put(facet.name(), adaptFacet(metrics, facet, limit, ranges, isLast));
            if (it.hasNext()) {
                facets.remove(facet);
                aggs.getJsonObject(facet.name()).put("aggs", adaptFacets(metrics, facets, limit, ranges));
            } else {
                aggs.getJsonObject(facet.name()).put("aggs", measuresAdapter.adaptMetrics(metrics));
            }
        }
        return aggs;
    }

    private JsonObject adaptFacet(List<MetricMeasuresQuery> metrics, Facet facet, Integer limit, List<NumberRange> ranges, boolean last) {
        if (last) {
            return toLeaf(facet, metrics, limit, ranges);
        }
        return json().put("terms", toTerms(facet));
    }

    private JsonObject toLeaf(Facet facet, List<MetricMeasuresQuery> metrics, Integer limit, List<NumberRange> ranges) {
        if (ranges == null || ranges.isEmpty()) {
            return toTermsLeaf(facet, metrics, limit);
        }
        return toRangeLeaf(facet, metrics, ranges);
    }

    private JsonObject toRangeLeaf(Facet facet, List<MetricMeasuresQuery> metrics, List<NumberRange> ranges) {
        var range = json().put("field", fieldResolver.fromFacet(facet));
        var queryRanges = range.put("ranges", new JsonArray()).getJsonArray("ranges");
        for (var rangeQuery : ranges) {
            queryRanges.add(toQueryRange(rangeQuery));
        }
        return json().put("range", range);
    }

    private JsonObject toQueryRange(NumberRange range) {
        var key = range.from() + "-" + range.to();
        return json().put("key", key).put("from", range.from()).put("to", range.to());
    }

    private JsonObject toTermsLeaf(Facet facet, List<MetricMeasuresQuery> metrics, Integer limit) {
        var terms = json().put("field", fieldResolver.fromFacet(facet));
        if (limit != null) {
            terms.put("size", limit);
        }
        for (var metric : metrics) {
            if (metric.sorts() != null && !metrics.isEmpty()) {
                var order = json();
                terms.put("order", order);
                for (var sort : metric.sorts()) {
                    order.put(AggregationAdapter.adaptName(metric.metric(), sort.measure()), sort.order().name().toLowerCase());
                }
            }
        }
        return json().put("terms", terms);
    }

    private JsonObject toTerms(Facet facet) {
        return json().put("field", fieldResolver.fromFacet(facet));
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
