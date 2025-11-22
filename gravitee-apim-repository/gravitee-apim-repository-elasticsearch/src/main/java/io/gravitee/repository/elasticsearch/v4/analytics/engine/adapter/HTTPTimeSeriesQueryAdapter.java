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
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPTimeSeriesQueryAdapter {

    private final FieldResolver fieldResolver = new HTTPFieldResolver();

    private final FilterAdapter filterAdapter = new FilterAdapter(fieldResolver);

    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);

    private final HTTPMeasuresQueryAdapter measuresAdapter = new HTTPMeasuresQueryAdapter();

    private final HTTPFacetsQueryAdapter facetsQueryAdapter = new HTTPFacetsQueryAdapter();

    public String adapt(TimeSeriesQuery query) {
        return json(query).toString();
    }

    private JsonObject json(TimeSeriesQuery query) {
        return new JsonObject().put("size", 0).put("query", boolAdapter.adapt(query)).put("aggs", adaptTimeSeries(query));
    }

    public JsonObject adaptTimeSeries(TimeSeriesQuery query) {
        var aggs = new JsonObject();
        for (var metric : query.metrics()) {
            aggs.mergeIn(adaptTimeSeries(metric, query));
        }
        return aggs;
    }

    public JsonObject adaptTimeSeries(MetricMeasuresQuery metric, TimeSeriesQuery query) {
        var dateHistogram = DateHistogramAdapter.adapt(query.interval(), query.timeRange());
        if (query.facets() != null && !query.facets().isEmpty()) {
            // Only pass the current metric, not all metrics
            dateHistogram.put("aggs", facetsQueryAdapter.adaptFacets(List.of(metric), query.facets(), query.limit(), query.ranges()));
        } else {
            // Only pass the current metric, not all metrics
            dateHistogram.put("aggs", measuresAdapter.adaptMetrics(List.of(metric)));
        }
        var aggName = AggregationAdapter.adaptName(metric.metric(), TIME_SERIES_AGG_NAME);
        return json().put(aggName, dateHistogram);
    }

    private JsonObject json() {
        return new JsonObject();
    }
}
