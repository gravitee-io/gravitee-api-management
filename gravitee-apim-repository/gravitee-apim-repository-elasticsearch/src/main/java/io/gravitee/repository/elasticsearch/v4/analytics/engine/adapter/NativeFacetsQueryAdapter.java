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
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation.SimpleCountBuilder;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class NativeFacetsQueryAdapter {

    private final FieldResolver fieldResolver = new NativeApiFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final SimpleCountBuilder countBuilder = new SimpleCountBuilder();

    public String adapt(FacetsQuery query) {
        return json(query).toString();
    }

    private JsonObject json(FacetsQuery query) {
        return new JsonObject()
            .put("size", 0)
            .put("query", boolAdapter.adaptForNative(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit()));
    }

    public JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> queryFacets, Integer limit) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(adaptFacets(metric, queryFacets, limit));
        }
        return aggs;
    }

    public JsonObject adaptFacets(MetricMeasuresQuery metric, List<Facet> queryFacets, Integer limit) {
        var aggs = new JsonObject();
        if (queryFacets.isEmpty()) {
            return aggs;
        }
        var facet = queryFacets.getFirst();
        var aggName = AggregationAdapter.adaptName(metric.metric(), facet);
        aggs.put(aggName, toTermsLeaf(facet, limit));
        aggs.getJsonObject(aggName).put("aggs", adaptMeasures(metric));
        return aggs;
    }

    private JsonObject toTermsLeaf(Facet facet, Integer limit) {
        var terms = new JsonObject().put("field", fieldResolver.fromFacet(facet));
        if (limit != null) {
            terms.put("size", limit);
        }
        return new JsonObject().put("terms", terms);
    }

    private JsonObject adaptMeasures(MetricMeasuresQuery metric) {
        var aggs = new JsonObject();
        var field = fieldResolver.fromMetric(metric.metric());
        for (var measure : metric.measures()) {
            if (measure != Measure.COUNT) {
                throw new UnsupportedOperationException("Native metric only supports COUNT measure, got: " + measure);
            }
            var aggName = AggregationAdapter.adaptName(metric.metric(), measure);
            var built = countBuilder.build(aggName, field);
            aggs.put(built.keySet().iterator().next(), built.values().iterator().next());
        }
        return aggs;
    }
}
