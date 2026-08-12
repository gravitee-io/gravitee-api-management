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
 * <p>A second facet is rejected loudly rather than silently ignored — note that
 * {@link NativeFacetsQueryAdapter} keeps only the first one instead, an inconsistency worth aligning.
 * Metrics spanning several {@code doc-type} are rejected too: the bucket sits above the measures, so
 * the per-metric filter envelope would break both the sort path and the reading of derived durations.
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
        // A bucketed query cannot carry a per-metric doc-type; see requireSingleDocType.
        measuresAdapter.requireSingleDocType(query.metrics(), "facets");
        return new JsonObject()
            .put("size", 0)
            .put("query", measuresAdapter.adaptQuery(query))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit(), true));
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
        return new JsonObject().put(aggName, toTermsLeaf(metric, facet, limit).put("aggs", measures));
    }

    JsonObject adaptMeasures(MetricMeasuresQuery metric, boolean docTypeHoisted) {
        return measuresAdapter.adaptMetrics(List.of(metric), docTypeHoisted);
    }

    private JsonObject toTermsLeaf(MetricMeasuresQuery metric, Facet facet, Integer limit) {
        var terms = new JsonObject().put("field", fieldResolver.fromFacet(facet));
        // A null limit leaves Elasticsearch's default terms size of 10. Acceptable for OPERATION
        // (a closed set of Kafka API keys), but a caller charting every topic or application must
        // pass an explicit limit.
        if (limit != null) {
            terms.put("size", limit);
        }
        applySorts(metric, terms);
        return new JsonObject().put("terms", terms);
    }

    /**
     * Orders the buckets by one of the metric's measures, mirroring {@link HTTPFacetsQueryAdapter}.
     *
     * <p>Without this, Elasticsearch falls back to its default {@code _count desc} — the number of
     * matching <em>documents</em>. On event metrics a document is one 5s flush, so a top-N would rank
     * by how <em>regularly</em> a topic saw traffic rather than by how much it carried: a steady
     * trickle would outrank a large burst.
     */
    private void applySorts(MetricMeasuresQuery metric, JsonObject terms) {
        if (metric.sorts() == null || metric.sorts().isEmpty()) {
            return;
        }
        if (fieldResolver.isAccumulatedDuration(metric.metric())) {
            // The average is a bucket_script, and Elasticsearch cannot order a terms aggregation by a
            // pipeline aggregation. Fail rather than emit a query whose ranking would silently be
            // something else — a widget claiming "slowest operations" must not be ordered by volume.
            throw new UnsupportedOperationException(
                "Cannot sort buckets by the derived average of " +
                    metric.metric() +
                    ": Elasticsearch cannot order terms by a pipeline aggregation"
            );
        }
        var order = new JsonObject();
        for (var sort : metric.sorts()) {
            order.put(AggregationAdapter.adaptName(metric.metric(), sort.measure()), sort.order().name().toLowerCase());
        }
        terms.put("order", order);
    }
}
