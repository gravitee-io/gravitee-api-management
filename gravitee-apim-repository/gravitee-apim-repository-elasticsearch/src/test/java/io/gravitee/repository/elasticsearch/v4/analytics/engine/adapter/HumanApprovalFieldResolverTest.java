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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HumanApprovalFieldResolverTest {

    private final HumanApprovalFieldResolver resolver = new HumanApprovalFieldResolver();

    @ParameterizedTest
    @CsvSource({ "HUMAN_APPROVALS,case-id", "HUMAN_APPROVAL_COST,additional-metrics.double_human-approval_cost" })
    void should_resolve_each_metric_to_its_field(Metric metric, String field) {
        assertThat(resolver.fromMetric(metric)).isEqualTo(field);
    }

    @ParameterizedTest
    @CsvSource(
        {
            "API,api-id",
            "APPLICATION,app-id",
            "PLAN,plan-id",
            "GATEWAY,gw-id",
            "HUMAN_APPROVAL_VERDICT,verdict",
            "HUMAN_APPROVAL_TOOL,resource-id",
        }
    )
    void should_resolve_a_dimension_to_the_same_field_as_a_filter_and_as_a_facet(String dimension, String field) {
        assertThat(resolver.fromFilter(new Filter(Filter.Name.valueOf(dimension), Filter.Operator.EQ, "x"))).isEqualTo(field);
        assertThat(resolver.fromFacet(Facet.valueOf(dimension))).isEqualTo(field);
    }

    @Test
    void should_refuse_a_metric_of_another_family() {
        assertThatThrownBy(() -> resolver.fromMetric(Metric.AUTHZ_PERMITS))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("AUTHZ_PERMITS");
    }

    @Test
    void should_name_the_offending_side_when_a_dimension_is_unsupported() {
        assertThatThrownBy(() -> resolver.fromFacet(Facet.HTTP_STATUS))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("facet 'HTTP_STATUS'");

        assertThatThrownBy(() -> resolver.fromFilter(new Filter(Filter.Name.HTTP_STATUS, Filter.Operator.EQ, 200)))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("filter 'HTTP_STATUS'");
    }
}
