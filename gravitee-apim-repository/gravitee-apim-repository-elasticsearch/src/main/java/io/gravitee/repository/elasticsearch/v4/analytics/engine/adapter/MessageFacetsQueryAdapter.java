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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Breaks message measures down by a dimension — operation, connector, API, application.
 *
 * <p>Runs on the second phase of the message join: the caller resolves the connection documents
 * first and passes their request ids in, because the message index carries none of the connection
 * dimensions (no plan-id, no app-id, no entrypoint-id) and could not be filtered by them directly.
 */
public class MessageFacetsQueryAdapter {

    private final MessageMeasuresQueryAdapter measuresAdapter = new MessageMeasuresQueryAdapter();
    private final FilterAdapter filterAdapter = new FilterAdapter(new MessageFieldResolver());
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    public String adapt(FacetsQuery query, Set<String> requestIDs) {
        return json(query, requestIDs).toString();
    }

    private JsonObject json(FacetsQuery query, Set<String> requestIDs) {
        var boolQuery = boolAdapter.messageFilter(query);
        MessageRequestIdFilter.restrictTo(boolQuery, requestIDs);

        return new JsonObject()
            .put("size", 0)
            .put("query", JsonObject.of("bool", boolQuery))
            .put("aggs", adaptFacets(query.metrics(), query.facets(), query.limit()));
    }

    JsonObject adaptFacets(List<MetricMeasuresQuery> metrics, List<Facet> facets, Integer limit) {
        var aggs = new JsonObject();
        for (var metric : metrics) {
            aggs.mergeIn(adaptFacets(metric, facets, limit));
        }
        return aggs;
    }

    /**
     * Nests the requested dimensions, outermost first, and hangs the metric's measures off the
     * innermost bucket — the shape {@code AggregationAdapter} unwraps on the way back.
     *
     * <p>Built top-down, one level per recursion, so every level is keyed by its own facet. Building
     * it bottom-up is what let a child be keyed by its parent's name, which the response side —
     * resolving the child strictly by name — cannot survive.
     */
    JsonObject adaptFacets(MetricMeasuresQuery metric, List<Facet> facets, Integer limit) {
        var aggs = new JsonObject();
        if (facets == null || facets.isEmpty()) {
            return aggs;
        }

        var facet = facets.getFirst();
        var isLast = facets.size() == 1;
        var aggName = AggregationAdapter.adaptName(metric.metric(), facet);

        aggs.put(aggName, isLast ? toTermsLeaf(metric, facet, limit) : toTerms(facet));

        if (isLast) {
            aggs.getJsonObject(aggName).put("aggs", measuresAdapter.adaptMeasures(metric));
        } else {
            aggs.getJsonObject(aggName).put("aggs", adaptFacets(metric, facets.subList(1, facets.size()), limit));
        }

        return aggs;
    }

    /**
     * Only the innermost level carries {@code size} and {@code order}. Both rank buckets by a
     * measure, and the measures hang off the leaf: Elasticsearch rejects the whole search when an
     * order path names an aggregation absent from the level that declares it. {@code limit} belongs
     * here for the same reason — applied at every level it would mean "top N at each depth" rather
     * than "top N of the ranked dimension".
     */
    private JsonObject toTermsLeaf(MetricMeasuresQuery metric, Facet facet, Integer limit) {
        var terms = new JsonObject().put("field", measuresAdapter.fieldResolver().fromFacet(facet));
        // A null limit leaves Elasticsearch its default terms size of 10. Fine for the closed
        // dimensions (operation, connector type); a caller ranking a high-cardinality one — connector
        // id, application — must ask for the size it wants.
        if (limit != null) {
            terms.put("size", limit);
        }
        if (metric.sorts() != null && !metric.sorts().isEmpty()) {
            var order = new JsonObject();
            for (var sort : metric.sorts()) {
                order.put(AggregationAdapter.adaptName(metric.metric(), sort.measure()), sort.order().name().toLowerCase());
            }
            terms.put("order", order);
        }
        return new JsonObject().put("terms", terms);
    }

    private JsonObject toTerms(Facet facet) {
        return new JsonObject().put("terms", new JsonObject().put("field", measuresAdapter.fieldResolver().fromFacet(facet)));
    }

    /** Kept out of the class body so the facets and time-series adapters restrict identically. */
    static final class MessageRequestIdFilter {

        private MessageRequestIdFilter() {}

        /**
         * An empty id set means the first phase matched no connection, so the second must match no
         * message. Both callers already return an empty response before reaching here; emitting a
         * {@code terms} on an empty array rather than returning keeps that true of the adapter alone,
         * because skipping the filter would widen the query instead of narrowing it.
         */
        static void restrictTo(JsonObject boolQuery, Set<String> requestIDs) {
            var ids = requestIDs == null ? new ArrayList<String>() : new ArrayList<>(requestIDs);
            var filters = boolQuery.getJsonArray("filter");
            if (filters == null) {
                filters = new JsonArray();
                boolQuery.put("filter", filters);
            }
            filters.add(JsonObject.of("terms", JsonObject.of("request-id", new JsonArray(ids))));
        }
    }
}
