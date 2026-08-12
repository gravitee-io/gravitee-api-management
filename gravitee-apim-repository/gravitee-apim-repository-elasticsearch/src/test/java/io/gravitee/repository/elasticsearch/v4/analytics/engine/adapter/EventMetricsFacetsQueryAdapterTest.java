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
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventMetricsFacetsQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;

    private final EventMetricsFacetsQueryAdapter adapter = new EventMetricsFacetsQueryAdapter();

    private FacetsQuery query(MetricMeasuresQuery metric, Facet facet, Integer limit) {
        return new FacetsQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(metric),
            List.of(facet),
            limit,
            List.of()
        );
    }

    @Test
    void orders_buckets_by_the_requested_measure() throws Exception {
        var metric = new MetricMeasuresQuery(
            Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
            Set.of(Measure.SUM),
            List.of(new MetricMeasuresQuery.Sort(Measure.SUM, MetricMeasuresQuery.Sort.Order.DESC))
        );

        var json = JSON.readTree(adapter.adapt(query(metric, Facet.NATIVE_TOPIC, 10)));
        var terms = json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#NATIVE_TOPIC/terms");

        assertThat(terms.get("field").asText()).isEqualTo("topic");
        assertThat(terms.get("size").asInt()).isEqualTo(10);
        // Without this, Elasticsearch falls back to `_count desc` — the number of 5s flush documents.
        // A topic with a steady trickle would then outrank a topic that carried a large burst.
        assertThat(terms.at("/order/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#SUM").asText()).isEqualTo("desc");
    }

    @Test
    void leaves_the_default_ordering_when_no_sort_is_requested() throws Exception {
        var metric = new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM));

        var json = JSON.readTree(adapter.adapt(query(metric, Facet.NATIVE_TOPIC, null)));
        var terms = json.at("/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#NATIVE_TOPIC/terms");

        assertThat(terms.get("order")).isNull();
        assertThat(terms.get("size")).isNull();
    }

    @Test
    void refuses_to_order_by_a_derived_duration() {
        // The average is a bucket_script; Elasticsearch cannot order a terms aggregation by a pipeline
        // aggregation. Failing is the only honest option — emitting the query anyway would rank by
        // document count while the widget claims to rank by duration.
        var metric = new MetricMeasuresQuery(
            Metric.NATIVE_OPERATION_BROKER_DURATION,
            Set.of(Measure.AVG),
            List.of(new MetricMeasuresQuery.Sort(Measure.AVG, MetricMeasuresQuery.Sort.Order.DESC))
        );

        assertThatThrownBy(() -> adapter.adapt(query(metric, Facet.NATIVE_OPERATION, 10)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("pipeline aggregation");
    }

    @Test
    void still_builds_an_unsorted_query_for_a_derived_duration() throws Exception {
        var metric = new MetricMeasuresQuery(Metric.NATIVE_OPERATION_BROKER_DURATION, Set.of(Measure.AVG));

        var json = JSON.readTree(adapter.adapt(query(metric, Facet.NATIVE_OPERATION, 10)));
        var bucket = json.at("/aggs/NATIVE_OPERATION_BROKER_DURATION#NATIVE_OPERATION");

        assertThat(bucket.at("/terms/field").asText()).isEqualTo("operation");
        // Assert the leaf too: without it this test would pass even if the measures were dropped.
        assertThat(bucket.at("/aggs/_NATIVE_OPERATION_BROKER_DURATION#AVG/aggs/_duration_sum/sum/field").asText()).isEqualTo(
            "endpoint-durations-nanos"
        );
    }

    @Test
    void refuses_metrics_that_span_several_document_types() {
        // The terms bucket sits above the measures, so a per-metric doc-type envelope would land
        // inside it: the sort path would no longer resolve, and a derived duration would read back as
        // 0 with no error at all. Refusing is the only option that cannot mislead.
        var topicMetric = new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM));
        var apiMetric = new MetricMeasuresQuery(Metric.NATIVE_ACTIVE_CONNECTIONS_DOWNSTREAM, Set.of(Measure.MAX));
        var mixed = new FacetsQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            List.of(),
            List.of(topicMetric, apiMetric),
            List.of(Facet.API),
            10,
            List.of()
        );

        assertThatThrownBy(() -> adapter.adapt(mixed))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("cannot mix documents types")
            .hasMessageContaining("topic")
            .hasMessageContaining("api");
    }
}
