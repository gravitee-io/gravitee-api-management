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
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HumanApprovalFacetsQueryAdapterTest extends AbstractQueryAdapterTest {

    private final HumanApprovalFacetsQueryAdapter adapter = new HumanApprovalFacetsQueryAdapter();

    private FacetsQuery byApplication(List<MetricMeasuresQuery> metrics, Integer limit) {
        return new FacetsQuery(buildTimeRange(), List.of(), metrics, List.of(Facet.APPLICATION), limit, List.of());
    }

    private static List<MetricMeasuresQuery> cost(Measure... measures) {
        return List.of(new MetricMeasuresQuery(Metric.HUMAN_APPROVAL_COST, Set.of(measures)));
    }

    @Test
    void should_break_the_cost_down_by_the_agent_that_asked_for_the_approval() {
        var aggs = new JsonObject(adapter.adapt(byApplication(cost(Measure.COUNT), null))).getJsonObject("aggs");
        var bucket = aggs.getJsonObject("HUMAN_APPROVAL_COST#APPLICATION");

        assertThat(bucket.getJsonObject("terms").getString("field")).isEqualTo("app-id");
        assertThat(
            bucket.getJsonObject("aggs").getJsonObject("HUMAN_APPROVAL_COST#COUNT").getJsonObject("sum").getString("field")
        ).isEqualTo("additional-metrics.double_human-approval_cost");
    }

    @Test
    void should_carry_the_family_scope_into_a_faceted_query() {
        var root = new JsonObject(adapter.adapt(byApplication(cost(Measure.COUNT), null)))
            .getJsonObject("query")
            .getJsonObject("bool")
            .getJsonArray("filter")
            .encode();

        assertThat(root).contains("human-approval").contains("RESOLVED");
    }

    @Test
    void should_cap_the_buckets_when_a_limit_is_given() {
        var terms = new JsonObject(adapter.adapt(byApplication(cost(Measure.COUNT), 5)))
            .getJsonObject("aggs")
            .getJsonObject("HUMAN_APPROVAL_COST#APPLICATION")
            .getJsonObject("terms");

        assertThat(terms.getInteger("size")).isEqualTo(5);
    }

    @Test
    void should_sort_buckets_on_the_plain_measure_because_the_tree_is_flat() {
        var metrics = List.of(
            new MetricMeasuresQuery(
                Metric.HUMAN_APPROVAL_COST,
                Set.of(Measure.COUNT),
                List.of(new MetricMeasuresQuery.Sort(Measure.COUNT, MetricMeasuresQuery.Sort.Order.DESC))
            )
        );

        var terms = new JsonObject(adapter.adapt(byApplication(metrics, null)))
            .getJsonObject("aggs")
            .getJsonObject("HUMAN_APPROVAL_COST#APPLICATION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getString("HUMAN_APPROVAL_COST#COUNT")).isEqualTo("desc");
    }

    @Test
    void should_return_bare_measures_when_no_facet_is_asked_for() {
        var query = new FacetsQuery(buildTimeRange(), List.of(), cost(Measure.COUNT), List.of());

        assertThat(new JsonObject(adapter.adapt(query)).getJsonObject("aggs").fieldNames()).containsExactly("HUMAN_APPROVAL_COST#COUNT");
    }

    @Test
    void should_sort_through_the_envelope_when_the_metric_carries_its_own_filters() {
        var metrics = List.of(
            new MetricMeasuresQuery(
                Metric.HUMAN_APPROVAL_COST,
                Set.of(Measure.COUNT),
                List.of(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1"))),
                List.of(new MetricMeasuresQuery.Sort(Measure.COUNT, MetricMeasuresQuery.Sort.Order.DESC))
            )
        );

        var terms = new JsonObject(adapter.adapt(byApplication(metrics, null)))
            .getJsonObject("aggs")
            .getJsonObject("HUMAN_APPROVAL_COST#APPLICATION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getString("HUMAN_APPROVAL_COST#__FILTER__>HUMAN_APPROVAL_COST#COUNT"))
            .as("the measure now sits under the envelope, so the sort needs the full path")
            .isEqualTo("desc");
    }

    @Test
    void should_refuse_more_than_one_facet() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            cost(Measure.COUNT),
            List.of(Facet.APPLICATION, Facet.HUMAN_APPROVAL_TOOL)
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("single facet");
    }

    @Test
    void should_refuse_range_facets() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            cost(Measure.COUNT),
            List.of(Facet.APPLICATION),
            null,
            List.of(new NumberRange(0d, 10d))
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("range facets");
    }
}
