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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AbstractQueryAdapterTest {

    protected static final String API_ID = "273f4728-1e30-4c78-bf47-281e304c78a5";
    protected static final Long FROM = 1756104349879L;
    protected static final Long TO = 1756190749879L;

    protected static final ObjectMapper JSON = new ObjectMapper();

    protected TimeRange buildTimeRange() {
        return new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO));
    }

    protected List<Filter> buildFilters() {
        return List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID)));
    }

    protected List<MetricMeasuresQuery> buildMetrics() {
        var measures = Set.of(Measure.P90, Measure.P50, Measure.P95, Measure.P99, Measure.MIN, Measure.MAX, Measure.AVG);
        return List.of(
            new MetricMeasuresQuery(Metric.HTTP_GATEWAY_LATENCY, measures),
            new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, measures),
            new MetricMeasuresQuery(Metric.HTTP_ENDPOINT_RESPONSE_TIME, measures),
            new MetricMeasuresQuery(Metric.HTTP_REQUEST_CONTENT_LENGTH, measures),
            new MetricMeasuresQuery(Metric.HTTP_RESPONSE_CONTENT_LENGTH, measures)
        );
    }

    protected List<MetricMeasuresQuery> buildSortedMetric() {
        var measures = Set.of(Measure.COUNT, Measure.RPS);
        return List.of(
            new MetricMeasuresQuery(
                Metric.HTTP_REQUESTS,
                measures,
                List.of(new MetricMeasuresQuery.Sort(Measure.COUNT, MetricMeasuresQuery.Sort.Order.DESC))
            )
        );
    }
}
