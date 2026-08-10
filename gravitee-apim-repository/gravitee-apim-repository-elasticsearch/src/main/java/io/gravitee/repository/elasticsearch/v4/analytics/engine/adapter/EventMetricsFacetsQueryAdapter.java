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
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * Faceted measures over the native Kafka {@code event-metrics} data stream: a {@code terms} bucket
 * on the requested dimension, whose leaf holds the metric measures built by
 * {@link EventMetricsMeasuresQueryAdapter}.
 *
 * <p>Like the native connection adapter, a single facet is supported and a second one is rejected
 * loudly rather than silently ignored.
 *
 * @author GraviteeSource Team
 */
public class EventMetricsFacetsQueryAdapter {

    private final EventMetricsFieldResolver fieldResolver = new EventMetricsFieldResolver();

    private final EventMetricsMeasuresQueryAdapter measuresAdapter = new EventMetricsMeasuresQueryAdapter();

    public String adapt(FacetsQuery query) {
        return json(query).toString();
    }

    private JsonObject json(FacetsQuery query) {
        return new JsonObject()
            .put("size", 0)
            .put("query", measuresAdapter.adaptQuery(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit(), docTypeHoisted(query.metrics())));
    }

    boolean docTypeHoisted(List<MetricMeasuresQuery> metrics) {
        return measuresAdapter.sharedDocType(metrics).isPresent();
    }

    public JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> facets, Integer limit, boolean docTypeHoisted) {
        if (facets != null && facets.size() > 1) {
            throw new UnsupportedOperationException("Native event metrics support a single facet, got: " + facets);
        }
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(adaptFacets(metric, facets, limit, docTypeHoisted));
        }
        return aggs;
    }

    public JsonObject adaptFacets(MetricMeasuresQuery metric, List<Facet> facets, Integer limit, boolean docTypeHoisted) {
        var measures = adaptMeasures(metric, docTypeHoisted);
        if (facets == null || facets.isEmpty()) {
            return measures;
        }
        var facet = facets.getFirst();
        var aggName = AggregationAdapter.adaptName(metric.metric(), facet);
        return new JsonObject().put(aggName, toTermsLeaf(facet, limit).put("aggs", measures));
    }

    JsonObject adaptMeasures(MetricMeasuresQuery metric, boolean docTypeHoisted) {
        return measuresAdapter.adaptMetrics(List.of(metric), docTypeHoisted);
    }

    private JsonObject toTermsLeaf(Facet facet, Integer limit) {
        var terms = new JsonObject().put("field", fieldResolver.fromFacet(facet));
        // A null limit leaves Elasticsearch's default terms size of 10. Acceptable for OPERATION
        // (a closed set of Kafka API keys), but a caller charting every topic or application must
        // pass an explicit limit.
        if (limit != null) {
            terms.put("size", limit);
        }
        return new JsonObject().put("terms", terms);
    }
}
