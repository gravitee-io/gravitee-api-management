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
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TimeSeriesResponseAdapter extends AbstractResponseAdapter {

    public TimeSeriesResult adapt(SearchResponse esResponse, TimeSeriesQuery query) {
        return lookupForAggregations(esResponse)
            .map(aggregations -> toTimeSeriesResult(aggregations, query))
            .orElseGet(() -> empty(query));
    }

    private TimeSeriesResult toTimeSeriesResult(Map<String, Aggregation> aggregations, TimeSeriesQuery query) {
        return new TimeSeriesResult(AggregationAdapter.toMetricsAndTimeSeries(aggregations, query));
    }

    private TimeSeriesResult empty(TimeSeriesQuery query) {
        return new TimeSeriesResult(emptyMetrics(query));
    }

    private List<MetricTimeSeriesResult> emptyMetrics(TimeSeriesQuery query) {
        return query.metrics().stream().map(this::emptyBuckets).toList();
    }

    private MetricTimeSeriesResult emptyBuckets(MetricMeasuresQuery query) {
        return new MetricTimeSeriesResult(query.metric(), List.of());
    }
}
