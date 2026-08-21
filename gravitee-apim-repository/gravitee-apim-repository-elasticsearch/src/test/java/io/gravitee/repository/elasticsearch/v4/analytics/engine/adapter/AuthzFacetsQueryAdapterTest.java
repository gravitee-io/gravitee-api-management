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
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery.Sort;
import io.gravitee.repository.analytics.engine.api.query.NumberRange;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthzFacetsQueryAdapterTest extends AbstractQueryAdapterTest {

    private final AuthzFacetsQueryAdapter adapter = new AuthzFacetsQueryAdapter();

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

        assertThat(terms.getString("field")).isEqualTo("decision");
        assertThat(terms.getInteger("size")).isEqualTo(5);
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
        ).isEqualTo("decision");
    }

    @Test
    void should_keep_the_doc_type_filter_on_a_faceted_query() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION)
        );

        var filters = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(filters.encode()).contains("\"doc-type\":\"authz\"");
    }

    @Test
    void should_nest_a_decision_scoped_metric_filter_inside_the_terms_bucket() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_FORBIDS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION)
        );

        var bucket = new JsonObject(adapter.adapt(query)).getJsonObject("aggs").getJsonObject("AUTHZ_FORBIDS#AUTHZ_ACTION");
        var filterAgg = bucket.getJsonObject("aggs").getJsonObject("AUTHZ_FORBIDS#__FILTER__");

        assertThat(filterAgg.getJsonObject("filter").getJsonObject("term").getString("decision")).isEqualTo("FORBID");
    }

    @Test
    void should_sort_a_scoped_metric_on_the_nested_filter_aggregation_path() {
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
            .getJsonObject("AUTHZ_FORBIDS#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getString("AUTHZ_FORBIDS#__FILTER__>AUTHZ_FORBIDS#COUNT")).isEqualTo("desc");
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
    void should_sort_the_duration_metric_on_the_nested_filter_aggregation_path() {
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
            .getJsonObject("AUTHZ_EVAL_DURATION#AUTHZ_ACTION")
            .getJsonObject("terms");

        assertThat(terms.getJsonObject("order").getString("AUTHZ_EVAL_DURATION#__FILTER__>AUTHZ_EVAL_DURATION#AVG")).isEqualTo("desc");
    }

    @Test
    void should_reject_more_than_one_facet() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION, Facet.AUTHZ_DECISION)
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("single facet");
    }

    @Test
    void should_reject_range_facets() {
        var query = new FacetsQuery(
            buildTimeRange(),
            List.of(),
            List.of(new MetricMeasuresQuery(Metric.AUTHZ_DECISIONS, Set.of(Measure.COUNT))),
            List.of(Facet.AUTHZ_ACTION),
            List.of(new NumberRange(0.0, 10.0))
        );

        assertThatThrownBy(() -> adapter.adapt(query))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("range facets");
    }
}
