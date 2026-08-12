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
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeRange;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EventMetricsTimeSeriesQueryAdapterTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;
    private static final long INTERVAL = Duration.ofHours(1).toMillis();

    private final EventMetricsTimeSeriesQueryAdapter adapter = new EventMetricsTimeSeriesQueryAdapter();

    private TimeSeriesQuery query(List<Filter> filters, List<Facet> facets, MetricMeasuresQuery... metrics) {
        return new TimeSeriesQuery(
            new TimeRange(Instant.ofEpochMilli(FROM), Instant.ofEpochMilli(TO)),
            filters,
            INTERVAL,
            List.of(metrics),
            facets
        );
    }

    @Test
    void buckets_each_metric_under_its_own_date_histogram() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(
                query(
                    List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1"))),
                    List.of(),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM)),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_UPSTREAM, Set.of(Measure.SUM))
                )
            )
        );

        var filters = json.at("/query/bool/filter");
        assertThat(filters.at("/0/range/@timestamp/gte").asLong()).isEqualTo(FROM);
        assertThat(filters.at("/1/terms/api-id/0").asText()).isEqualTo("api-1");
        // Both metrics live on `topic` documents, so the doc-type is hoisted into the root query and
        // the aggregations stay flat.
        assertThat(filters.at("/2/term/doc-type").asText()).isEqualTo("topic");

        for (var metric : List.of("NATIVE_MESSAGES_PRODUCED_DOWNSTREAM", "NATIVE_MESSAGES_PRODUCED_UPSTREAM")) {
            var bucket = json.at("/aggs/" + metric + "#TIME_SERIES");
            assertThat(bucket.at("/date_histogram/fixed_interval").asText()).isEqualTo(INTERVAL + "ms");
            assertThat(bucket.at("/aggs/" + metric + "#SUM/sum/field").isMissingNode())
                .as("%s must carry its measure inside the bucket", metric)
                .isFalse();
        }
    }

    @Test
    void nests_the_facet_inside_the_bucket_when_one_is_requested() throws Exception {
        var metric = new MetricMeasuresQuery(
            Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM,
            Set.of(Measure.SUM),
            List.of(new MetricMeasuresQuery.Sort(Measure.SUM, MetricMeasuresQuery.Sort.Order.DESC))
        );

        var json = JSON.readTree(adapter.adapt(query(List.of(), List.of(Facet.NATIVE_TOPIC), metric)));
        var terms = json.at(
            "/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#TIME_SERIES/aggs/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#NATIVE_TOPIC/terms"
        );

        assertThat(terms.at("/field").asText()).isEqualTo("topic");
        // The ordering must survive the nesting: without it Elasticsearch ranks the per-bucket topics
        // by document count, which on event metrics counts 5s flushes rather than messages.
        assertThat(terms.at("/order/NATIVE_MESSAGES_PRODUCED_DOWNSTREAM#SUM").asText()).isEqualTo("desc");
    }

    @Test
    void wraps_a_derived_duration_so_it_can_be_read_back_per_bucket() throws Exception {
        var json = JSON.readTree(
            adapter.adapt(
                query(List.of(), List.of(), new MetricMeasuresQuery(Metric.NATIVE_OPERATION_BROKER_DURATION, Set.of(Measure.AVG)))
            )
        );

        var aggs = json.at("/aggs/NATIVE_OPERATION_BROKER_DURATION#TIME_SERIES/aggs/_NATIVE_OPERATION_BROKER_DURATION#AVG/aggs");

        assertThat(aggs.at("/_duration_sum/sum/field").asText()).isEqualTo("endpoint-durations-nanos");
        assertThat(aggs.at("/_sample_count_sum/sum/field").asText()).isEqualTo("endpoint-downstream-count-increment");
        assertThat(aggs.at("/NATIVE_OPERATION_BROKER_DURATION#AVG/bucket_script").isMissingNode()).isFalse();
    }

    @Test
    void refuses_a_second_facet_rather_than_charting_only_the_first() {
        assertThatThrownBy(() ->
            adapter.adapt(
                query(
                    List.of(),
                    List.of(Facet.NATIVE_TOPIC, Facet.API),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM))
                )
            )
        )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("single facet");
    }

    @Test
    void refuses_metrics_that_span_several_document_types() {
        // The date_histogram sits above the measures, so a per-metric doc-type envelope would nest one
        // level deeper than the response side reads: a derived duration would come back as 0, silently.
        assertThatThrownBy(() ->
            adapter.adapt(
                query(
                    List.of(),
                    List.of(),
                    new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM)),
                    new MetricMeasuresQuery(Metric.NATIVE_OPERATIONS_RECEIVED, Set.of(Measure.SUM))
                )
            )
        )
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("cannot mix documents types")
            .hasMessageContaining("topic")
            .hasMessageContaining("operation");
    }

    @Test
    void drops_the_entrypoint_scoping_gamma_injects_by_default() {
        // Event-metrics documents have no entrypoint. The allow-list drops the filter instead of
        // failing, so the widget still returns data — without this the whole family would chart zeros.
        var json = adapter.adapt(
            query(
                List.of(new Filter(Filter.Name.ENTRYPOINT, Filter.Operator.IN, List.of("http-proxy"))),
                List.of(),
                new MetricMeasuresQuery(Metric.NATIVE_MESSAGES_PRODUCED_DOWNSTREAM, Set.of(Measure.SUM))
            )
        );

        assertThat(json).doesNotContain("entrypoint");
    }
}
