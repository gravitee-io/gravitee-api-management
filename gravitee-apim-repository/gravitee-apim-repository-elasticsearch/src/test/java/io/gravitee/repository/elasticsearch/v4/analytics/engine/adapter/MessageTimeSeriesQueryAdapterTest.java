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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MessageTimeSeriesQueryAdapterTest extends AbstractQueryAdapterTest {

    private final MessageTimeSeriesQueryAdapter adapter = new MessageTimeSeriesQueryAdapter();

    private TimeSeriesQuery query(List<Facet> facets) {
        return new TimeSeriesQuery(
            buildTimeRange(),
            List.of(),
            60_000L,
            List.of(new MetricMeasuresQuery(Metric.MESSAGES, Set.of(Measure.COUNT))),
            facets,
            null,
            List.of()
        );
    }

    @Test
    void should_bucket_over_time_and_hang_the_measures_off_each_interval() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of()), Set.of("req-1")));

        var series = json.at("/aggs").elements().next();
        assertThat(series.has("date_histogram")).isTrue();
        assertThat(series.at("/aggs").isEmpty()).isFalse();
    }

    @Test
    void should_split_each_interval_by_the_requested_dimension() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of(Facet.MESSAGE_OPERATION_TYPE)), Set.of("req-1")));

        var series = json.at("/aggs").elements().next();
        var split = series.at("/aggs").elements().next();
        assertThat(split.at("/terms/field").asText()).isEqualTo("operation");
        // Measures sit under the split, so each interval yields one value per dimension value.
        assertThat(split.at("/aggs").isEmpty()).isFalse();
    }

    @Test
    void should_restrict_the_query_to_the_resolved_request_ids() throws JsonProcessingException {
        var json = JSON.readTree(adapter.adapt(query(List.of()), Set.of("req-1", "req-2")));

        var filters = json.at("/query/bool/filter").toString();
        assertThat(filters).contains("request-id").contains("req-1").contains("req-2");
    }
}
