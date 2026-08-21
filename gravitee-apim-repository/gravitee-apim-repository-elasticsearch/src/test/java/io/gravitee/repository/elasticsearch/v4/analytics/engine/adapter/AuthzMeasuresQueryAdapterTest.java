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

import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzMeasuresQueryAdapterTest extends AbstractQueryAdapterTest {

    private final AuthzMeasuresQueryAdapter adapter = new AuthzMeasuresQueryAdapter();

    @Test
    void should_pin_every_query_to_the_authz_doc_type() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var json = new JsonObject(adapter.adapt(query));

        var filters = json.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(
            filters
                .stream()
                .map(JsonObject.class::cast)
                .map(f -> f.getJsonObject("term"))
                .filter(Objects::nonNull)
                .toList()
        ).anySatisfy(term -> assertThat(term.getString("doc-type")).isEqualTo("authz"));
    }

    @Test
    void should_count_decisions_with_a_value_count_on_the_decision_field() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");

        assertThat(aggs.getJsonObject("AUTHZ_DECISIONS#COUNT").getJsonObject("value_count").getString("field")).isEqualTo("decision");
    }

    @Test
    void should_wrap_a_decision_scoped_metric_in_a_filter_aggregation() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FORBIDS, Set.of(Measure.COUNT)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");
        var wrapper = aggs.getJsonObject("AUTHZ_FORBIDS#__FILTER__");

        assertThat(wrapper.getJsonObject("filter").getJsonObject("term").getString("decision")).isEqualTo("FORBID");
        assertThat(
            wrapper.getJsonObject("aggs").getJsonObject("AUTHZ_FORBIDS#COUNT").getJsonObject("value_count").getString("field")
        ).isEqualTo("decision");
    }

    @Test
    void should_wrap_the_failure_metric_in_a_must_not_success_filter() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FAILURES, Set.of(Measure.COUNT)))
        );

        var wrapper = new JsonObject(adapter.adapt(query)).getJsonObject("aggs").getJsonObject("AUTHZ_FAILURES#__FILTER__");
        var mustNot = wrapper.getJsonObject("filter").getJsonObject("bool").getJsonArray("must_not");

        assertThat(mustNot.getJsonObject(0).getJsonObject("term").getString("status")).isEqualTo("success");
    }

    @Test
    void should_support_percentiles_on_the_duration_metric() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.P50, Measure.P95, Measure.AVG)))
        );

        var wrapped = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs");

        assertThat(wrapped.fieldNames()).contains("AUTHZ_EVAL_DURATION#P50", "AUTHZ_EVAL_DURATION#P95", "AUTHZ_EVAL_DURATION#AVG");
        assertThat(wrapped.getJsonObject("AUTHZ_EVAL_DURATION#AVG").getJsonObject("avg").getString("field")).isEqualTo("duration-nanos");
    }

    @Test
    void should_scale_the_duration_metric_from_nanoseconds_to_milliseconds() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.AVG, Measure.P50)))
        );

        var wrapped = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs");

        assertThat(
            wrapped.getJsonObject("AUTHZ_EVAL_DURATION#AVG").getJsonObject("avg").getJsonObject("script").getString("source")
        ).isEqualTo("_value / 1000000.0");
        assertThat(
            wrapped.getJsonObject("AUTHZ_EVAL_DURATION#P50").getJsonObject("percentiles").getJsonObject("script").getString("source")
        ).isEqualTo("_value / 1000000.0");
    }

    @Test
    void should_not_scale_counter_metrics() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");

        assertThat(aggs.getJsonObject("AUTHZ_DECISIONS#COUNT").getJsonObject("value_count").containsKey("script")).isFalse();
    }

    @Test
    void should_scope_the_duration_metric_to_evaluations_with_a_recorded_duration() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.AVG)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");
        var wrapper = aggs.getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__");

        assertThat(wrapper).isNotNull();
        var filterClauses = wrapper.getJsonObject("filter").getJsonObject("bool").getJsonArray("filter");
        assertThat(
            filterClauses
                .stream()
                .map(JsonObject.class::cast)
                .map(f -> f.getJsonObject("term"))
                .filter(Objects::nonNull)
                .toList()
        ).anySatisfy(term -> assertThat(term.getString("operation")).isEqualTo("evaluate"));
        assertThat(
            filterClauses
                .stream()
                .map(JsonObject.class::cast)
                .map(f -> f.getJsonObject("exists"))
                .filter(Objects::nonNull)
                .toList()
        ).anySatisfy(exists -> assertThat(exists.getString("field")).isEqualTo("duration-nanos"));
        assertThat(
            wrapper.getJsonObject("aggs").getJsonObject("AUTHZ_EVAL_DURATION#AVG").getJsonObject("avg").getString("field")
        ).isEqualTo("duration-nanos");
    }

    @Test
    void should_not_scope_a_counter_metric_with_an_exists_clause() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var json = new JsonObject(adapter.adapt(query));

        assertThat(json.getJsonObject("aggs").containsKey("AUTHZ_DECISIONS#__FILTER__")).isFalse();
    }

    @Test
    void should_reject_per_metric_filters() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(
                    Metric.AUTHZ_DECISIONS,
                    Set.of(Measure.COUNT),
                    List.of(new Filter(Filter.Name.AUTHZ_CALLER, Filter.Operator.IN, List.of("pep"))),
                    List.of()
                )
            )
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("per-metric filters");
    }

    @Test
    void should_adapt_an_authz_filter_to_its_field() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(new Filter(Filter.Name.AUTHZ_CALLER, Filter.Operator.IN, List.of("pep"))),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT)))
        );

        var filters = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(filters.encode()).contains("caller").contains("pep");
    }

    @Test
    void should_not_scale_a_count_taken_on_the_duration_metric() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.COUNT)))
        );

        var wrapped = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs");

        assertThat(wrapped.getJsonObject("AUTHZ_EVAL_DURATION#COUNT").getJsonObject("value_count").containsKey("script")).isFalse();
    }

    @Test
    void should_support_p90_on_the_duration_metric() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.P90)))
        );

        var wrapped = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs");

        assertThat(
            wrapped.getJsonObject("AUTHZ_EVAL_DURATION#P90").getJsonObject("percentiles").getJsonObject("script").getString("source")
        ).isEqualTo("_value / 1000000.0");
    }

    @Test
    void should_reject_a_measure_the_adapter_cannot_express() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.SUM)))
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("does not support measure");
    }
}
