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
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery.Sort;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuthzFacetsQueryAdapterTest extends AbstractQueryAdapterTest {

    private final AuthzFacetsQueryAdapter adapter = new AuthzFacetsQueryAdapter(new AuthzMeasuresQueryAdapter());

    @Test
    void should_build_a_terms_aggregation_on_the_facet_field() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_DECISION),
            5
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");
        var terms = aggs.getJsonObject("AUTHZ_DECISIONS#AUTHZ_DECISION").getJsonObject("terms");

        assertThat(terms.getString("field")).isEqualTo("verdict");
        assertThat(terms.getInteger("size")).isEqualTo(5);
    }

    @Test
    void should_group_decisions_by_the_pdp_that_evaluated_them() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_PDP)
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_DECISIONS#AUTHZ_PDP")
            .getJsonObject("terms");

        assertThat(terms.getString("field")).isEqualTo("decision-point-id");
    }

    @Test
    void should_nest_the_measures_under_the_terms_bucket() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION)
        );

        var bucket = new JsonObject(adapter.adapt(query)).getJsonObject("aggs").getJsonObject("AUTHZ_DECISIONS#AUTHZ_ACTION");

        assertThat(
            bucket.getJsonObject("aggs").getJsonObject("AUTHZ_DECISIONS#COUNT").getJsonObject("value_count").getString("field")
        ).isEqualTo("event-id");
    }

    @Test
    void should_build_the_terms_of_a_scoped_metric_inside_its_scope_filter() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FORBIDS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION)
        );

        var aggs = new JsonObject(adapter.adapt(query)).getJsonObject("aggs");
        var filterAgg = aggs.getJsonObject("AUTHZ_FORBIDS#__FILTER__");
        var facetAgg = filterAgg.getJsonObject("aggs").getJsonObject("AUTHZ_FORBIDS#AUTHZ_ACTION");

        assertThat(aggs.fieldNames()).containsExactly("AUTHZ_FORBIDS#__FILTER__");
        assertThat(filterAgg.getJsonObject("filter").getJsonObject("term").getString("verdict")).isEqualTo("FORBID");
        assertThat(facetAgg.getJsonObject("terms").getString("field")).isEqualTo("action");
        assertThat(facetAgg.getJsonObject("aggs").fieldNames()).containsExactly("AUTHZ_FORBIDS#COUNT");
    }

    @Test
    void should_sort_a_scoped_metric_on_its_measure_inside_the_scope_filter() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(Metric.AUTHZ_FORBIDS, Set.of(Measure.COUNT), List.of(new Sort(Measure.COUNT, Sort.Order.DESC)))
            ),
            List.of(Facet.AUTHZ_ACTION)
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_FORBIDS#__FILTER__")
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_FORBIDS#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getMap()).containsExactly(Map.entry("AUTHZ_FORBIDS#COUNT", "desc"));
    }

    @Test
    void should_leave_the_default_order_of_a_scoped_metric_without_sort() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.MIN))),
            List.of(Facet.AUTHZ_ACTION),
            1
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getInteger("size")).isEqualTo(1);
        assertThat(terms.containsKey("order")).isFalse();
    }

    @Test
    void should_leave_the_default_order_of_an_unscoped_metric_without_sort() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION),
            1
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_DECISIONS#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.containsKey("order")).isFalse();
    }

    @Test
    void should_sort_an_unscoped_metric_on_the_flat_measure_aggregation_path() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT), List.of(new Sort(Measure.COUNT, Sort.Order.DESC)))
            ),
            List.of(Facet.AUTHZ_ACTION)
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_DECISIONS#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getString("AUTHZ_DECISIONS#COUNT")).isEqualTo("desc");
    }

    @Test
    void should_sort_the_duration_metric_on_its_measure_inside_the_scope_filter() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(
                new MetricMeasuresQuery(Metric.AUTHZ_EVAL_DURATION, Set.of(Measure.AVG), List.of(new Sort(Measure.AVG, Sort.Order.DESC)))
            ),
            List.of(Facet.AUTHZ_ACTION)
        );

        var terms = new JsonObject(adapter.adapt(query))
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#__FILTER__")
            .getJsonObject("aggs")
            .getJsonObject("AUTHZ_EVAL_DURATION#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getMap()).containsExactly(Map.entry("AUTHZ_EVAL_DURATION#AVG", "desc"));
    }

    static Stream<Arguments> sources() {
        return Stream.of(
            Arguments.of(new AuthzMeasuresQueryAdapter(), Metric.AUTHZ_DECISIONS),
            Arguments.of(new AuthzTrafficMeasuresQueryAdapter(), Metric.AUTHZ_SEARCHES)
        );
    }

    @ParameterizedTest
    @MethodSource("sources")
    void should_reject_more_than_one_facet(AuthzMeasuresAdapter source, Metric metric) {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(metric, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION, Facet.AUTHZ_SUBJECT_ID)
        );

        assertThatThrownBy(() -> new AuthzFacetsQueryAdapter(source).adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("single facet");
    }

    @ParameterizedTest
    @MethodSource("sources")
    void should_reject_range_facets(AuthzMeasuresAdapter source, Metric metric) {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(metric, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION),
            List.of(new NumberRange(0.0, 10.0))
        );

        assertThatThrownBy(() -> new AuthzFacetsQueryAdapter(source).adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("range facets");
    }

    @ParameterizedTest
    @MethodSource("sources")
    void should_reject_per_metric_filters(AuthzMeasuresAdapter source, Metric metric) {
        var perMetricFilters = List.of(new Filter(Filter.Name.AUTHZ_ACTION, Filter.Operator.IN, List.of("read")));
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(metric, Set.of(Measure.COUNT), perMetricFilters, List.of())),
            List.of(Facet.AUTHZ_ACTION)
        );

        assertThatThrownBy(() -> new AuthzFacetsQueryAdapter(source).adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("per-metric filters");
    }
}
