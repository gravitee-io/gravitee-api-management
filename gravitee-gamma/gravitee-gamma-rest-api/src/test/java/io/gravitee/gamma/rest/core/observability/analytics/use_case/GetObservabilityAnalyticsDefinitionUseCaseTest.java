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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.analytics.inmemory.InMemoryAnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricFamily;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityAnalyticsDefinitionUseCaseTest {

    private static final List<ApiType> HTTP_KINDS = List.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A);

    private final InMemoryAnalyticsDefinitionPort catalog = new InMemoryAnalyticsDefinitionPort();
    private final GetObservabilityAnalyticsDefinitionUseCase useCase = new GetObservabilityAnalyticsDefinitionUseCase(catalog);

    @Test
    void should_return_the_metrics_of_the_requested_kind_only() {
        catalog.givenMetric("HTTP_REQUESTS", HTTP_KINDS);
        catalog.givenMetric("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE));

        var definition = execute(Set.of(ApiType.HTTP_PROXY));

        assertThat(metricNames(definition)).containsExactly("HTTP_REQUESTS");
    }

    @Test
    void should_return_a_metric_shared_by_several_requested_kinds_once() {
        catalog.givenMetric("HTTP_REQUESTS", HTTP_KINDS);
        catalog.givenMetric("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM));

        var definition = execute(Set.of(ApiType.HTTP_PROXY, ApiType.LLM));

        assertThat(metricNames(definition)).containsExactly("HTTP_REQUESTS", "LLM_PROMPT_TOTAL_TOKEN");
    }

    @Test
    void should_group_metrics_by_family_in_family_order_whatever_the_catalog_order() {
        catalog.givenMetric("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM));
        catalog.givenMetric("HTTP_REQUESTS", HTTP_KINDS);
        catalog.givenMetric("HTTP_ERRORS", HTTP_KINDS);

        var definition = execute(Set.of(ApiType.LLM));

        assertThat(definition.families())
            .extracting(AnalyticsDefinition.FamilyMetrics::family)
            .containsExactly(MetricFamily.GATEWAY, MetricFamily.LLM);
        assertThat(definition.families().get(0).metrics())
            .extracting(MetricDefinition::name)
            .containsExactly("HTTP_REQUESTS", "HTTP_ERRORS");
    }

    @Test
    void should_answer_the_same_kinds_the_same_way_whatever_the_order_they_are_asked_in() {
        catalog.givenMetric("MCP_SHARED", List.of(ApiType.LLM, ApiType.MCP), "MCP_PROXY_METHOD");
        catalog.givenMetric("HTTP_SHARED", List.of(ApiType.HTTP_PROXY, ApiType.A2A), "API");

        var mcpFirst = execute(new LinkedHashSet<>(List.of(ApiType.MCP, ApiType.HTTP_PROXY)));
        var httpFirst = execute(new LinkedHashSet<>(List.of(ApiType.HTTP_PROXY, ApiType.MCP)));

        assertThat(mcpFirst).isEqualTo(httpFirst);
        assertThat(metricNames(mcpFirst)).containsExactly("HTTP_SHARED", "MCP_SHARED");
    }

    @Test
    void should_list_each_facet_of_the_metrics_once_in_the_order_the_families_show_them() {
        catalog.givenMetric("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL");
        catalog.givenMetric("HTTP_REQUESTS", HTTP_KINDS, "API", "HTTP_STATUS");

        var definition = execute(Set.of(ApiType.LLM));

        assertThat(definition.dimensions()).extracting(DimensionDefinition::name).containsExactly("API", "HTTP_STATUS", "LLM_PROXY_MODEL");
        assertThat(definition.dimensions().get(0).label()).isEqualTo("API label");
    }

    @Test
    void should_leave_out_the_families_without_metrics() {
        catalog.givenMetric("AUTHZ_DECISIONS", List.of(ApiType.AUTHZ));

        var definition = execute(Set.of(ApiType.AUTHZ));

        assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.AUTHZ);
    }

    @Test
    void should_answer_an_empty_definition_for_a_kind_without_metrics() {
        catalog.givenMetric("HTTP_REQUESTS", HTTP_KINDS);

        var definition = execute(Set.of(ApiType.EDGE));

        assertThat(definition.families()).isEmpty();
        assertThat(definition.dimensions()).isEmpty();
    }

    @Test
    void should_refuse_a_request_without_any_api_kind() {
        assertThatThrownBy(() -> execute(Set.of()))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.api_types_required");
    }

    @Test
    void should_refuse_the_decision_scope_which_is_not_an_api_kind() {
        assertThatThrownBy(() -> execute(EnumSet.of(ApiType.HTTP_PROXY, ApiType.AUTHZ_DECISION)))
            .isInstanceOf(InvalidObservabilityQueryException.class)
            .extracting("technicalCode")
            .isEqualTo("observability.query.not_an_api_kind");
    }

    private AnalyticsDefinition execute(Set<ApiType> apiTypes) {
        return useCase.execute(new GetObservabilityAnalyticsDefinitionUseCase.Input(apiTypes)).definition();
    }

    private static List<String> metricNames(AnalyticsDefinition definition) {
        return definition
            .families()
            .stream()
            .flatMap(family -> family.metrics().stream())
            .map(MetricDefinition::name)
            .toList();
    }
}
