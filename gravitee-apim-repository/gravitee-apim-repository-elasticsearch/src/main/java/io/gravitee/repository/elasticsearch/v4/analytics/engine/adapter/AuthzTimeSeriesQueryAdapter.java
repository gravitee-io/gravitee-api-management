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

import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.vertx.core.json.JsonObject;

public class AuthzTimeSeriesQueryAdapter {

    private final AuthzMeasuresQueryAdapter measuresAdapter = new AuthzMeasuresQueryAdapter();
    private final AuthzFacetsQueryAdapter facetsAdapter = new AuthzFacetsQueryAdapter();

    public String adapt(TimeSeriesQuery query) {
        AuthzFacetsQueryAdapter.rejectRanges(query.ranges());
        return new JsonObject().put("size", 0).put("query", measuresAdapter.adaptQuery(query)).put("aggs", adaptMetrics(query)).toString();
    }

    private JsonObject adaptMetrics(TimeSeriesQuery query) {
        var aggs = new JsonObject();
        for (var metric : query.metrics()) {
            var inner = facetsAdapter.adaptFacets(metric, query.facets(), query.limit());
            var histogram = DateHistogramAdapter.adapt(query.interval(), query.timeRange()).put("aggs", inner);
            aggs.put(AggregationAdapter.adaptName(metric.metric(), AggregationAdapter.TIME_SERIES_AGG_NAME), histogram);
        }
        return aggs;
    }
}
