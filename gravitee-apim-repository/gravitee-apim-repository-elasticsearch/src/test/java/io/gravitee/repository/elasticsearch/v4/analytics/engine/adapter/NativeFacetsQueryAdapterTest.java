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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.*;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeFacetsQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;
    private static final String API_ID = "api-1";

    private final NativeFacetsQueryAdapter adapter = new NativeFacetsQueryAdapter();

    @Test
    void builds_terms_aggregation_on_native_connection_status_with_count_sub_agg() throws Exception {
        var query = new FacetsQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID))),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT))),
            List.of(Facet.NATIVE_CONNECTION_STATUS),
            4
        );

        var json = JSON.readTree(adapter.adapt(query));

        var filters = json.at("/query/bool/filter");
        assertThat(filters.at("/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(filters.at("/0/range/@timestamp/lte").asLong()).isEqualTo(TO);
        assertThat(filters.at("/1/terms/api-id/0").asText()).isEqualTo(API_ID);

        var terms = json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#NATIVE_CONNECTION_STATUS/terms");
        assertThat(terms.get("field").asText()).isEqualTo("additional-metrics." + NativeApiMetricKeys.CONNECTION_STATUS);
        assertThat(terms.get("size").asInt()).isEqualTo(4);

        var countSubAgg = json.at(
            "/aggs/NATIVE_CONNECTIONS_SUMMARY#NATIVE_CONNECTION_STATUS/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT/value_count"
        );
        assertThat(countSubAgg.get("field").asText()).isEqualTo("@timestamp");
    }

    @Test
    void omits_size_when_limit_is_null() throws Exception {
        var query = new FacetsQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT))),
            List.of(Facet.NATIVE_CONNECTION_STATUS)
        );

        var json = JSON.readTree(adapter.adapt(query));

        assertThat(json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#NATIVE_CONNECTION_STATUS/terms/size").isMissingNode()).isTrue();
    }

    @Test
    void throws_UnsupportedOperationException_for_non_count_measure() {
        var query = new FacetsQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.AVG))),
            List.of(Facet.NATIVE_CONNECTION_STATUS)
        );

        assertThatThrownBy(() -> adapter.adapt(query)).isInstanceOf(UnsupportedOperationException.class);
    }
}
