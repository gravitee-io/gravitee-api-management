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
        assertThat(resolver.fromMetric(Metric.AUTHZ_OPERATIONS)).isEqualTo("operation");
        assertThat(resolver.fromMetric(Metric.AUTHZ_DECISIONS)).isEqualTo("decision");
        assertThat(resolver.fromMetric(Metric.AUTHZ_PERMITS)).isEqualTo("decision");
        assertThat(resolver.fromMetric(Metric.AUTHZ_FORBIDS)).isEqualTo("decision");
        assertThat(resolver.fromMetric(Metric.AUTHZ_NOT_APPLICABLE)).isEqualTo("decision");
        assertThat(resolver.fromMetric(Metric.AUTHZ_SEARCHES)).isEqualTo("search-type");
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
    void should_mark_decision_scoped_metrics_with_their_decision_value() {
        assertThat(resolver.isDecisionScoped(Metric.AUTHZ_PERMITS)).isTrue();
        assertThat(resolver.decisionValue(Metric.AUTHZ_PERMITS)).isEqualTo("PERMIT");
        assertThat(resolver.decisionValue(Metric.AUTHZ_FORBIDS)).isEqualTo("FORBID");
        assertThat(resolver.decisionValue(Metric.AUTHZ_NOT_APPLICABLE)).isEqualTo("NOT_APPLICABLE");
        assertThat(resolver.isDecisionScoped(Metric.AUTHZ_DECISIONS)).isFalse();
    }

    @Test
    void should_mark_the_failure_metric_as_failure_scoped() {
        assertThat(resolver.isFailureScoped(Metric.AUTHZ_FAILURES)).isTrue();
        assertThat(resolver.isFailureScoped(Metric.AUTHZ_OPERATIONS)).isFalse();
    }

    @Test
    void should_resolve_every_authz_facet() {
        assertThat(resolver.fromFacet(Facet.AUTHZ_DECISION)).isEqualTo("decision");
        assertThat(resolver.fromFacet(Facet.AUTHZ_OPERATION)).isEqualTo("operation");
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
    void should_resolve_filters_through_the_same_mapping_as_facets() {
        var filter = new Filter(Filter.Name.AUTHZ_CALLER, Filter.Operator.IN, List.of("pep"));
        assertThat(resolver.fromFilter(filter)).isEqualTo("caller");
    }

    @Test
    void should_resolve_every_authz_metric_declared_on_the_enum() {
        var unresolved = Arrays.stream(Metric.values())
            .filter(metric -> metric.name().startsWith("AUTHZ_"))
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

    @Test
    void should_resolve_every_authz_facet_declared_on_the_enum() {
        var unresolved = Arrays.stream(Facet.values())
            .filter(facet -> facet.name().startsWith("AUTHZ_"))
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

    @Test
    void should_resolve_every_authz_filter_to_the_same_field_as_its_facet() {
        var mismatched = Arrays.stream(Filter.Name.values())
            .filter(name -> name.name().startsWith("AUTHZ_"))
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
