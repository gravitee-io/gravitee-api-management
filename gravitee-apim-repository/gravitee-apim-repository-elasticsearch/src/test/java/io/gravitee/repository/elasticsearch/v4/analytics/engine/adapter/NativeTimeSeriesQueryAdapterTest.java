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
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.*;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeTimeSeriesQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;
    private static final String API_ID = "api-1";

    private final NativeTimeSeriesQueryAdapter adapter = new NativeTimeSeriesQueryAdapter();

    @Test
    void builds_a_date_histogram_stacked_by_native_connection_status() throws Exception {
        var query = new TimeSeriesQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID))),
            Duration.ofHours(1).toMillis(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT))),
            List.of(Facet.NATIVE_CONNECTION_STATUS)
        );

        var json = JSON.readTree(adapter.adapt(query));

        // Native filter (v4-metrics, not HTTP) applied
        var filters = json.at("/query/bool/filter");
        assertThat(filters.at("/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(filters.at("/0/range/@timestamp/lte").asLong()).isEqualTo(TO);
        assertThat(filters.at("/1/terms/api-id/0").asText()).isEqualTo(API_ID);

        // Time-series wrapper is a date_histogram
        var dateHistogram = json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#TIME_SERIES/date_histogram");
        assertThat(dateHistogram.isMissingNode()).isFalse();

        // Per-bucket terms sub-agg on the native connection status field
        var terms = json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#TIME_SERIES/aggs/NATIVE_CONNECTIONS_SUMMARY#NATIVE_CONNECTION_STATUS/terms");
        assertThat(terms.get("field").asText()).isEqualTo("additional-metrics." + NativeApiMetricKeys.CONNECTION_STATUS);

        // COUNT leaf under the terms
        var countLeaf = json.at(
            "/aggs/NATIVE_CONNECTIONS_SUMMARY#TIME_SERIES/aggs/NATIVE_CONNECTIONS_SUMMARY#NATIVE_CONNECTION_STATUS" +
                "/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT/value_count"
        );
        assertThat(countLeaf.get("field").asText()).isEqualTo("@timestamp");
    }

    @Test
    void falls_back_to_a_plain_count_when_no_facet_is_requested() throws Exception {
        var query = new TimeSeriesQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            Duration.ofHours(1).toMillis(),
            List.of(new MetricMeasuresQuery(Metric.NATIVE_CONNECTIONS_SUMMARY, Set.of(Measure.COUNT))),
            List.of()
        );

        var json = JSON.readTree(adapter.adapt(query));

        assertThat(json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#TIME_SERIES/date_histogram").isMissingNode()).isFalse();
        var count = json.at("/aggs/NATIVE_CONNECTIONS_SUMMARY#TIME_SERIES/aggs/NATIVE_CONNECTIONS_SUMMARY#COUNT/value_count");
        assertThat(count.get("field").asText()).isEqualTo("@timestamp");
    }
}
