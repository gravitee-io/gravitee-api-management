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
import static org.assertj.core.api.Assertions.tuple;

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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
    private static final String TOKENS_CACHED = "additional-metrics.long_llm-proxy_tokens-cached";
    private static final String PROTOCOL_VERSION = "additional-metrics.keyword_mcp-proxy_endpoint-protocol-version";
    private static final Instant SEEN = Instant.parse("2026-11-03T08:00:00Z");
    /** The HTTP family among the kinds the fixture's engine measures additional metrics for, in its order. */
    private static final List<ApiType> MEASURED_HTTP_FAMILY = List.of(
        ApiType.HTTP_PROXY,
        ApiType.LLM,
        ApiType.MESSAGE,
        ApiType.MCP,
        ApiType.A2A
    );

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
            assertThat(additional.get(0)).satisfies(metric -> {
                assertThat(metric.label()).isEqualTo("Basket value");
                assertThat(metric.unit()).isEqualTo("€");
                assertThat(metric.description()).isNull();
                assertThat(metric.namespace()).isNull();
                assertThat(metric.apiTypes()).containsExactlyInAnyOrderElementsOf(InMemoryAnalyticsDefinitionPort.ADDITIONAL_METRIC_KINDS);
                assertThat(metric.measures()).isEqualTo(ENGINE_MEASURES);
                assertThat(metric.facets()).isEqualTo(fixture.ports.additionalMetricSupport().facets());
                assertThat(metric.filters()).containsExactly("API", "API_TYPE");
                assertThat(metric.origin()).isEqualTo(CatalogOrigin.DECLARED);
                assertThat(metric.currency()).isNull();
            });
            assertThat(additional.get(1).label()).isEqualTo("Failover count");
            assertThat(additional.get(1).unit()).isNull();
            assertThat(additional.get(1).origin()).isEqualTo(CatalogOrigin.DISCOVERED);
            assertThat(additional.get(1).apiTypes()).containsExactlyInAnyOrderElementsOf(MEASURED_HTTP_FAMILY);
            assertThat(additional.get(1).namespace()).isEqualTo("failover");
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
        void should_serve_no_currency_code_for_a_declared_field_in_currency_its_currency_being_unknown() {
            fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "CURRENCY", true);

            var additional = additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY)));

            assertThat(additional)
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.unit()).isEqualTo("CURRENCY");
                    assertThat(metric.currency()).isNull();
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

    /** A dynamic entry is scoped and named by the namespace of its key (OBS-193). */
    @Nested
    class NamespaceScoping {

        @Test
        void should_tie_a_numeric_key_to_the_kinds_of_its_namespace_among_the_engine_s_and_label_it_without_the_namespace() {
            fixture.givenDeclared(TOKENS_CACHED);

            var additional = additionalMetrics(execute(Set.of(ApiType.LLM)));

            assertThat(additional)
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.label()).isEqualTo("Tokens cached");
                    assertThat(metric.apiTypes()).containsExactly(ApiType.LLM);
                    assertThat(metric.namespace()).isEqualTo("llm-proxy");
                    assertThat(metric.description()).isNull();
                });
            assertThat(additionalMetrics(execute(Set.of(ApiType.MCP)))).as("not a kind its namespace ties it to").isEmpty();
        }

        /** The widget editor offers a metric for the kinds it carries: a key of no kind would be offered nowhere. */
        @Test
        void should_serve_a_key_of_no_known_namespace_for_every_kind_the_engine_measures_such_a_field_for() {
            fixture.ports.givenDiscovered(List.of(discovered("double_risk-score", AdditionalMetricValueType.DOUBLE)), false);

            for (var kind : InMemoryAnalyticsDefinitionPort.ADDITIONAL_METRIC_KINDS) {
                assertThat(additionalMetrics(execute(Set.of(kind))))
                    .as(kind.name())
                    .singleElement()
                    .satisfies(metric -> {
                        assertThat(metric.apiTypes()).containsExactlyInAnyOrderElementsOf(
                            InMemoryAnalyticsDefinitionPort.ADDITIONAL_METRIC_KINDS
                        );
                        assertThat(metric.namespace()).isNull();
                    });
            }
        }

        @Test
        void should_list_no_metric_for_a_key_whose_namespace_ties_it_to_kinds_the_engine_measures_no_such_field_for() {
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE));
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("long_native-kafka_messages-in", AdditionalMetricValueType.LONG),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            assertThat(additionalMetrics(execute(Set.of(ApiType.HTTP_PROXY, ApiType.NATIVE))))
                .extracting(MetricDefinition::name)
                .containsExactly(FAILOVER);
            assertThat(additionalMetrics(execute(Set.of(ApiType.NATIVE)))).isEmpty();
        }

        @Test
        void should_drop_the_native_kafka_keys_and_keep_the_failover_keys_for_the_ai_kinds() {
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("long_native-kafka_messages-in", AdditionalMetricValueType.LONG),
                    discovered("keyword_native-kafka_client-id", AdditionalMetricValueType.KEYWORD),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.LLM, ApiType.MCP, ApiType.A2A));

            assertThat(additionalMetrics(definition)).extracting(MetricDefinition::name).containsExactly(FAILOVER);
            assertThat(definition.dimensions())
                .extracting(DimensionDefinition::name)
                .doesNotContain("additional-metrics.keyword_native-kafka_client-id");
        }

        @Test
        void should_let_a_declared_label_win_over_the_derived_one_and_serve_the_record_description() {
            fixture.givenDescribed(TOKENS_CACHED, "Cached tokens", "Tokens served from the cache");

            assertThat(additionalMetrics(execute(Set.of(ApiType.LLM))))
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.label()).isEqualTo("Cached tokens");
                    assertThat(metric.description()).isEqualTo("Tokens served from the cache");
                });
        }
    }

    /** The environment's keyword and bool keys are facets of the definition (OBS-193, D5). */
    @Nested
    class DynamicFacets {

        @Test
        void should_add_them_to_the_facets_of_the_built_in_the_additional_metrics_take_theirs_from_and_of_every_additional_metric() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            givenBuiltIn("HTTP_ERRORS", HTTP_KINDS, "API");
            fixture.givenDeclared(PROTOCOL_VERSION);
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("bool_pii_detected", AdditionalMetricValueType.BOOL),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.LLM, ApiType.MCP));

            var facetsOf = facetNamesByMetric(definition);
            assertThat(facetsOf.get(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF)).containsExactly(
                "API",
                PROTOCOL_VERSION,
                "additional-metrics.bool_pii_detected"
            );
            assertThat(facetsOf.get("HTTP_ERRORS")).containsExactly("API");
            assertThat(facetsOf.get(FAILOVER)).containsExactly(
                "API",
                "HTTP_STATUS",
                PROTOCOL_VERSION,
                "additional-metrics.bool_pii_detected"
            );
            assertThat(definition.dimensions())
                .extracting(DimensionDefinition::name, DimensionDefinition::label, DimensionDefinition::origin)
                .containsExactly(
                    tuple("API", "API label", CatalogOrigin.BUILT_IN),
                    tuple(PROTOCOL_VERSION, "Endpoint protocol version", CatalogOrigin.DECLARED),
                    tuple("additional-metrics.bool_pii_detected", "Detected", CatalogOrigin.DISCOVERED),
                    tuple("HTTP_STATUS", "HTTP_STATUS label", CatalogOrigin.BUILT_IN)
                );
        }

        /** The dimension picker groups a facet by the same namespace and kinds the filter catalog serves for its key. */
        @Test
        void should_serve_the_namespace_and_kinds_of_a_dynamic_dimension_and_none_on_an_unknown_namespace() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.givenDeclared(PROTOCOL_VERSION);
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("bool_pii_detected", AdditionalMetricValueType.BOOL),
                    discovered("keyword_failover_target", AdditionalMetricValueType.KEYWORD),
                    discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.LLM, ApiType.MCP));

            assertThat(definition.dimensions())
                .extracting(DimensionDefinition::name, DimensionDefinition::namespace, DimensionDefinition::apiTypes)
                .containsExactlyInAnyOrder(
                    tuple("API", null, Set.copyOf(HTTP_KINDS)),
                    tuple(PROTOCOL_VERSION, "mcp-proxy", Set.of(ApiType.MCP)),
                    tuple("additional-metrics.bool_pii_detected", "pii", Set.of(ApiType.LLM)),
                    tuple(
                        "additional-metrics.keyword_failover_target",
                        "failover",
                        Set.of(ApiType.HTTP_PROXY, ApiType.MESSAGE, ApiType.LLM, ApiType.MCP, ApiType.A2A)
                    ),
                    tuple("additional-metrics.keyword_fraud-check_tier", null, Set.of())
                );
        }

        /**
         * A built-in facet is tied to the kinds of every served metric listing it, the kinds the engine accepts it
         * for, not guessed from its name: each facet of a metric is offered for the metric's kinds (OBS-193, second
         * PR). A dynamic facet keeps its namespace's kinds.
         */
        @Test
        void should_tie_a_built_in_dimension_to_the_kinds_of_every_served_metric_listing_it() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API", "HTTP_STATUS");
            givenBuiltIn("LLM_PROMPT_TOTAL_TOKEN", List.of(ApiType.LLM), "API", "LLM_PROXY_MODEL");
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE), "API", "NATIVE_TOPIC");
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("bool_pii_detected", AdditionalMetricValueType.BOOL),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.LLM, ApiType.NATIVE));

            var kindsByDimension = definition
                .dimensions()
                .stream()
                .collect(Collectors.toMap(DimensionDefinition::name, DimensionDefinition::apiTypes));
            assertThat(kindsByDimension.get("API")).containsExactlyInAnyOrder(
                ApiType.HTTP_PROXY,
                ApiType.LLM,
                ApiType.MESSAGE,
                ApiType.MCP,
                ApiType.A2A,
                ApiType.NATIVE
            );
            assertThat(kindsByDimension.get("HTTP_STATUS")).containsExactlyInAnyOrderElementsOf(HTTP_KINDS);
            assertThat(kindsByDimension.get("LLM_PROXY_MODEL")).containsExactly(ApiType.LLM);
            assertThat(kindsByDimension.get("NATIVE_TOPIC")).containsExactly(ApiType.NATIVE);
            assertThat(kindsByDimension.get("additional-metrics.bool_pii_detected"))
                .as("a dynamic facet keeps its namespace's kinds")
                .containsExactly(ApiType.LLM);
            for (var kind : Set.of(ApiType.LLM, ApiType.NATIVE)) {
                for (var family : definition.families()) {
                    for (var metric : family.metrics()) {
                        if (metric.apiTypes().contains(kind)) {
                            for (var facet : metric.facets()) {
                                assertThat(kindsByDimension.get(facet.name()))
                                    .as(kind + " " + metric.name() + " by " + facet.name())
                                    .contains(kind);
                            }
                        }
                    }
                }
            }
        }

        /** The row tooltip reads the description and the last sighting of a dynamic entry as the filter catalog serves them. */
        @Test
        void should_serve_the_description_and_last_seen_of_dynamic_entries_to_a_traffic_reader_only() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.givenDescribed(PROTOCOL_VERSION, "Protocol version", "MCP protocol version negotiated");
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("keyword_mcp-proxy_endpoint-protocol-version", AdditionalMetricValueType.KEYWORD),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.MCP));

            assertThat(dimension(definition, PROTOCOL_VERSION)).satisfies(protocol -> {
                assertThat(protocol.description()).isEqualTo("MCP protocol version negotiated");
                assertThat(protocol.lastSeen()).isEqualTo(SEEN);
            });
            assertThat(dimension(definition, "API")).satisfies(api -> {
                assertThat(api.description()).isNull();
                assertThat(api.lastSeen()).isNull();
            });
            assertThat(additionalMetrics(definition))
                .singleElement()
                .satisfies(failover -> assertThat(failover.lastSeen()).isEqualTo(SEEN));

            fixture.ports.givenReadsTraffic(false);
            var blind = execute(Set.of(ApiType.MCP));

            assertThat(dimension(blind, PROTOCOL_VERSION).lastSeen()).as("traffic data, to a traffic reader only").isNull();
            assertThat(dimension(blind, PROTOCOL_VERSION).description()).isEqualTo("MCP protocol version negotiated");
        }

        private static DimensionDefinition dimension(AnalyticsDefinition definition, String name) {
            return definition
                .dimensions()
                .stream()
                .filter(dimension -> dimension.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No dimension " + name));
        }

        /** A facet of one kind goes with the metrics of that kind, and with those tied to none. */
        @Test
        void should_group_an_additional_metric_by_the_facets_of_its_kinds_only() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("keyword_llm-proxy_model-family", AdditionalMetricValueType.KEYWORD),
                    discovered("keyword_mcp-proxy_endpoint-protocol-version", AdditionalMetricValueType.KEYWORD),
                    discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD),
                    discovered("long_mcp-proxy_tool-calls", AdditionalMetricValueType.LONG),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG),
                    discovered("double_risk-score", AdditionalMetricValueType.DOUBLE)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.LLM, ApiType.MCP));

            var facetsOf = facetNamesByMetric(definition);
            assertThat(facetsOf.get("additional-metrics.long_mcp-proxy_tool-calls")).containsExactlyInAnyOrder(
                "API",
                "HTTP_STATUS",
                PROTOCOL_VERSION,
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(facetsOf.get(FAILOVER)).containsExactlyInAnyOrder(
                "API",
                "HTTP_STATUS",
                "additional-metrics.keyword_llm-proxy_model-family",
                PROTOCOL_VERSION,
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(facetsOf.get("additional-metrics.double_risk-score")).isEqualTo(facetsOf.get(FAILOVER));
            assertThat(facetsOf.get(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF)).containsExactlyInAnyOrder(
                "API",
                "additional-metrics.keyword_llm-proxy_model-family",
                PROTOCOL_VERSION,
                "additional-metrics.keyword_fraud-check_tier"
            );
        }

        /** The built-in takes the facets of its own kinds, like an additional metric: a request mixing kinds adds none of another. */
        @Test
        void should_group_the_built_in_by_the_facets_of_its_kinds_only_when_the_request_mixes_kinds() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE), "API");
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("keyword_native-kafka_topic", AdditionalMetricValueType.KEYWORD),
                    discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG)
                ),
                false
            );

            var definition = execute(Set.of(ApiType.HTTP_PROXY, ApiType.NATIVE));

            var facetsOf = facetNamesByMetric(definition);
            assertThat(facetsOf.get(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF)).containsExactlyInAnyOrder(
                "API",
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(facetsOf.get("NATIVE_CONNECTIONS")).containsExactly("API");
            assertThat(facetsOf.get(FAILOVER)).containsExactlyInAnyOrder(
                "API",
                "HTTP_STATUS",
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(definition.dimensions())
                .as("a facet no listed metric is grouped by is no dimension")
                .extracting(DimensionDefinition::name)
                .doesNotContain("additional-metrics.keyword_native-kafka_topic");
        }

        @Test
        void should_list_them_for_the_kinds_their_namespace_ties_them_to() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.givenDeclared(PROTOCOL_VERSION);
            fixture.ports.givenDiscovered(List.of(discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)), false);

            assertThat(execute(Set.of(ApiType.HTTP_PROXY)).dimensions())
                .extracting(DimensionDefinition::name)
                .containsExactly("API", "additional-metrics.keyword_fraud-check_tier");
            assertThat(execute(Set.of(ApiType.MCP)).dimensions())
                .extracting(DimensionDefinition::name)
                .containsExactly("API", PROTOCOL_VERSION, "additional-metrics.keyword_fraud-check_tier");
        }

        @Test
        void should_leave_out_a_record_that_is_not_exposed_and_a_reserved_key() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.givenDeclared(ENVIRONMENT, PROTOCOL_VERSION, null, null, false);
            fixture.ports.givenDiscovered(List.of(discovered("keyword_custom_tier", AdditionalMetricValueType.KEYWORD)), false);

            assertThat(execute(Set.of(ApiType.MCP)).dimensions()).extracting(DimensionDefinition::name).containsExactly("API");
        }
    }

    @Nested
    class Origins {

        @Test
        void should_keep_the_metrics_and_dimensions_of_the_asked_origins_only_once_the_definition_is_built() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.givenDeclared(BASKET);
            fixture.ports.givenDiscovered(
                List.of(
                    discovered("long_failover_count", AdditionalMetricValueType.LONG),
                    discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)
                ),
                true
            );

            var discoveredOnly = useCase.execute(
                new GetObservabilityAnalyticsDefinitionUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(ApiType.HTTP_PROXY),
                    Set.of(CatalogOrigin.DISCOVERED)
                )
            );
            var builtInOnly = useCase.execute(
                new GetObservabilityAnalyticsDefinitionUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(ApiType.HTTP_PROXY),
                    Set.of(CatalogOrigin.BUILT_IN)
                )
            );

            assertThat(discoveredOnly.definition().families())
                .extracting(AnalyticsDefinition.FamilyMetrics::family)
                .containsExactly(MetricFamily.ADDITIONAL);
            assertThat(metricNames(discoveredOnly.definition())).containsExactly(FAILOVER);
            assertThat(discoveredOnly.definition().dimensions())
                .extracting(DimensionDefinition::name)
                .containsExactly("additional-metrics.keyword_fraud-check_tier");
            assertThat(discoveredOnly.truncated()).as("the discovery pass is the same whatever origins are kept").isTrue();
            assertThat(metricNames(builtInOnly.definition())).containsExactly(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF);
            assertThat(builtInOnly.definition().dimensions())
                .as("the built-in facets, those of the dropped additional metric included")
                .extracting(DimensionDefinition::name)
                .containsExactly("API", "HTTP_STATUS");
            assertThat(builtInOnly.definition().families().getFirst().metrics().getFirst().facets())
                .as("a kept metric still names every facet it is grouped by")
                .extracting(DimensionDefinition::name)
                .containsExactly("API", "additional-metrics.keyword_fraud-check_tier");
        }

        /** The facet rides on a built-in metric, dropped by the narrowing: the dimension is an entry of its own. */
        @Test
        void should_keep_a_discovered_dimension_whose_only_carrier_is_a_built_in_metric() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.ports.givenDiscovered(List.of(discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)), false);

            var definition = useCase
                .execute(
                    new GetObservabilityAnalyticsDefinitionUseCase.Input(
                        ORGANIZATION,
                        ENVIRONMENT,
                        Set.of(ApiType.HTTP_PROXY),
                        Set.of(CatalogOrigin.DISCOVERED)
                    )
                )
                .definition();

            assertThat(definition.families()).isEmpty();
            assertThat(definition.dimensions())
                .extracting(DimensionDefinition::name)
                .containsExactly("additional-metrics.keyword_fraud-check_tier");
        }

        /** The kinds of a built-in dimension come from the whole definition: narrowing to some origins keeps them. */
        @Test
        void should_keep_the_kinds_of_a_built_in_dimension_whatever_origins_are_asked() {
            givenBuiltIn(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF, HTTP_KINDS, "API");
            fixture.ports.givenDiscovered(List.of(discovered("double_risk-score", AdditionalMetricValueType.DOUBLE)), false);

            var all = definitionNarrowedTo(Set.of());
            var builtInOnly = definitionNarrowedTo(Set.of(CatalogOrigin.BUILT_IN));

            assertThat(metricNames(builtInOnly)).containsExactly(InMemoryAnalyticsDefinitionPort.DIMENSIONS_OF);
            assertThat(kindsOf(builtInOnly, "HTTP_STATUS"))
                .as("listed by the dropped additional metric only, measured for Edge too")
                .contains(ApiType.EDGE)
                .isEqualTo(kindsOf(all, "HTTP_STATUS"));
            assertThat(kindsOf(builtInOnly, "API")).isEqualTo(kindsOf(all, "API"));
        }

        private AnalyticsDefinition definitionNarrowedTo(Set<CatalogOrigin> origins) {
            return useCase
                .execute(
                    new GetObservabilityAnalyticsDefinitionUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(ApiType.HTTP_PROXY), origins)
                )
                .definition();
        }

        private static Set<ApiType> kindsOf(AnalyticsDefinition definition, String dimension) {
            return definition
                .dimensions()
                .stream()
                .filter(candidate -> candidate.name().equals(dimension))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No dimension " + dimension))
                .apiTypes();
        }

        @Test
        void should_narrow_a_definition_of_kinds_the_engine_measures_no_additional_metric_for_as_well() {
            givenBuiltIn("NATIVE_CONNECTIONS", List.of(ApiType.NATIVE));

            var definition = useCase
                .execute(
                    new GetObservabilityAnalyticsDefinitionUseCase.Input(
                        ORGANIZATION,
                        ENVIRONMENT,
                        Set.of(ApiType.NATIVE),
                        Set.of(CatalogOrigin.DECLARED)
                    )
                )
                .definition();

            assertThat(definition.families()).isEmpty();
            assertThat(definition.dimensions()).isEmpty();
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

    /** The names of the facets each metric of the definition is grouped by, by metric name. */
    private static Map<String, List<String>> facetNamesByMetric(AnalyticsDefinition definition) {
        return definition
            .families()
            .stream()
            .flatMap(family -> family.metrics().stream())
            .collect(Collectors.toMap(MetricDefinition::name, metric -> metric.facets().stream().map(DimensionDefinition::name).toList()));
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
