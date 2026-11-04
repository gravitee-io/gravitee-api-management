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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MetricFamilyTest {

    @ParameterizedTest
    @CsvSource({ "LLM, LLM", "MESSAGE, MESSAGE", "EDGE, EDGE", "NATIVE, NATIVE", "AUTHZ, AUTHZ" })
    void should_put_a_metric_of_one_kind_in_that_kinds_family(ApiType kind, MetricFamily family) {
        assertThat(MetricFamily.of(List.of(kind))).isEqualTo(family);
    }

    @Test
    void should_put_a_metric_shared_by_several_kinds_in_the_gateway_family() {
        assertThat(MetricFamily.of(List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A))).isEqualTo(
            MetricFamily.GATEWAY
        );
    }

    @ParameterizedTest
    @CsvSource({ "HTTP_PROXY", "MCP", "A2A" })
    void should_put_a_metric_of_a_kind_without_a_family_of_its_own_in_the_gateway_family(ApiType kind) {
        assertThat(MetricFamily.of(List.of(kind))).isEqualTo(MetricFamily.GATEWAY);
    }

    @Test
    void should_count_a_kind_listed_twice_once() {
        assertThat(MetricFamily.of(List.of(ApiType.LLM, ApiType.LLM))).isEqualTo(MetricFamily.LLM);
    }

    /** One vocabulary for a kind: the family of a kind is labelled as the kind's `API_TYPE` filter value. */
    @ParameterizedTest
    @CsvSource({ "LLM, LLM", "MESSAGE, MESSAGE", "EDGE, EDGE", "NATIVE, NATIVE", "AUTHZ, AUTHZ" })
    void should_label_the_family_of_a_kind_as_the_kind(MetricFamily family, ApiType kind) {
        assertThat(family.ownKind()).contains(kind);
        assertThat(family.label()).isEqualTo(kind.label());
    }

    @Test
    void should_label_the_families_of_no_kind_on_their_own() {
        assertThat(MetricFamily.GATEWAY.ownKind()).isEmpty();
        assertThat(MetricFamily.GATEWAY.label()).isEqualTo("Gateway");
        assertThat(MetricFamily.ADDITIONAL.ownKind()).isEmpty();
    }

    /** The editor lists the families in enum order: the environment's own metrics close the list. */
    @Test
    void should_list_the_additional_metrics_last_under_their_own_label() {
        assertThat(MetricFamily.values()[MetricFamily.values().length - 1]).isEqualTo(MetricFamily.ADDITIONAL);
        assertThat(MetricFamily.ADDITIONAL.label()).isEqualTo("Additional metrics");
    }

    @Test
    void should_never_derive_the_additional_family_from_api_kinds() {
        for (var kind : ApiType.values()) {
            assertThat(MetricFamily.of(List.of(kind))).as(kind.name()).isNotEqualTo(MetricFamily.ADDITIONAL);
        }
        assertThat(MetricFamily.of(List.of())).isNotEqualTo(MetricFamily.ADDITIONAL);
    }
}
