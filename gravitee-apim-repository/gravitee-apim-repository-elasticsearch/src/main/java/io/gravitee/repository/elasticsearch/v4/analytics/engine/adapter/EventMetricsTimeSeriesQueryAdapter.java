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

/**
 * Time series over the native Kafka {@code event-metrics} data stream: a {@code date_histogram}
 * whose per-bucket sub-aggregation is either the faceted breakdown or the plain measures.
 *
 * <p>Counters are accumulated per 5s flush, so a bucket SUM is the exact total for that bucket —
 * no interpolation needed. Gauges use MAX, i.e. the peak observed in the bucket.
 *
 * @author GraviteeSource Team
 */
public class EventMetricsTimeSeriesQueryAdapter {

    private final EventMetricsMeasuresQueryAdapter measuresAdapter = new EventMetricsMeasuresQueryAdapter();

    private final EventMetricsFacetsQueryAdapter facetsQueryAdapter = new EventMetricsFacetsQueryAdapter();

    public String adapt(TimeSeriesQuery query) {
        return json(query).toString();
    }

    private JsonObject json(TimeSeriesQuery query) {
        return new JsonObject().put("size", 0).put("query", measuresAdapter.adaptQuery(query)).put("aggs", adaptTimeSeries(query));
    }

    public JsonObject adaptTimeSeries(TimeSeriesQuery query) {
        if (query.facets() != null && query.facets().size() > 1) {
            throw new UnsupportedOperationException("Native event metrics time series support a single facet, got: " + query.facets());
        }
        var docTypeHoisted = facetsQueryAdapter.docTypeHoisted(query.metrics());
        var aggs = new JsonObject();
        for (var metric : query.metrics()) {
            aggs.mergeIn(adaptTimeSeries(metric, query, docTypeHoisted));
        }
        return aggs;
    }

    public JsonObject adaptTimeSeries(MetricMeasuresQuery metric, TimeSeriesQuery query, boolean docTypeHoisted) {
        var dateHistogram = DateHistogramAdapter.adapt(query.interval(), query.timeRange());
        if (query.facets() != null && !query.facets().isEmpty()) {
            dateHistogram.put("aggs", facetsQueryAdapter.adaptFacets(metric, query.facets(), query.limit(), docTypeHoisted));
        } else {
            dateHistogram.put("aggs", measuresAdapter.adaptMetrics(List.of(metric), docTypeHoisted));
        }
        var aggName = AggregationAdapter.adaptName(metric.metric(), TIME_SERIES_AGG_NAME);
        return new JsonObject().put(aggName, dateHistogram);
    }
}
