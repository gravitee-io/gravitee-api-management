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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HumanApprovalMeasuresQueryAdapterTest extends AbstractQueryAdapterTest {

    private final HumanApprovalMeasuresQueryAdapter adapter = new HumanApprovalMeasuresQueryAdapter();

    private JsonArray rootFilters(MeasuresQuery query) {
        return new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
    }

    private static List<String> termsOn(JsonArray filters, String field) {
        return filters
            .stream()
            .map(JsonObject.class::cast)
            .map(entry -> entry.getJsonObject("term"))
            .filter(term -> term != null && term.containsKey(field))
            .map(term -> term.getString(field))
            .toList();
    }

    private MeasuresQuery costQuery(Measure... measures) {
        return new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVAL_COST, Set.of(measures)))
        );
    }

    @Test
    void should_scope_every_query_to_the_human_approval_decision_point() {
        assertThat(termsOn(rootFilters(costQuery(Measure.COUNT)), "decision-point-type")).containsExactly("human-approval");
    }

    @Test
    void should_scope_every_query_to_the_resolved_phase() {
        assertThat(termsOn(rootFilters(costQuery(Measure.COUNT)), "phase")).containsExactly("RESOLVED");
    }

    private JsonObject costCountAgg() {
        return new JsonObject(adapter.adapt(costQuery(Measure.COUNT))).getJsonObject("aggs").getJsonObject("HUMAN_APPROVAL_COST#COUNT");
    }

    @Test
    void should_read_count_on_the_cost_as_an_amount_like_the_other_cost_metrics_do() {
        var sum = costCountAgg().getJsonObject("sum");

        assertThat(sum)
            .as("COUNT on a cost sums money here; LLM and MCP tool cost read the same way, and a widget stacks all three")
            .isNotNull();
        assertThat(sum.getString("field")).isEqualTo("additional-metrics.double_human-approval_cost");
    }

    @Test
    void should_treat_an_unpriced_decision_as_no_charge_rather_than_dropping_it() {
        assertThat(costCountAgg().getJsonObject("sum").getInteger("missing")).isZero();
    }

    @Test
    void should_average_the_cost_over_the_decisions_that_carried_one() {
        var aggs = new JsonObject(adapter.adapt(costQuery(Measure.AVG))).getJsonObject("aggs");

        assertThat(aggs.getJsonObject("HUMAN_APPROVAL_COST#AVG").getJsonObject("avg").getString("field")).isEqualTo(
            "additional-metrics.double_human-approval_cost"
        );
    }

    @Test
    void should_count_approvals_by_the_case_they_opened_not_by_the_document() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVALS, Set.of(Measure.COUNT)))
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");

        var count = aggs.getJsonObject("HUMAN_APPROVALS#COUNT");

        assertThat(count.getJsonObject("value_count")).as("volume is a real count, unlike COUNT on the cost").isNotNull();
        assertThat(count.getJsonObject("value_count").getString("field")).isEqualTo("case-id");
    }

    @Test
    void should_keep_the_aggregations_flat_so_no_filter_envelope_is_needed() {
        var aggs = new JsonObject(adapter.adapt(costQuery(Measure.COUNT, Measure.AVG))).getJsonObject("aggs");

        assertThat(aggs.fieldNames()).containsExactlyInAnyOrder("HUMAN_APPROVAL_COST#COUNT", "HUMAN_APPROVAL_COST#AVG");
    }

    @Test
    void should_translate_the_application_filter_to_the_agent_field() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(new Filter(Filter.Name.APPLICATION, Filter.Operator.IN, List.of("agent-1"))),
            List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVAL_COST, Set.of(Measure.COUNT)))
        );

        assertThat(rootFilters(query).encode()).contains("app-id").contains("agent-1");
    }

    @Test
    void should_drop_a_filter_the_decisions_index_does_not_carry() {
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(new Filter(Filter.Name.HTTP_STATUS, Filter.Operator.EQ, 200)),
            List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVAL_COST, Set.of(Measure.COUNT)))
        );

        assertThat(rootFilters(query).encode()).doesNotContain("HTTP_STATUS").doesNotContain("status");
    }

    @Test
    void should_refuse_a_measure_the_family_cannot_compute() {
        assertThatThrownBy(() -> adapter.adapt(costQuery(Measure.SUM)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("COUNT and AVG");
    }

    @Test
    void should_drop_a_metric_level_filter_the_decisions_index_does_not_carry_rather_than_fail_the_whole_request() {
        // The root query drops an out-of-family filter silently (see should_drop_a_filter_the_decisions_index_does_not_carry);
        // a metric-level filter from a mixed widget must be dropped the same way rather than take the whole request down.
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(
                    Metric.HUMAN_APPROVAL_COST,
                    Set.of(Measure.COUNT),
                    List.of(new Filter(Filter.Name.HTTP_STATUS, Filter.Operator.EQ, 200)),
                    List.of()
                )
            )
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");

        assertThat(aggs.getJsonObject("HUMAN_APPROVAL_COST#COUNT"))
            .as("no filter survives, so the metric stays flat, unwrapped")
            .isNotNull();
    }

    @Test
    void should_scope_a_metric_that_carries_its_own_filters_without_touching_the_others() {
        // A widget mixing families sends these routinely — one tile scoped to an API type, another to
        // none. Refusing them took the whole request down rather than the one tile.
        var query = new MeasuresQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(
                    Metric.HUMAN_APPROVAL_COST,
                    Set.of(Measure.COUNT),
                    List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1"))),
                    List.of()
                ),
                new MetricMeasuresQuery(Metric.HUMAN_APPROVALS, Set.of(Measure.COUNT))
            )
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");

        var wrapped = aggs.getJsonObject("HUMAN_APPROVAL_COST#__FILTER__");
        assertThat(wrapped).as("a metric carrying filters is wrapped in the envelope").isNotNull();
        assertThat(wrapped.getJsonObject("filter").encode()).contains("api-id").contains("api-1");
        assertThat(wrapped.getJsonObject("aggs").getJsonObject("HUMAN_APPROVAL_COST#COUNT").getJsonObject("sum")).isNotNull();
        assertThat(aggs.getJsonObject("HUMAN_APPROVALS#COUNT")).as("a metric without filters stays flat").isNotNull();
    }
}
