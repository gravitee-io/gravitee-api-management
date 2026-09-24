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

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthzTrafficFieldResolverTest {

    private final AuthzTrafficFieldResolver resolver = new AuthzTrafficFieldResolver();

    @Test
    void should_count_operations_and_searches_on_the_same_operation_field() {
        assertThat(resolver.fromMetric(Metric.AUTHZ_OPERATIONS)).isEqualTo("additional-metrics.keyword_authz_operation");
        assertThat(resolver.fromMetric(Metric.AUTHZ_SEARCHES)).isEqualTo("additional-metrics.keyword_authz_operation");
    }

    @Test
    void should_reject_a_metric_that_is_not_an_authz_traffic_metric() {
        assertThatThrownBy(() -> resolver.fromMetric(Metric.HTTP_REQUESTS))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("HTTP_REQUESTS");
    }

    @Test
    void should_scope_searches_to_the_search_operation() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_SEARCHES)).contains(
            new AuthzTrafficFieldResolver.ScopeTerm("additional-metrics.keyword_authz_operation", "search")
        );
    }

    @Test
    void should_not_scope_operations() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_OPERATIONS)).isEmpty();
    }

    @Test
    void should_count_no_operation_when_filtered_on_a_field_only_searches_carry() {
        for (var name : List.of(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Name.AUTHZ_ACTION, Filter.Name.AUTHZ_RESOURCE_ID)) {
            assertThat(resolver.countsNothing(Metric.AUTHZ_OPERATIONS, List.of(new Filter(name, Filter.Operator.EQ, "x")))).isTrue();
        }
    }

    @Test
    void should_keep_counting_operations_on_fields_every_operation_carries() {
        var filters = List.of(
            new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1")),
            new Filter(Filter.Name.AUTHZ_OPERATION, Filter.Operator.EQ, "evaluation"),
            new Filter(Filter.Name.AUTHZ_SEARCH_TYPE, Filter.Operator.EQ, "subject")
        );
        assertThat(resolver.countsNothing(Metric.AUTHZ_OPERATIONS, filters)).isFalse();
    }

    @Test
    void should_keep_counting_searches_on_fields_every_search_carries() {
        var filters = List.of(new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\""));
        assertThat(resolver.countsNothing(Metric.AUTHZ_SEARCHES, filters)).isFalse();
    }

    @Test
    void should_resolve_every_authz_traffic_filter() {
        assertThat(resolver.fromFilter(new Filter(Filter.Name.API, Filter.Operator.IN, List.of("api-1")))).isEqualTo("api-id");
        assertThat(resolver.fromFilter(new Filter(Filter.Name.GATEWAY, Filter.Operator.IN, List.of("gw-1")))).isEqualTo("gateway");
        assertThat(resolver.fromFilter(new Filter(Filter.Name.AUTHZ_OPERATION, Filter.Operator.EQ, "search"))).isEqualTo(
            "additional-metrics.keyword_authz_operation"
        );
        assertThat(resolver.fromFilter(new Filter(Filter.Name.AUTHZ_SEARCH_TYPE, Filter.Operator.EQ, "subject"))).isEqualTo(
            "additional-metrics.keyword_authz_search-type"
        );
        assertThat(resolver.fromFilter(new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\""))).isEqualTo(
            "additional-metrics.keyword_authz_subject-id"
        );
        assertThat(resolver.fromFilter(new Filter(Filter.Name.AUTHZ_ACTION, Filter.Operator.EQ, "read"))).isEqualTo(
            "additional-metrics.keyword_authz_action"
        );
        assertThat(resolver.fromFilter(new Filter(Filter.Name.AUTHZ_RESOURCE_ID, Filter.Operator.EQ, "Doc::\"d1\""))).isEqualTo(
            "additional-metrics.keyword_authz_resource-id"
        );
    }

    @Test
    void should_reject_a_decision_only_filter() {
        var filter = new Filter(Filter.Name.AUTHZ_PDP, Filter.Operator.EQ, "default");

        assertThatThrownBy(() -> resolver.fromFilter(filter)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_resolve_the_type_field_of_the_subject_and_resource_filters() {
        var subject = new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\"");
        var resource = new Filter(Filter.Name.AUTHZ_RESOURCE_ID, Filter.Operator.EQ, "Doc::\"d1\"");

        assertThat(resolver.entityTypeFromFilter(subject)).isEqualTo("additional-metrics.keyword_authz_subject-type");
        assertThat(resolver.entityTypeFromFilter(resource)).isEqualTo("additional-metrics.keyword_authz_resource-type");
    }

    @Test
    void should_reject_the_type_field_of_a_filter_that_names_no_entity() {
        var action = new Filter(Filter.Name.AUTHZ_ACTION, Filter.Operator.EQ, "read");

        assertThatThrownBy(() -> resolver.entityTypeFromFilter(action)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_resolve_every_authz_traffic_facet() {
        assertThat(resolver.fromFacet(Facet.API)).isEqualTo("api-id");
        assertThat(resolver.fromFacet(Facet.GATEWAY)).isEqualTo("gateway");
        assertThat(resolver.fromFacet(Facet.AUTHZ_OPERATION)).isEqualTo("additional-metrics.keyword_authz_operation");
        assertThat(resolver.fromFacet(Facet.AUTHZ_SEARCH_TYPE)).isEqualTo("additional-metrics.keyword_authz_search-type");
        assertThat(resolver.fromFacet(Facet.AUTHZ_SUBJECT_ID)).isEqualTo("additional-metrics.keyword_authz_subject-id");
        assertThat(resolver.fromFacet(Facet.AUTHZ_ACTION)).isEqualTo("additional-metrics.keyword_authz_action");
        assertThat(resolver.fromFacet(Facet.AUTHZ_RESOURCE_ID)).isEqualTo("additional-metrics.keyword_authz_resource-id");
    }

    @Test
    void should_reject_a_decision_only_facet() {
        assertThatThrownBy(() -> resolver.fromFacet(Facet.AUTHZ_PDP)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_resolve_every_filter_to_the_same_field_as_its_facet() {
        for (var name : List.of(
            Filter.Name.API,
            Filter.Name.GATEWAY,
            Filter.Name.AUTHZ_OPERATION,
            Filter.Name.AUTHZ_SEARCH_TYPE,
            Filter.Name.AUTHZ_SUBJECT_ID,
            Filter.Name.AUTHZ_ACTION,
            Filter.Name.AUTHZ_RESOURCE_ID
        )) {
            var filter = new Filter(name, Filter.Operator.EQ, "x");
            assertThat(resolver.fromFilter(filter)).as("filter %s", name).isEqualTo(resolver.fromFacet(Facet.valueOf(name.name())));
        }
    }
}
