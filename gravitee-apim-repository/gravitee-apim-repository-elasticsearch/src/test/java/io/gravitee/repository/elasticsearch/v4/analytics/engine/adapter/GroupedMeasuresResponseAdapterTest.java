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

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.GroupedMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GroupedMeasuresResponseAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final GroupedMeasuresResponseAdapter adapter = new GroupedMeasuresResponseAdapter();

    private static GroupedMeasuresQuery aQuery() {
        var groups = new LinkedHashMap<String, List<Filter>>();
        groups.put("busy", List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-a"))));
        groups.put("idle", List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-b"))));
        return new GroupedMeasuresQuery(
            new TimeRange(Instant.now().minusSeconds(3600), Instant.now()),
            List.of(),
            List.of(
                new MetricMeasuresQuery(Metric.HTTP_REQUESTS, Set.of(Measure.COUNT)),
                new MetricMeasuresQuery(Metric.HTTP_GATEWAY_RESPONSE_TIME, Set.of(Measure.P95))
            ),
            groups
        );
    }

    @Test
    void should_read_the_measures_of_each_group_bucket_by_its_key() throws Exception {
        var response = JSON.readValue(
            """
            {"took": 3, "timed_out": false, "aggregations": {"GROUPS": {"buckets": [
              {"key": "busy", "doc_count": 5, "HTTP_REQUESTS#COUNT": {"value": 5}, "HTTP_GATEWAY_RESPONSE_TIME#P95": {"values": {"95.0": 120.0}}},
              {"key": "idle", "doc_count": 0, "HTTP_REQUESTS#COUNT": {"value": 0}, "HTTP_GATEWAY_RESPONSE_TIME#P95": {"values": {"95.0": null}}}
            ]}}}
            """,
            SearchResponse.class
        );

        var result = adapter.adapt(response, aQuery());

        assertThat(result.groups().keySet()).containsExactly("busy", "idle");
        assertThat(measure(result.groups().get("busy").measures(), Metric.HTTP_REQUESTS, Measure.COUNT)).isEqualTo(5.0);
        assertThat(measure(result.groups().get("busy").measures(), Metric.HTTP_GATEWAY_RESPONSE_TIME, Measure.P95)).isEqualTo(120.0);
        assertThat(measure(result.groups().get("idle").measures(), Metric.HTTP_REQUESTS, Measure.COUNT)).isZero();
    }

    @Test
    void should_answer_every_group_with_zeroed_measures_when_elasticsearch_returns_no_aggregation() throws Exception {
        var response = JSON.readValue("{\"took\": 1, \"timed_out\": false}", SearchResponse.class);

        var result = adapter.adapt(response, aQuery());

        assertThat(result.groups().keySet()).containsExactly("busy", "idle");
        assertThat(result.groups().get("busy").measures()).allSatisfy(metric ->
            assertThat(metric.measures().values()).allSatisfy(value -> assertThat(value.doubleValue()).isZero())
        );
    }

    private static double measure(
        List<io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult> measures,
        Metric metric,
        Measure measure
    ) {
        return measures
            .stream()
            .filter(m -> m.metric() == metric)
            .map(m -> m.measures().get(measure))
            .map(Number::doubleValue)
            .findFirst()
            .orElseThrow();
    }
}
