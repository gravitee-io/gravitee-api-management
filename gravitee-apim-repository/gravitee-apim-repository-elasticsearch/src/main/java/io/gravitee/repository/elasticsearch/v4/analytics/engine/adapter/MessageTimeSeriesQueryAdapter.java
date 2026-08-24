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

import static io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.AggregationAdapter.TIME_SERIES_AGG_NAME;

import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;

/**
 * Message measures over time, optionally split by a dimension.
 *
 * <p>Like the facets adapter, this runs on the second phase of the message join and receives the
 * request ids resolved from the connection documents.
 */
public class MessageTimeSeriesQueryAdapter {

    private final MessageFacetsQueryAdapter facetsAdapter = new MessageFacetsQueryAdapter();
    private final MessageMeasuresQueryAdapter measuresAdapter = new MessageMeasuresQueryAdapter();
    private final FilterAdapter filterAdapter = new FilterAdapter(new MessageFieldResolver());
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    public String adapt(TimeSeriesQuery query, Set<String> requestIDs) {
        return json(query, requestIDs).toString();
    }

    private JsonObject json(TimeSeriesQuery query, Set<String> requestIDs) {
        var boolQuery = boolAdapter.messageFilter(query);
        MessageFacetsQueryAdapter.MessageRequestIdFilter.restrictTo(boolQuery, requestIDs);

        return new JsonObject().put("size", 0).put("query", JsonObject.of("bool", boolQuery)).put("aggs", adaptTimeSeries(query));
    }

    JsonObject adaptTimeSeries(TimeSeriesQuery query) {
        var aggs = new JsonObject();
        for (var metric : query.metrics()) {
            aggs.mergeIn(adaptTimeSeries(metric, query));
        }
        return aggs;
    }

    JsonObject adaptTimeSeries(MetricMeasuresQuery metric, TimeSeriesQuery query) {
        var dateHistogram = DateHistogramAdapter.adapt(query.interval(), query.timeRange());

        if (query.facets() != null && !query.facets().isEmpty()) {
            dateHistogram.put("aggs", facetsAdapter.adaptFacets(List.of(metric), query.facets(), query.limit()));
        } else {
            dateHistogram.put("aggs", measuresAdapter.adaptMeasures(metric));
        }

        return new JsonObject().put(AggregationAdapter.adaptName(metric.metric(), TIME_SERIES_AGG_NAME), dateHistogram);
    }
}
