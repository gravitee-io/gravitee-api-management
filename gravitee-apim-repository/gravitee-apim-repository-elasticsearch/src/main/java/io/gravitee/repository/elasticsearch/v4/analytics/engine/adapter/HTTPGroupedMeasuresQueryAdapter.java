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

import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.vertx.core.json.JsonObject;

/**
 * One {@code filters} aggregation with a named bucket per group, each bucket carrying the measure aggregations of
 * every metric. The buckets come back as an array, in the order the groups were declared, each with its key.
 */
public class HTTPGroupedMeasuresQueryAdapter {

    static final String GROUPS_AGG_NAME = "GROUPS";

    private final FilterAdapter filterAdapter = new FilterAdapter(new HTTPFieldResolver());
    private final BoolQueryAdapter boolAdapter = new BoolQueryAdapter(filterAdapter);
    private final HTTPMeasuresQueryAdapter measuresAdapter = new HTTPMeasuresQueryAdapter();

    public String adapt(GroupedMeasuresQuery query) {
        var groupFilters = new JsonObject();
        query.groups().forEach((key, filters) -> groupFilters.put(key, filterAdapter.adaptMetricFilters(filters)));
        var groups = new JsonObject()
            .put("filters", new JsonObject().put("keyed", false).put("filters", groupFilters))
            .put("aggs", measuresAdapter.adaptMetrics(query.metrics(), HTTPMeasuresQueryAdapter.rateWindow(query.timeRange())));
        return new JsonObject()
            .put("size", 0)
            .put("query", boolAdapter.adaptForHTTP(query))
            .put("aggs", new JsonObject().put(GROUPS_AGG_NAME, groups))
            .toString();
    }
}
