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
package io.gravitee.gamma.rest.core.observability.filter.use_case;

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterContributor;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityFilterDefinitionsUseCaseTest {

    private static final String ORGANIZATION = MetricDefinitionFixture.ORGANIZATION;
    private static final String ENVIRONMENT = MetricDefinitionFixture.ENVIRONMENT;
    private static final Set<ApiType> HTTP_FAMILY = Set.of(ApiType.HTTP_PROXY, ApiType.LLM, ApiType.MESSAGE, ApiType.MCP, ApiType.A2A);
    private static final Set<ApiType> AI_KINDS = Set.of(ApiType.LLM, ApiType.MCP, ApiType.A2A);

    private final MetricDefinitionFixture metricDefinitions = new MetricDefinitionFixture();

    // Real registry → exercises the actual unified vocabulary shipped with the host catalog.
    private final GetObservabilityFilterDefinitionsUseCase useCase = new GetObservabilityFilterDefinitionsUseCase(
        new SpiFilterRegistry(),
        metricDefinitions.repository,
        metricDefinitions.ports,
        metricDefinitions.ports,
        metricDefinitions.ports,
        metricDefinitions.ports,
        metricDefinitions.catalog
    );

    @Test
    void should_return_the_full_catalog_when_both_axes_are_empty() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()));

        assertThat(output.filters()).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "API_TYPE", "HTTP_GATEWAY_RESPONSE_TIME", "MCP_PROXY_METHOD", "URI", "HTTP_PATH")
            .doesNotContain("HTTP_PATH_MAPPING");
    }

    @Test
    void should_narrow_to_logs_served_filters_when_signal_is_logs() {
        var output = useCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(Signal.LOGS), Set.of())
        );

        // Every returned spec must declare LOGS; analytics-only filters (e.g. GATEWAY) drop out.
        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.signals()).contains(Signal.LOGS));
        assertThat(output.filters()).extracting(FilterSpec::name).contains("API", "URI").doesNotContain("GATEWAY", "HOST");
    }

    @Test
    void should_narrow_to_llm_relevant_filters_when_api_type_is_llm() {
        var output = useCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of(ApiType.LLM))
        );

        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.apiTypes()).contains(ApiType.LLM));
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "LLM_PROXY_MODEL")
            .doesNotContain("MESSAGE_SIZE", "EDGE_TYPE");
    }

    @Test
    void should_intersect_both_axes_independently() {
        var output = useCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(Signal.ANALYTICS), Set.of(ApiType.MESSAGE))
        );

        List<String> names = output.filters().stream().map(FilterSpec::name).toList();
        assertThat(names).contains("MESSAGE_OPERATION_TYPE", "MESSAGE_SIZE", "API_TYPE");
        // One exclusion per axis, so the test keeps covering both: GEO_IP_COUNTRY is HTTP/LLM/MCP/A2A
        // only → excluded by api type; REQUEST_ID is LOGS-only → excluded by signal. HOST and
        // HTTP_GATEWAY_LATENCY no longer serve as the api-type witness: they read connection-document
        // fields and are now offered for MESSAGE like the rest.
        assertThat(names).doesNotContain("GEO_IP_COUNTRY", "REQUEST_ID");
    }

    /**
     * A Message API's log row is an HTTP connection document, so the filters reading its fields must
     * be offered. Measured before this was wired: the catalog answered six filters for MESSAGE+LOGS
     * (API, APPLICATION, PLAN, ENTRYPOINT, API_TYPE, RECORD_TYPE), leaving the screen with nothing
     * useful to narrow on.
     */
    @Test
    void should_serve_the_connection_document_filters_for_message_logs() {
        var output = useCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(Signal.LOGS), Set.of(ApiType.MESSAGE))
        );

        List<String> names = output.filters().stream().map(FilterSpec::name).toList();
        assertThat(names).contains(
            "API",
            "APPLICATION",
            "PLAN",
            "ENTRYPOINT",
            "HTTP_METHOD",
            "HTTP_STATUS",
            "HTTP_STATUS_CODE_GROUP",
            "URI",
            "HTTP_GATEWAY_RESPONSE_TIME",
            "ERROR_KEY",
            "REQUEST_ID",
            "TRANSACTION_ID"
        );
        // A Message API records no connection-level body, so a payload search would always be empty.
        assertThat(names).doesNotContain("PAYLOAD");
    }

    @Nested
    class DeclaredEntries {

        @Test
        void should_follow_the_built_in_specs_with_the_exposed_records_of_the_environment() {
            metricDefinitions.givenDeclared(ENVIRONMENT, "additional-metrics.double_checkout_basket-value", "Basket value", "€", true);
            metricDefinitions.givenDeclared(ENVIRONMENT, "additional-metrics.keyword_fraud_verdict", null, null, true);
            metricDefinitions.givenDeclared(ENVIRONMENT, "additional-metrics.bool_cache_hit", null, null, true);

            var filters = useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();

            assertThat(filters.subList(0, StaticFilters.values().length + ExtensibleFilters.values().length)).allSatisfy(spec ->
                assertThat(spec.catalog()).isNull()
            );
            assertThat(declared(filters, "additional-metrics.double_checkout_basket-value")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Basket value");
                assertThat(spec.type()).isEqualTo(FilterType.NUMBER);
                assertThat(spec.operators()).containsExactly(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE);
                assertThat(spec.signals()).containsExactlyInAnyOrder(Signal.LOGS, Signal.ANALYTICS);
                assertThat(spec.apiTypes()).isEmpty();
                assertThat(spec.takesDecimals()).isTrue();
                assertThat(spec.catalog()).isEqualTo(
                    new CatalogFlags(
                        CatalogOrigin.DECLARED,
                        "double_checkout_basket-value",
                        AdditionalMetricValueType.DOUBLE,
                        null,
                        true,
                        null
                    )
                );
            });
            assertThat(declared(filters, "additional-metrics.keyword_fraud_verdict")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Fraud verdict");
                assertThat(spec.type()).isEqualTo(FilterType.KEYWORD);
                assertThat(spec.operators()).containsExactly(FilterOperator.EQ, FilterOperator.IN);
            });
            assertThat(declared(filters, "additional-metrics.bool_cache_hit").operators()).containsExactly(FilterOperator.EQ);
        }

        @Test
        void should_leave_out_a_record_that_is_not_exposed() {
            metricDefinitions.givenDeclared(ENVIRONMENT, "additional-metrics.double_checkout_basket-value", "Basket value", null, false);

            var filters = useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();

            assertThat(filters).extracting(FilterSpec::name).doesNotContain("additional-metrics.double_checkout_basket-value");
        }

        @Test
        void should_leave_out_the_detail_only_value_types() {
            metricDefinitions.givenDeclared("additional-metrics.json_fraud-signals");
            metricDefinitions.givenDeclared("additional-metrics.string_note");

            var filters = useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();

            assertThat(filters).noneMatch(spec -> spec.catalog() != null);
        }

        @Test
        void should_leave_out_a_record_a_built_in_now_reads() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");
            metricDefinitions.ports.givenBuiltIn("keyword_fraud_verdict", "FRAUD_VERDICT");

            var filters = useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();

            assertThat(filters).noneMatch(spec -> spec.catalog() != null);
        }

        @Test
        void should_keep_them_for_any_api_kind_and_drop_them_for_the_decisions_screen_or_the_traces_alone() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");

            assertThat(declaredNames(Set.of(), Set.of(ApiType.LLM))).containsExactly("additional-metrics.keyword_fraud_verdict");
            assertThat(declaredNames(Set.of(Signal.ANALYTICS), Set.of(ApiType.AUTHZ_DECISION, ApiType.MCP))).hasSize(1);
            assertThat(declaredNames(Set.of(), Set.of(ApiType.AUTHZ_DECISION))).isEmpty();
            assertThat(declaredNames(Set.of(Signal.TRACES), Set.of())).isEmpty();
        }

        @Test
        void should_serve_the_records_of_the_requested_environment_only() {
            metricDefinitions.givenDeclared(
                MetricDefinitionFixture.OTHER_ENVIRONMENT,
                "additional-metrics.keyword_fraud_verdict",
                null,
                null,
                true
            );

            assertThat(declaredNames(Set.of(), Set.of())).isEmpty();
        }

        private List<String> declaredNames(Set<Signal> signals, Set<ApiType> apiTypes) {
            return useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, signals, apiTypes))
                .filters()
                .stream()
                .filter(spec -> spec.catalog() != null)
                .map(FilterSpec::name)
                .toList();
        }

        private FilterSpec declared(List<FilterSpec> filters, String name) {
            return filters
                .stream()
                .filter(spec -> spec.name().equals(name))
                .findFirst()
                .orElseThrow();
        }
    }

    /** A dynamic entry is scoped and named by the namespace of its key (OBS-193). */
    @Nested
    class NamespaceScoping {

        private static final Instant SEEN = Instant.parse("2026-11-09T08:05:00Z");

        @Test
        void should_tie_a_dynamic_entry_to_the_kinds_of_its_namespace_and_label_it_without_the_namespace() {
            metricDefinitions.givenDeclared("additional-metrics.long_llm-proxy_tokens-cached");
            metricDefinitions.ports.givenDiscovered(
                List.of(discovered("keyword_mcp-proxy_endpoint-protocol-version", AdditionalMetricValueType.KEYWORD)),
                false
            );

            var filters = execute(Set.of(), Set.of());

            assertThat(spec(filters, "additional-metrics.long_llm-proxy_tokens-cached")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Tokens cached");
                assertThat(spec.apiTypes()).containsExactly(ApiType.LLM);
                assertThat(spec.catalog().namespace()).isEqualTo("llm-proxy");
                assertThat(spec.description()).isNull();
            });
            assertThat(spec(filters, "additional-metrics.keyword_mcp-proxy_endpoint-protocol-version")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Endpoint protocol version");
                assertThat(spec.apiTypes()).containsExactly(ApiType.MCP);
                assertThat(spec.catalog().namespace()).isEqualTo("mcp-proxy");
            });
        }

        @Test
        void should_serve_no_namespace_and_no_kind_for_a_namespace_the_map_does_not_know() {
            metricDefinitions.ports.givenDiscovered(
                List.of(discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)),
                false
            );

            assertThat(spec(execute(Set.of(), Set.of()), "additional-metrics.keyword_fraud-check_tier")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Fraud check tier");
                assertThat(spec.apiTypes()).isEmpty();
                assertThat(spec.catalog().namespace()).isNull();
            });
        }

        @Test
        void should_drop_the_native_kafka_keys_and_keep_the_failover_keys_for_the_ai_kinds() {
            metricDefinitions.ports.givenDiscovered(
                List.of(
                    discovered("long_native-kafka_messages-in", AdditionalMetricValueType.LONG),
                    discovered("long_failover_count", AdditionalMetricValueType.LONG),
                    discovered("keyword_fraud-check_tier", AdditionalMetricValueType.KEYWORD)
                ),
                false
            );

            assertThat(dynamicNames(Set.of(), AI_KINDS)).containsExactlyInAnyOrder(
                "additional-metrics.long_failover_count",
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(dynamicNames(Set.of(), Set.of(ApiType.NATIVE))).containsExactlyInAnyOrder(
                "additional-metrics.long_native-kafka_messages-in",
                "additional-metrics.keyword_fraud-check_tier"
            );
            assertThat(dynamicNames(Set.of(), Set.of())).hasSize(3);
        }

        @Test
        void should_let_a_declared_label_win_over_the_derived_one_and_serve_the_record_description() {
            metricDefinitions.givenDescribed(
                "additional-metrics.long_llm-proxy_tokens-cached",
                "Cached tokens",
                "Tokens served from the cache"
            );
            metricDefinitions.givenDescribed("additional-metrics.keyword_fraud-check_tier", "Risk tier", null);

            var filters = execute(Set.of(), Set.of());

            assertThat(spec(filters, "additional-metrics.long_llm-proxy_tokens-cached")).satisfies(spec -> {
                assertThat(spec.label()).isEqualTo("Cached tokens");
                assertThat(spec.description()).isEqualTo("Tokens served from the cache");
            });
            assertThat(spec(filters, "additional-metrics.keyword_fraud-check_tier").label()).isEqualTo("Risk tier");
            assertThat(spec(filters, "API").description()).isNull();
        }

        @Test
        void should_keep_the_entries_of_the_asked_origins_only_once_the_catalog_is_built() {
            metricDefinitions.givenDeclared("additional-metrics.long_llm-proxy_tokens-cached");
            metricDefinitions.ports
                .givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL")
                .givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

            var discoveredOnly = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(),
                    Set.of(),
                    Set.of(CatalogOrigin.DISCOVERED)
                )
            );
            var builtInOnly = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(),
                    Set.of(),
                    Set.of(CatalogOrigin.BUILT_IN)
                )
            );
            var declaredAndDiscovered = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(),
                    Set.of(),
                    Set.of(CatalogOrigin.DECLARED, CatalogOrigin.DISCOVERED)
                )
            );

            assertThat(discoveredOnly.filters()).extracting(FilterSpec::name).containsExactly("additional-metrics.long_failover_count");
            assertThat(discoveredOnly.truncated()).as("the discovery pass is the same whatever origins are kept").isTrue();
            assertThat(builtInOnly.filters()).allSatisfy(spec -> assertThat(spec.origin()).isEqualTo(CatalogOrigin.BUILT_IN));
            assertThat(builtInOnly.filters()).extracting(FilterSpec::name).contains("API", "MCP_PROXY_TOOL");
            assertThat(builtInOnly.truncated()).isTrue();
            assertThat(declaredAndDiscovered.filters())
                .extracting(FilterSpec::name)
                .containsExactly("additional-metrics.long_llm-proxy_tokens-cached", "additional-metrics.long_failover_count");
        }

        private DiscoveredAdditionalMetric discovered(String rawKey, AdditionalMetricValueType valueType) {
            return new DiscoveredAdditionalMetric(rawKey, valueType, SEEN);
        }

        private List<FilterSpec> execute(Set<Signal> signals, Set<ApiType> apiTypes) {
            return useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, signals, apiTypes))
                .filters();
        }

        private List<String> dynamicNames(Set<Signal> signals, Set<ApiType> apiTypes) {
            return execute(signals, apiTypes)
                .stream()
                .filter(spec -> spec.catalog() != null && spec.catalog().origin() != CatalogOrigin.BUILT_IN)
                .map(FilterSpec::name)
                .toList();
        }

        private FilterSpec spec(List<FilterSpec> filters, String name) {
            return filters
                .stream()
                .filter(spec -> spec.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry " + name));
        }
    }

    /**
     * A numeric key a built-in metric reads is filterable under its dynamic name, the engine answering on it, so the
     * log detail and the filter bar can name and narrow on the tokens and costs (OBS-193, second PR).
     */
    @Nested
    class BuiltInKeys {

        private static final Instant SEEN = Instant.parse("2026-11-10T08:05:00Z");
        private static final String TOKENS_SENT = "additional-metrics.long_llm-proxy_tokens-sent";

        @Test
        void should_list_a_numeric_key_a_built_in_metric_reads_and_no_built_in_filter_reads_under_its_dynamic_name() {
            givenBuiltInMetricReading("long_llm-proxy_tokens-sent", "LLM_PROMPT_TOKEN_SENT", List.of(ApiType.LLM));
            metricDefinitions.ports.givenDiscovered(
                List.of(discovered("long_llm-proxy_tokens-sent", AdditionalMetricValueType.LONG)),
                false
            );

            var spec = spec(execute(Set.of(), Set.of()), TOKENS_SENT);

            assertThat(spec.label()).as("the metric's label, one source").isEqualTo("LLM_PROMPT_TOKEN_SENT label");
            assertThat(spec.type()).isEqualTo(FilterType.NUMBER);
            assertThat(spec.operators()).containsExactly(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE);
            assertThat(spec.signals()).containsExactlyInAnyOrder(Signal.LOGS, Signal.ANALYTICS);
            assertThat(spec.apiTypes()).containsExactly(ApiType.LLM);
            assertThat(spec.description()).isNull();
            assertThat(spec.takesDecimals()).isFalse();
            assertThat(spec.catalog()).isEqualTo(
                new CatalogFlags(CatalogOrigin.BUILT_IN, "long_llm-proxy_tokens-sent", AdditionalMetricValueType.LONG, SEEN, true, null)
            );
        }

        @Test
        void should_take_decimals_on_a_double_key_and_serve_its_last_seen_to_a_traffic_reader_only() {
            givenBuiltInMetricReading("double_llm-proxy_sent-cost", "LLM_PROMPT_TOKEN_SENT_COST", List.of(ApiType.LLM));
            metricDefinitions.ports.givenDiscovered(
                List.of(discovered("double_llm-proxy_sent-cost", AdditionalMetricValueType.DOUBLE)),
                false
            );
            metricDefinitions.ports.givenReadsTraffic(false);

            var spec = spec(execute(Set.of(), Set.of()), "additional-metrics.double_llm-proxy_sent-cost");

            assertThat(spec.takesDecimals()).isTrue();
            assertThat(spec.origin()).isEqualTo(CatalogOrigin.BUILT_IN);
            assertThat(spec.catalog().lastSeen()).isNull();
        }

        /** The logs search serves no Edge row: an Edge key is a filter of the analytics signal only. */
        @Test
        void should_serve_an_edge_key_on_the_analytics_signal_only() {
            givenBuiltInMetricReading("long_edge_tokens_in", "EDGE_TOKENS_IN", List.of(ApiType.EDGE));

            assertThat(spec(execute(Set.of(), Set.of()), "additional-metrics.long_edge_tokens_in").signals()).containsExactly(
                Signal.ANALYTICS
            );
            assertThat(names(execute(Set.of(Signal.LOGS), Set.of()))).doesNotContain("additional-metrics.long_edge_tokens_in");
            assertThat(names(execute(Set.of(Signal.ANALYTICS), Set.of(ApiType.EDGE)))).contains("additional-metrics.long_edge_tokens_in");
        }

        @Test
        void should_serve_the_key_for_the_kinds_of_its_metric_only_and_never_twice_under_any_narrowing() {
            givenBuiltInMetricReading("long_llm-proxy_tokens-sent", "LLM_PROMPT_TOKEN_SENT", List.of(ApiType.LLM));
            metricDefinitions.ports.givenDiscovered(
                List.of(discovered("long_llm-proxy_tokens-sent", AdditionalMetricValueType.LONG)),
                false
            );

            assertThat(names(execute(Set.of(), Set.of(ApiType.LLM)))).containsOnlyOnce(TOKENS_SENT);
            assertThat(names(execute(Set.of(), Set.of(ApiType.MCP)))).doesNotContain(TOKENS_SENT);
            assertThat(names(execute(Set.of(), Set.of(ApiType.AUTHZ_DECISION)))).doesNotContain(TOKENS_SENT);
            for (Signal signal : Signal.values()) {
                for (ApiType kind : ApiType.values()) {
                    assertThat(names(execute(Set.of(signal), Set.of(kind)))).as(signal + " " + kind).doesNotHaveDuplicates();
                }
            }
        }

        /**
         * Numeric keys only: a keyword key lists its values nowhere, whether a facet or a metric reads it; a key whose
         * metric the definition does not serve has no label.
         */
        @Test
        void should_leave_out_a_keyword_key_whatever_built_in_reads_it_and_a_key_of_a_metric_the_definition_does_not_serve() {
            metricDefinitions.ports.givenBuiltIn("keyword_edge_model", "EDGE_MODEL");
            metricDefinitions.ports.givenBuiltInMetric(aMetric("EDGE_TOKENS_IN", "TOKENS", List.of(ApiType.EDGE), "EDGE_MODEL"));
            givenBuiltInMetricReading("keyword_edge_route", "EDGE_ROUTE", List.of(ApiType.EDGE));
            metricDefinitions.ports.givenBuiltIn("long_edge_tokens_out", "EDGE_TOKENS_OUT");

            assertThat(names(execute(Set.of(), Set.of()))).doesNotContain(
                "additional-metrics.keyword_edge_model",
                "additional-metrics.keyword_edge_route",
                "additional-metrics.long_edge_tokens_out"
            );
        }

        /**
         * The registry the use case serves is narrowed by signal and kind: whether a built-in filter reads the key is
         * judged on the whole registry, or a filter narrowed out would let its key reappear under the dynamic name.
         */
        @Test
        void should_judge_whether_a_built_in_filter_reads_the_key_on_the_whole_registry_not_on_the_narrowed_one() {
            var tokensSentFilter = new FilterSpec(
                "TOKENS_SENT",
                "Sent token count",
                FilterType.NUMBER,
                List.of(FilterOperator.GTE),
                null,
                null,
                Set.of(Signal.ANALYTICS),
                Set.of(ApiType.LLM)
            );
            var contributed = new GetObservabilityFilterDefinitionsUseCase(
                new SpiFilterRegistry(
                    List.of(
                        new FilterContributor() {
                            @Override
                            public List<FilterSpec> filters() {
                                return List.of(tokensSentFilter);
                            }
                        }
                    )
                ),
                metricDefinitions.repository,
                metricDefinitions.ports,
                metricDefinitions.ports,
                metricDefinitions.ports,
                metricDefinitions.ports,
                metricDefinitions.catalog
            );
            metricDefinitions.ports.givenBuiltIn("long_llm-proxy_tokens-sent", "TOKENS_SENT", "LLM_PROMPT_TOKEN_SENT");
            metricDefinitions.ports.givenBuiltInMetric(aMetric("LLM_PROMPT_TOKEN_SENT", "TOKENS", List.of(ApiType.LLM)));

            var logsOnly = contributed
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(Signal.LOGS), Set.of()))
                .filters();
            var all = contributed
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();

            assertThat(names(logsOnly))
                .as("the analytics-only filter is narrowed out; its key stays read")
                .doesNotContain("TOKENS_SENT", TOKENS_SENT);
            assertThat(names(all)).contains("TOKENS_SENT").doesNotContain(TOKENS_SENT);
        }

        private void givenBuiltInMetricReading(String rawKey, String metric, List<ApiType> kinds) {
            metricDefinitions.ports.givenBuiltIn(rawKey, metric);
            metricDefinitions.ports.givenBuiltInMetric(aMetric(metric, "TOKENS", kinds));
        }

        private DiscoveredAdditionalMetric discovered(String rawKey, AdditionalMetricValueType valueType) {
            return new DiscoveredAdditionalMetric(rawKey, valueType, SEEN);
        }

        private List<FilterSpec> execute(Set<Signal> signals, Set<ApiType> apiTypes) {
            return useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, signals, apiTypes))
                .filters();
        }

        private static List<String> names(List<FilterSpec> filters) {
            return filters.stream().map(FilterSpec::name).toList();
        }

        private FilterSpec spec(List<FilterSpec> filters, String name) {
            return filters
                .stream()
                .filter(spec -> spec.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry " + name));
        }
    }

    @Nested
    class DiscoveredEntries {

        private static final Instant SEEN = Instant.parse("2026-10-29T08:05:00Z");

        @Test
        void should_list_a_discovered_key_once_as_discovered_and_a_built_in_key_once_as_built_in() {
            metricDefinitions.ports
                .givenBuiltIn("keyword_llm-proxy_model", "LLM_PROXY_MODEL")
                .givenBuiltIn("keyword_native-kafka_client-software-version", "NATIVE_CLIENT_SOFTWARE_VERSION")
                .givenBuiltIn("keyword_authz_subject-type")
                .givenDiscovered(
                    List.of(
                        discovered("long_failover_count", AdditionalMetricValueType.LONG),
                        discovered("keyword_llm-proxy_model", AdditionalMetricValueType.KEYWORD),
                        discovered("keyword_native-kafka_client-software-version", AdditionalMetricValueType.KEYWORD),
                        discovered("keyword_authz_subject-type", AdditionalMetricValueType.KEYWORD)
                    ),
                    false
                );

            var output = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(
                    ORGANIZATION,
                    ENVIRONMENT,
                    Set.of(Signal.ANALYTICS),
                    Set.of(ApiType.HTTP_PROXY)
                )
            );

            assertThat(dynamic(output.filters())).containsExactly(
                new FilterSpec(
                    "additional-metrics.long_failover_count",
                    "Failover count",
                    FilterType.NUMBER,
                    List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                    null,
                    null,
                    Set.of(Signal.LOGS, Signal.ANALYTICS),
                    HTTP_FAMILY,
                    new CatalogFlags(
                        CatalogOrigin.DISCOVERED,
                        "long_failover_count",
                        AdditionalMetricValueType.LONG,
                        SEEN,
                        true,
                        "failover"
                    )
                )
            );
            assertThat(output.truncated()).isFalse();
        }

        @Test
        void should_flag_a_built_in_reading_an_additional_metric_with_its_key_and_last_seen() {
            metricDefinitions.ports
                .givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL")
                .givenDiscovered(List.of(discovered("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD)), false);

            var tool = spec(all(), "MCP_PROXY_TOOL");

            assertThat(tool.catalog()).isEqualTo(
                new CatalogFlags(
                    CatalogOrigin.BUILT_IN,
                    "keyword_mcp-proxy_tools/call",
                    AdditionalMetricValueType.KEYWORD,
                    SEEN,
                    true,
                    null
                )
            );
            assertThat(tool.label()).isEqualTo(StaticFilters.MCP_PROXY_TOOL.toSpec().label());
            assertThat(spec(all(), "API").catalog()).isNull();
        }

        @Test
        void should_serve_neither_discovered_entries_nor_last_seen_to_a_caller_who_does_not_read_the_traffic() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");
            metricDefinitions.ports
                .givenReadsTraffic(false)
                .givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL")
                .givenDiscovered(
                    List.of(
                        discovered("long_failover_count", AdditionalMetricValueType.LONG),
                        discovered("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD),
                        discovered("keyword_fraud_verdict", AdditionalMetricValueType.KEYWORD)
                    ),
                    true
                );

            var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()));

            assertThat(dynamic(output.filters()))
                .extracting(spec -> spec.catalog().origin())
                .containsExactly(CatalogOrigin.DECLARED);
            assertThat(output.filters())
                .filteredOn(spec -> spec.catalog() != null)
                .allSatisfy(spec -> assertThat(spec.catalog().lastSeen()).isNull());
            assertThat(spec(output.filters(), "MCP_PROXY_TOOL").catalog().rawKey()).isEqualTo("keyword_mcp-proxy_tools/call");
            assertThat(output.truncated()).isFalse();
            assertThat(metricDefinitions.ports.discoveries()).isZero();
        }

        @Test
        void should_serve_the_last_seen_of_a_declared_key_and_never_list_it_twice() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");
            metricDefinitions.ports.givenDiscovered(List.of(discovered("keyword_fraud_verdict", AdditionalMetricValueType.KEYWORD)), false);

            assertThat(dynamic(all()))
                .singleElement()
                .satisfies(spec -> {
                    assertThat(spec.catalog().origin()).isEqualTo(CatalogOrigin.DECLARED);
                    assertThat(spec.catalog().lastSeen()).isEqualTo(SEEN);
                });
        }

        @Test
        void should_leave_out_detail_only_and_reserved_keys() {
            metricDefinitions.ports.givenDiscovered(
                List.of(
                    discovered("json_fraud-signals", AdditionalMetricValueType.JSON),
                    discovered("string_note", AdditionalMetricValueType.STRING),
                    discovered("double_custom_basket-value", AdditionalMetricValueType.DOUBLE)
                ),
                false
            );

            assertThat(dynamic(all())).isEmpty();
        }

        @Test
        void should_keep_a_key_of_no_known_namespace_for_any_api_kind_and_drop_it_for_the_decisions_screen_alone() {
            metricDefinitions.ports.givenDiscovered(List.of(discovered("long_fraud-check_score", AdditionalMetricValueType.LONG)), false);

            assertThat(dynamicNames(Set.of(ApiType.NATIVE))).containsExactly("additional-metrics.long_fraud-check_score");
            assertThat(dynamicNames(Set.of(ApiType.AUTHZ_DECISION, ApiType.MCP))).hasSize(1);
            assertThat(dynamicNames(Set.of(ApiType.AUTHZ_DECISION))).isEmpty();
        }

        @Test
        void should_list_the_200_most_recently_seen_keys_and_say_others_were_left_out_never_counting_built_in_or_declared_ones() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");
            metricDefinitions.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
            List<DiscoveredAdditionalMetric> found = new ArrayList<>();
            IntStream.range(0, 201).forEach(i ->
                found.add(
                    new DiscoveredAdditionalMetric("long_key-%03d".formatted(i), AdditionalMetricValueType.LONG, SEEN.minusSeconds(i))
                )
            );
            found.add(new DiscoveredAdditionalMetric("keyword_fraud_verdict", AdditionalMetricValueType.KEYWORD, SEEN.minusSeconds(500)));
            found.add(
                new DiscoveredAdditionalMetric("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD, SEEN.minusSeconds(600))
            );
            metricDefinitions.ports.givenDiscovered(found, false);

            var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()));

            assertThat(dynamic(output.filters()))
                .filteredOn(spec -> spec.catalog().origin() == CatalogOrigin.DISCOVERED)
                .hasSize(200)
                .extracting(FilterSpec::name)
                .doesNotContain("additional-metrics.long_key-200");
            assertThat(spec(output.filters(), "additional-metrics.keyword_fraud_verdict").catalog().lastSeen()).isEqualTo(
                SEEN.minusSeconds(500)
            );
            assertThat(spec(output.filters(), "MCP_PROXY_TOOL").catalog().lastSeen()).isEqualTo(SEEN.minusSeconds(600));
            assertThat(output.truncated()).isTrue();
            assertThat(metricDefinitions.ports.lastUncountedKeys()).containsExactlyInAnyOrder(
                "keyword_fraud_verdict",
                "keyword_mcp-proxy_tools/call"
            );
        }

        @Test
        void should_list_nowhere_a_declared_key_that_is_not_exposed_even_when_documents_carry_it() {
            metricDefinitions.givenDeclared(ENVIRONMENT, "additional-metrics.keyword_fraud_verdict", null, null, false);
            metricDefinitions.ports.givenDiscovered(List.of(discovered("keyword_fraud_verdict", AdditionalMetricValueType.KEYWORD)), false);

            assertThat(dynamic(all())).isEmpty();
        }

        @Test
        void should_serve_the_static_catalog_flagged_truncated_to_a_reader_whose_discovery_is_unavailable() {
            metricDefinitions.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL").givenDiscovered(List.of(), true);

            var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()));

            assertThat(dynamic(output.filters())).isEmpty();
            assertThat(output.filters()).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
            assertThat(spec(output.filters(), "MCP_PROXY_TOOL").catalog()).isEqualTo(
                new CatalogFlags(
                    CatalogOrigin.BUILT_IN,
                    "keyword_mcp-proxy_tools/call",
                    AdditionalMetricValueType.KEYWORD,
                    null,
                    true,
                    null
                )
            );
            assertThat(output.truncated()).isTrue();
        }

        @Test
        void should_not_say_truncated_when_the_request_admits_no_discovered_entry() {
            metricDefinitions.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

            var decisions = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of(ApiType.AUTHZ_DECISION))
            );
            var traces = useCase.execute(
                new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(Signal.TRACES), Set.of())
            );

            assertThat(decisions.truncated()).isFalse();
            assertThat(traces.truncated()).isFalse();
        }

        @Test
        void should_not_say_truncated_to_a_caller_who_does_not_read_the_traffic() {
            metricDefinitions.ports.givenReadsTraffic(false).givenDiscovered(List.of(), true);

            var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()));

            assertThat(output.truncated()).isFalse();
        }

        private DiscoveredAdditionalMetric discovered(String rawKey, AdditionalMetricValueType valueType) {
            return new DiscoveredAdditionalMetric(rawKey, valueType, SEEN);
        }

        private List<FilterSpec> all() {
            return useCase
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), Set.of()))
                .filters();
        }

        private List<String> dynamicNames(Set<ApiType> apiTypes) {
            return dynamic(
                useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ORGANIZATION, ENVIRONMENT, Set.of(), apiTypes)).filters()
            )
                .stream()
                .map(FilterSpec::name)
                .toList();
        }

        /** The entries named by an additional metric field: declared and discovered ones. */
        private List<FilterSpec> dynamic(List<FilterSpec> filters) {
            return filters
                .stream()
                .filter(spec -> spec.catalog() != null && spec.catalog().origin() != CatalogOrigin.BUILT_IN)
                .toList();
        }

        private FilterSpec spec(List<FilterSpec> filters, String name) {
            return filters
                .stream()
                .filter(spec -> spec.name().equals(name))
                .findFirst()
                .orElseThrow();
        }
    }
}
