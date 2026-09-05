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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.GroupedMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/** Reads one measures result per group bucket; a group Elasticsearch did not answer gets the zeroed measures. */
public class GroupedMeasuresResponseAdapter extends AbstractResponseAdapter {

    private final MeasuresResponseAdapter measuresResponseAdapter = new MeasuresResponseAdapter();

    public GroupedMeasuresResult adapt(SearchResponse esResponse, GroupedMeasuresQuery query) {
        var buckets = lookupForAggregations(esResponse)
            .map(aggregations -> aggregations.get(HTTPGroupedMeasuresQueryAdapter.GROUPS_AGG_NAME))
            .map(Aggregation::getBuckets)
            .orElse(List.of());
        var answered = new HashMap<String, MeasuresResult>();
        for (var bucket : buckets) {
            answered.put(bucket.get(AggregationAdapter.ES_KEY_PROP).asText(), AggregationAdapter.toBucketMeasures(bucket, query));
        }
        var empty = measuresResponseAdapter.empty(new MeasuresQuery(query.timeRange(), query.filters(), query.metrics()));
        var groups = new LinkedHashMap<String, MeasuresResult>();
        for (var key : query.groups().keySet()) {
            groups.put(key, answered.getOrDefault(key, empty));
        }
        return new GroupedMeasuresResult(groups);
    }
}
