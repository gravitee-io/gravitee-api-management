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
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventMetricsMeasuresQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;
    private static final String API_ID = "api-1";

    private final EventMetricsMeasuresQueryAdapter adapter = new EventMetricsMeasuresQueryAdapter();

    private MeasuresQuery query(List<Filter> filters, MetricMeasuresQuery... metrics) {
        return new MeasuresQuery(new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)), filters, List.of(metrics));
    }

    @Test
    void hoists_the_doc_type_into_the_root_query_when_every_metric_shares_it() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(
                query(
                    List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of(API_ID))),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM)),
                    new MetricMeasuresQuery(Metric.NATIVE_BYTES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM))
                )
            )
        );

        var filters = json.at("/query/bool/filter");
        assertThat(filters.at("/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(filters.at("/1/terms/api-id/0").asText()).isEqualTo(API_ID);
        assertThat(filters.at("/2/term/doc-type").asText()).isEqualTo("topic");

        // Hoisted: the measures sit at the root, with no per-metric filter envelope.
        assertThat(json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#SUM/sum/field").asText()).isEqualTo(
            "downstream-publish-messages-count-increment"
        );
        assertThat(json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#__FILTER__").isMissingNode()).isTrue();
    }

    @Test
    void wraps_each_metric_in_its_own_doc_type_filter_when_they_differ() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(
                query(
                    List.of(),
                    // topic document
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM)),
                    // api document
                    new MetricMeasuresQuery(Metric.NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM, Set.of(Measure.MAX))
                )
            )
        );

        // No doc-type in the root query: it would exclude one of the two metrics.
        assertThat(json.at("/query/bool/filter/1").isMissingNode()).isTrue();

        assertThat(json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#__FILTER__/filter/term/doc-type").asText()).isEqualTo("topic");
        assertThat(
            json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#__FILTER__/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#SUM/sum/field").asText()
        ).isEqualTo("downstream-publish-messages-count-increment");

        assertThat(json.at("/aggs/NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM#__FILTER__/filter/term/doc-type").asText()).isEqualTo("api");
        assertThat(
            json
                .at("/aggs/NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM#__FILTER__/aggs/NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM#MAX/max/field")
                .asText()
        ).isEqualTo("downstream-active-connections");
    }

    @Test
    void derives_the_operation_duration_average_from_its_own_sample_counter() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(query(List.of(), new MetricMeasuresQuery(Metric.NATIVE_OPERATION_BROKER_DURATION, Set.of(Measure.AVG))))
        );

        var aggs = json.at("/aggs/_NATIVE_OPERATION_BROKER_DURATION#AVG/aggs");

        // sum(durations) / sum(samples), NOT an avg on the duration field: the stored value is the
        // sum accumulated over each 5s flush.
        assertThat(aggs.at("/_duration_sum/sum/field").asText()).isEqualTo("endpoint-durations-nanos");
        // The broker duration is sampled when the broker answers, so its denominator is the
        // "answered" counter — not the one used by the two gateway phases.
        assertThat(aggs.at("/_sample_count_sum/sum/field").asText()).isEqualTo("endpoint-downstream-count-increment");

        var script = aggs.at("/NATIVE_OPERATION_BROKER_DURATION#AVG/bucket_script");
        assertThat(script.at("/buckets_path/duration").asText()).isEqualTo("_duration_sum");
        assertThat(script.at("/buckets_path/count").asText()).isEqualTo("_sample_count_sum");
        assertThat(script.at("/script/source").asText()).contains("1000000");
    }

    @Test
    void rejects_percentiles_on_an_accumulated_duration() {
        assertThatThrownBy(() ->
            adapter.adapt(query(List.of(), new MetricMeasuresQuery(Metric.NATIVE_OPERATION_BROKER_DURATION, Set.of(Measure.P95))))
        )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("only AVG is supported");
    }

    @Test
    void rejects_a_measure_the_counters_cannot_answer() {
        assertThatThrownBy(() ->
            adapter.adapt(query(List.of(), new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.P95))))
        )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("SUM and MAX only");
    }

    @Test
    void refuses_per_metric_filters_it_cannot_honour() {
        // The validator accepts these upstream and HTTPMeasuresQueryAdapter honours them, but here the
        // `#__FILTER__` slot already carries the doc-type. Dropping them would silently widen the query.
        var metric = new MetricMeasuresQuery(
            Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
            Set.of(Measure.SUM),
            List.of(new Filter(Filter.Name.NATIVE_TOPIC, Filter.Operator.IN, List.of("orders"))),
            List.of()
        );

        assertThatThrownBy(() -> adapter.adapt(query(List.of(), metric)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("per-metric filters");
    }

    @Test
    void drops_filters_the_event_metrics_documents_do_not_carry() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(
                query(
                    // Gamma injects this scoping by default; event-metrics documents have no entrypoint.
                    List.of(new Filter(Filter.Name.ENTRYPOINT, Filter.Operator.IN, List.of("http-proxy"))),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM))
                )
            )
        );

        var filters = json.at("/query/bool/filter");
        // time range + doc-type only: the entrypoint filter is dropped, not rejected, so the widget
        // still returns data instead of erroring out.
        assertThat(filters.size()).isEqualTo(2);
        assertThat(filters.at("/1/term/doc-type").asText()).isEqualTo("topic");
    }
}
