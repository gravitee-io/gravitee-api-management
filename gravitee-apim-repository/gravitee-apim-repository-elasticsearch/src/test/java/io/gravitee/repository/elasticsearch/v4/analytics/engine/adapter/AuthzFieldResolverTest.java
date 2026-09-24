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
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthzFieldResolverTest {

    private final AuthzFieldResolver resolver = new AuthzFieldResolver();

    @Test
    void should_resolve_counter_metrics_by_field_presence() {
        assertThat(resolver.fromMetric(Metric.AUTHZ_DECISIONS)).isEqualTo("event-id");
        assertThat(resolver.fromMetric(Metric.AUTHZ_PERMITS)).isEqualTo("verdict");
        assertThat(resolver.fromMetric(Metric.AUTHZ_FORBIDS)).isEqualTo("verdict");
        assertThat(resolver.fromMetric(Metric.AUTHZ_NOT_APPLICABLE)).isEqualTo("indeterminate-cause");
        assertThat(resolver.fromMetric(Metric.AUTHZ_FAILURES)).isEqualTo("status");
        assertThat(resolver.fromMetric(Metric.AUTHZ_EVAL_DURATION)).isEqualTo("duration-nanos");
    }

    @Test
    void should_reject_a_metric_that_is_not_an_authz_metric() {
        assertThatThrownBy(() -> resolver.fromMetric(Metric.HTTP_REQUESTS))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("HTTP_REQUESTS");
    }

    @Test
    void should_reject_operations_and_searches_now_that_they_are_served_from_pdp_api_traffic() {
        assertThatThrownBy(() -> resolver.fromMetric(Metric.AUTHZ_OPERATIONS)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> resolver.fromMetric(Metric.AUTHZ_SEARCHES)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_scope_permits_and_forbids_by_the_verdict_of_the_pdp() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_PERMITS)).contains(new AuthzFieldResolver.ScopeTerm("verdict", "PERMIT"));
        assertThat(resolver.scopeTerm(Metric.AUTHZ_FORBIDS)).contains(new AuthzFieldResolver.ScopeTerm("verdict", "FORBID"));
    }

    @Test
    void should_scope_not_applicable_by_the_indeterminate_cause() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_NOT_APPLICABLE)).contains(
            new AuthzFieldResolver.ScopeTerm("indeterminate-cause", "NOT_APPLICABLE")
        );
    }

    @Test
    void should_scope_failures_by_the_error_status() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_FAILURES)).contains(new AuthzFieldResolver.ScopeTerm("status", "error"));
    }

    @Test
    void should_not_scope_the_decision_count() {
        assertThat(resolver.scopeTerm(Metric.AUTHZ_DECISIONS)).isEmpty();
    }

    @Test
    void should_resolve_every_authz_facet() {
        assertThat(resolver.fromFacet(Facet.AUTHZ_DECISION)).isEqualTo("verdict");
        assertThat(resolver.fromFacet(Facet.AUTHZ_PDP)).isEqualTo("decision-point-id");
        assertThat(resolver.fromFacet(Facet.AUTHZ_STATUS)).isEqualTo("status");
        assertThat(resolver.fromFacet(Facet.AUTHZ_CALLER)).isEqualTo("caller");
        assertThat(resolver.fromFacet(Facet.AUTHZ_SUBJECT_ID)).isEqualTo("subject-id");
        assertThat(resolver.fromFacet(Facet.AUTHZ_ACTION)).isEqualTo("action");
        assertThat(resolver.fromFacet(Facet.AUTHZ_RESOURCE_ID)).isEqualTo("resource-id");
        assertThat(resolver.fromFacet(Facet.AUTHZ_REASON)).isEqualTo("reasons");
        assertThat(resolver.fromFacet(Facet.API)).isEqualTo("api-id");
        assertThat(resolver.fromFacet(Facet.GATEWAY)).isEqualTo("gw-id");
    }

    @Test
    void should_reject_the_operation_facet_now_that_decisions_no_longer_carry_it() {
        assertThatThrownBy(() -> resolver.fromFacet(Facet.AUTHZ_OPERATION)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_resolve_the_type_field_of_the_subject_and_resource_filters() {
        var subject = new Filter(Filter.Name.AUTHZ_SUBJECT_ID, Filter.Operator.EQ, "User::\"alice\"");
        var resource = new Filter(Filter.Name.AUTHZ_RESOURCE_ID, Filter.Operator.EQ, "Doc::\"d1\"");

        assertThat(resolver.entityTypeFromFilter(subject)).isEqualTo("subject-type");
        assertThat(resolver.entityTypeFromFilter(resource)).isEqualTo("resource-type");
    }

    @Test
    void should_reject_the_type_field_of_a_filter_that_names_no_entity() {
        var action = new Filter(Filter.Name.AUTHZ_ACTION, Filter.Operator.EQ, "read");

        assertThatThrownBy(() -> resolver.entityTypeFromFilter(action)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_resolve_filters_through_the_same_mapping_as_facets() {
        var filter = new Filter(Filter.Name.AUTHZ_CALLER, Filter.Operator.IN, List.of("pep"));
        assertThat(resolver.fromFilter(filter)).isEqualTo("caller");
    }

    @Test
    void should_reject_the_operation_filter_now_that_decisions_no_longer_carry_it() {
        var filter = new Filter(Filter.Name.AUTHZ_OPERATION, Filter.Operator.EQ, "search");

        assertThatThrownBy(() -> resolver.fromFilter(filter)).isInstanceOf(UnsupportedOperationException.class);
    }

    private static final List<Metric> DECISION_UNSUPPORTED_METRICS = List.of(Metric.AUTHZ_OPERATIONS, Metric.AUTHZ_SEARCHES);

    @Test
    void should_resolve_every_authz_metric_declared_on_the_enum() {
        var unresolved = Arrays.stream(Metric.values())
            .filter(metric -> metric.name().startsWith("AUTHZ_"))
            .filter(metric -> !DECISION_UNSUPPORTED_METRICS.contains(metric))
            .filter(metric -> {
                try {
                    return resolver.fromMetric(metric) == null;
                } catch (UnsupportedOperationException e) {
                    return true;
                }
            })
            .toList();

        assertThat(unresolved).as("every AUTHZ_ metric on the enum needs a field here, or it throws at query time").isEmpty();
    }

    private static final List<Facet> DECISION_UNSUPPORTED_FACETS = List.of(Facet.AUTHZ_SEARCH_TYPE, Facet.AUTHZ_OPERATION);

    @Test
    void should_resolve_every_authz_facet_declared_on_the_enum() {
        var unresolved = Arrays.stream(Facet.values())
            .filter(facet -> facet.name().startsWith("AUTHZ_"))
            .filter(facet -> !DECISION_UNSUPPORTED_FACETS.contains(facet))
            .filter(facet -> {
                try {
                    return resolver.fromFacet(facet) == null;
                } catch (UnsupportedOperationException e) {
                    return true;
                }
            })
            .toList();

        assertThat(unresolved).as("every AUTHZ_ facet on the enum needs a field here").isEmpty();
    }

    private static final List<Filter.Name> DECISION_UNSUPPORTED_FILTERS = List.of(
        Filter.Name.AUTHZ_OPERATION,
        Filter.Name.AUTHZ_SEARCH_TYPE
    );

    @Test
    void should_resolve_every_authz_filter_to_the_same_field_as_its_facet() {
        var mismatched = Arrays.stream(Filter.Name.values())
            .filter(name -> name.name().startsWith("AUTHZ_"))
            .filter(name -> !DECISION_UNSUPPORTED_FILTERS.contains(name))
            .filter(name -> {
                var filter = new Filter(name, Filter.Operator.EQ, List.of("x"));
                try {
                    return !resolver.fromFilter(filter).equals(resolver.fromFacet(Facet.valueOf(name.name())));
                } catch (RuntimeException e) {
                    return true;
                }
            })
            .toList();

        assertThat(mismatched)
            .as("a filter and the facet of the same name must read the same field, or the two switches have drifted")
            .isEmpty();
    }
}
