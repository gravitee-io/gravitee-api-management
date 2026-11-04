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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeMeasuresQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;
    private static final String API_ID = "api-1";

    private final NativeMeasuresQueryAdapter adapter = new NativeMeasuresQueryAdapter();

    @Test
    void should_count_the_connections_matching_the_native_filters() throws Exception {
        var query = new MeasuresQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID))),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT)))
        );

        var json = JSON.readTree(adapter.adapt(query));

        assertThat(json.get("size").asInt()).isZero();
        var filters = json.at("/query/bool/filter");
        assertThat(filters.at("/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(filters.at("/0/range/@timestamp/lte").asLong()).isEqualTo(TO);
        assertThat(filters.at("/1/terms/api-id/0").asText()).isEqualTo(API_ID);
        assertNativeConnectionScope(filters.get(2));
        assertThat(json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT/value_count/field").asText()).isEqualTo("@timestamp");
    }

    /** A query without a native API filter counts the connections of the native entrypoint, never every request. */
    @Test
    void should_count_the_documents_of_the_native_entrypoint_only_without_an_api_filter() throws Exception {
        var query = new MeasuresQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT)))
        );

        var filters = JSON.readTree(adapter.adapt(query)).at("/query/bool/filter");

        assertThat(filters).hasSize(2);
        assertNativeConnectionScope(filters.get(1));
    }

    static void assertNativeConnectionScope(JsonNode clause) {
        assertThat(clause.at("/bool/should/0/terms/entrypoint-id/0").asText()).isEqualTo("native-kafka");
        assertThat(clause.at("/bool/should/1/terms/entrypoint-id.keyword/0").asText()).isEqualTo("native-kafka");
        assertThat(clause.at("/bool/minimum_should_match").asInt()).isEqualTo(1);
    }

    /** A widget's own conditions scope its metric: the envelope the HTTP measures use, read back by the response side. */
    @Test
    void should_scope_the_count_by_the_per_metric_filters_in_the_filter_envelope() throws Exception {
        var query = new MeasuresQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(
                new MetricMeasuresQuery(
                    Metric.NATIVE_CONNECTIONS_SUMMARY,
                    Set.of(Measure.COUNT),
                    List.of(new Filter(Filter.Name.NATIVE_CONNECTION_STATUS, Filter.Operator.EQ, "CONNECTED")),
                    List.of()
                )
            )
        );

        var json = JSON.readTree(adapter.adapt(query));

        var envelope = json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#__FILTER__");
        assertThat(
            envelope.at("/filter/bool/must/0/term").get("additional-metrics." + NativeApiMetricKeys.CONNECTION_STATUS).asText()
        ).isEqualTo("CONNECTED");
        assertThat(envelope.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT/value_count/field").asText()).isEqualTo("@timestamp");
        assertThat(json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT").isMissingNode()).isTrue();
    }

    @Test
    void should_refuse_a_measure_other_than_count_as_the_facets_do() {
        var query = new MeasuresQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.AVG)))
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("COUNT");
    }
}
