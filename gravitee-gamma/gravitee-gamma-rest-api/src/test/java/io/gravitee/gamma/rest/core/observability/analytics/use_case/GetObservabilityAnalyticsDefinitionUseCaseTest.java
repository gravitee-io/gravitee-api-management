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

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.ENVIRONMENT;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.ORGANIZATION;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.OTHER_ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.analytics.inmemory.InMemoryAnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.DimensionDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricFamily;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityAnalyticsDefinitionUseCaseTest {

    private static final List<ApiType> HTTP_KINDS = InMemoryAnalyticsDefinitionPort.HTTP_KINDS;
    private static final List<String> ENGINE_MEASURES = List.of("AVG", "SUM", "MIN", "MAX", "P50", "P90", "P95", "P99", "COUNT");
    private static final String BASKET = "additional-metrics.double_checkout_basket-value";
    private static final String FAILOVER = "additional-metrics.long_failover_count";
    private static final Instant SEEN = Instant.parse("2026-11-03T08:00:00Z");

    private MetricDefinitionFixture fixture;
    private GetObservabilityAnalyticsDefinitionUseCase useCase;

    @BeforeEach
    void setUp() {
        fixture = new MetricDefinitionFixture();
        useCase = new GetObservabilityAnalyticsDefinitionUseCase(
            fixture.ports,
            fixture.repository,
            fixture.ports,
            fixture.ports,
            fixture.ports,
            fixture.catalog
        );
    }

    private void givenBuiltIn(String name, List<ApiType> apiTypes, String... facets) {
        fixture.ports.givenBuiltInMetric(aMetric(name, apiTypes, facets));
    }

    @Nested
    class BuiltIns {

        @Test
        void should_return_the_metrics_of_the_requested_kind_only() {
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS);
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE));

            var definition = execute(Set.of(ApiType.HTTP_PROXY));

            assertThat(metricNames(definition)).containsExactly("HTTP_REQUESTS");
        }

        @Test
        void should_return_a_metric_shared_by_several_requested_kinds_once() {
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS);
            givenBuiltIn("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM));

            var definition = execute(Set.of(ApiType.HTTP_PROXY, ApiType.LLM));

            assertThat(metricNames(definition)).containsExactly("HTTP_REQUESTS", "LLM_PROMPT_TOTAL_TOKEN");
        }

        @Test
        void should_group_metrics_by_family_in_family_order_whatever_the_catalog_order() {
            givenBuiltIn("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM));
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS);
            givenBuiltIn("HTTP_ERRORS", HTTP_KINDS);

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
            givenBuiltIn("MCP_SHARED", List.of(ApiType.LLM, ApiType.MCP), "MCP_PROXY_METHOD");
            givenBuiltIn("HTTP_SHARED", List.of(ApiType.HTTP_PROXY, ApiType.A2A), "API");
            fixture.givenDeclared(BASKET);

            var mcpFirst = execute(new LinkedHashSet<>(List.of(ApiType.MCP, ApiType.HTTP_PROXY)));
            var httpFirst = execute(new LinkedHashSet<>(List.of(ApiType.HTTP_PROXY, ApiType.MCP)));

            assertThat(mcpFirst).isEqualTo(httpFirst);
            assertThat(metricNames(mcpFirst)).containsExactly("HTTP_SHARED", "MCP_SHARED", BASKET);
        }

        @Test
        void should_list_each_facet_of_the_metrics_once_in_the_order_the_families_show_them() {
            givenBuiltIn("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL");
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS, "API", "HTTP_STATUS");

            var definition = execute(Set.of(ApiType.LLM));

            assertThat(definition.dimensions())
                .extracting(DimensionDefinition::name)
                .containsExactly("API", "HTTP_STATUS", "LLM_PROXY_MODEL");
            assertThat(definition.dimensions().get(0).label()).isEqualTo("API label");
        }

        @Test
        void should_leave_out_the_families_without_metrics() {
            givenBuiltIn("AUTHZ_DECISIONS", List.of(ApiType.AUTHZ));

            var definition = execute(Set.of(ApiType.AUTHZ));

            assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.AUTHZ);
        }

        @Test
        void should_answer_an_empty_definition_for_a_kind_without_metrics() {
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS);

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
    }

    @Nested
    class AdditionalMetrics {

        @Test
        void should_list_the_declared_then_the_discovered_numeric_fields_last_as_the_engine_serves_them() {
            givenBuiltIn("HTTP_REQUESTS", HTTP_KINDS, "API");
            fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "€", true);
            fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), false);

            var definition = execute(Set.of(ApiType.HTTP_PROXY));

            assertThat(definition.families())
                .extracting(AnalyticsDefinition.FamilyMetrics::family)
                .containsExactly(MetricFamily.GATEWAY, MetricFamily.ADDITIONAL);
            var additional = definition.families().get(1).metrics();
            assertThat(additional).extracting(MetricDefinition::name).containsExactly(BASKET, FAILOVER);
            assertThat(additional.get(0)).isEqualTo(
                new MetricDefinition(
                    BASKET,
                    "Basket value",
                    "€",
                    InMemoryAnalyticsDefinitionPort.ADDITIONAL_METRIC_KINDS,
                    ENGINE_MEASURES,
                    fixture.ports.additionalMetricSupport().facets(),
                    List.of("API", "API_TYPE"),
                    CatalogOrigin.DECLARED
                )
            );
            assertThat(additional.get(1).label()).isEqualTo("Failover count");
            assertThat(additional.get(1).unit()).isNull();
            assertThat(additional.get(1).origin()).isEqualTo(CatalogOrigin.DISCOVERED);
            assertThat(additional.get(1).measures()).isEqualTo(ENGINE_MEASURES);
            assertThat(truncated(Set.of(ApiType.HTTP_PROXY))).isFalse();
        }

        @Test
        void should_label_a_declared_field_without_a_label_from_its_key_and_keep_it_declared_once_discovered() {
            fixture.givenDeclared(ENVIRONMENT, FAILOVER, null, null, true);
            fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), false);

            var additional = additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)));

            assertThat(additional)
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.label()).isEqualTo("Failover count");
                    assertThat(metric.unit()).isNull();
                    assertThat(metric.origin()).isEqualTo(CatalogOrigin.DECLARED);
                });
        }

        @Test
        void should_list_the_facets_of_the_additional_metrics_among_the_dimensions() {
            fixture.givenDeclared(BASKET);

            var definition = execute(Set.of(ApiType.MCP));

            assertThat(definition.families())
                .extracting(AnalyticsDefinition.FamilyMetrics::family)
                .containsExactly(MetricFamily.ADDITIONAL);
            assertThat(definition.dimensions()).extracting(DimensionDefinition::name).containsExactly("API", "HTTP_STATUS");
        }

        @Test
        void should_serve_the_declared_fields_only_to_a_caller_who_does_not_read_the_traffic_without_a_discovery() {
            fixture.ports.givenReadsTraffic(false);
            fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "€", true);
            fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

            var definition = execute(Set.of(ApiType.HTTP_PROXY));

            assertThat(additionalMetrics(definition)).extracting(MetricDefinition::name).containsExactly(BASKET);
            assertThat(truncated(Set.of(ApiType.HTTP_PROXY))).isFalse();
            assertThat(fixture.ports.discoveries()).isZero();
        }

        @Test
        void should_say_the_discovery_was_truncated_to_a_traffic_reader_only_when_the_kinds_admit_additional_metrics() {
            fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

            assertThat(truncated(Set.of(ApiType.HTTP_PROXY))).isTrue();
            assertThat(truncated(Set.of(ApiType.NATIVE))).isFalse();
        }

        @Test
        void should_drop_a_record_that_is_not_exposed_whether_or_not_documents_carry_its_key() {
            fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "€", false);
            fixture.ports.givenDiscovered(List.of(discovered("double_checkout_basket-value", AdditionalMetricValueType.DOUBLE)), false);

            assertThat(additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)))).isEmpty();
        }

        @Test
        void should_list_no_key_a_built_in_already_reads() {
            fixture.ports.givenBuiltIn("long_llm-proxy_tokens-sent", "LLM_PROMPT_TOKEN_SENT");
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("long_llm-proxy_tokens-sent", AdditionalMetricValueType.LONG),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var additional = additionalMetrics(execute(Set.of(ApiType.LLM)));

            assertThat(additional).extracting(MetricDefinition::name).containsExactly(FAILOVER);
            assertThat(fixture.ports.lastUncountedKeys()).contains("long_llm-proxy_tokens-sent");
        }

        @Test
        void should_list_only_the_fields_holding_a_number() {
            fixture.givenDeclared(ENVIRONMENT, "additional-metrics.keyword_fraud-check_tier", "Tier", null, true);
            fixture.givenDeclared(ENVIRONMENT, "additional-metrics.int_fraud-check_score", null, null, true);
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("bool_cache_hit", AdditionalMetricValueType.BOOL),
                    discovered("string_fraud-check_detail", AdditionalMetricValueType.STRING),
                    discovered("double_risk-score", AdditionalMetricValueType.DOUBLE)
                ),
                false
            );

            var additional = additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)));

            assertThat(additional)
                .extracting(MetricDefinition::name)
                .containsExactly("additional-metrics.int_fraud-check_score", "additional-metrics.double_risk-score");
        }

        @Test
        void should_serve_no_additional_metric_and_run_no_discovery_for_kinds_the_engine_does_not_measure_them_for() {
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE));
            fixture.givenDeclared(BASKET);
            fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

            var definition = execute(Set.of(ApiType.NATIVE));

            assertThat(definition.families()).extracting(AnalyticsDefinition.FamilyMetrics::family).containsExactly(MetricFamily.NATIVE);
            assertThat(fixture.ports.discoveries()).isZero();
        }

        @Test
        void should_serve_the_records_of_the_asked_environment_only() {
            fixture.givenDeclared(OTHER_ENVIRONMENT, BASKET, "Basket value", "€", true);
            fixture.givenDeclared(ENVIRONMENT, FAILOVER, null, null, true);

            var additional = additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)));

            assertThat(additional).extracting(MetricDefinition::name).containsExactly(FAILOVER);
        }

        @Test
        void should_skip_a_discovered_key_outside_the_grammar_or_reserved() {
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("double_custom_basket-value", AdditionalMetricValueType.DOUBLE),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            assertThat(additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)))).extracting(MetricDefinition::name).containsExactly(FAILOVER);
        }
    }

    private AnalyticsDefinition execute(Set<ApiType> apiTypes) {
        return useCase.execute(new GetObservabilityAnalyticsDefinitionUseCase.Input(ORGANIZATION, ENVIRONMENT, apiTypes)).definition();
    }

    private boolean truncated(Set<ApiType> apiTypes) {
        return useCase.execute(new GetObservabilityAnalyticsDefinitionUseCase.Input(ORGANIZATION, ENVIRONMENT, apiTypes)).truncated();
    }

    private static List<MetricDefinition> additionalMetrics(AnalyticsDefinition definition) {
        return definition
            .families()
            .stream()
            .filter(family -> family.family() == MetricFamily.ADDITIONAL)
            .flatMap(family -> family.metrics().stream())
            .toList();
    }

    private static List<String> metricNames(AnalyticsDefinition definition) {
        return definition
            .families()
            .stream()
            .flatMap(family -> family.metrics().stream())
            .map(MetricDefinition::name)
            .toList();
    }

    private static DiscoveredAdditionalMetric discovered(String rawKey, AdditionalMetricValueType valueType) {
        return new DiscoveredAdditionalMetric(rawKey, valueType, SEEN);
    }
}
