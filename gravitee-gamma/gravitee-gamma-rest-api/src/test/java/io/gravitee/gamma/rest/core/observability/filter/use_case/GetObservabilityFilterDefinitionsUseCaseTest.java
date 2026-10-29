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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityFilterDefinitionsUseCaseTest {

    private static final String ENVIRONMENT = MetricDefinitionFixture.ENVIRONMENT;

    private final MetricDefinitionFixture metricDefinitions = new MetricDefinitionFixture();

    // Real registry → exercises the actual unified vocabulary shipped with the host catalog.
    private final GetObservabilityFilterDefinitionsUseCase useCase = new GetObservabilityFilterDefinitionsUseCase(
        new SpiFilterRegistry(),
        metricDefinitions.repository,
        metricDefinitions.ports,
        metricDefinitions.catalog
    );

    @Test
    void should_return_the_full_catalog_when_both_axes_are_empty() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of()));

        assertThat(output.filters()).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "API_TYPE", "HTTP_GATEWAY_RESPONSE_TIME", "MCP_PROXY_METHOD", "URI", "HTTP_PATH")
            .doesNotContain("HTTP_PATH_MAPPING");
    }

    @Test
    void should_narrow_to_logs_served_filters_when_signal_is_logs() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(Signal.LOGS), Set.of()));

        // Every returned spec must declare LOGS; analytics-only filters (e.g. GATEWAY) drop out.
        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.signals()).contains(Signal.LOGS));
        assertThat(output.filters()).extracting(FilterSpec::name).contains("API", "URI").doesNotContain("GATEWAY", "HOST");
    }

    @Test
    void should_narrow_to_llm_relevant_filters_when_api_type_is_llm() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of(ApiType.LLM)));

        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.apiTypes()).contains(ApiType.LLM));
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "LLM_PROXY_MODEL")
            .doesNotContain("MESSAGE_SIZE", "EDGE_TYPE");
    }

    @Test
    void should_intersect_both_axes_independently() {
        var output = useCase.execute(
            new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(Signal.ANALYTICS), Set.of(ApiType.MESSAGE))
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
            new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(Signal.LOGS), Set.of(ApiType.MESSAGE))
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

            var filters = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of())).filters();

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
                    new CatalogFlags(CatalogOrigin.DECLARED, "double_checkout_basket-value", AdditionalMetricValueType.DOUBLE, null, true)
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

            var filters = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of())).filters();

            assertThat(filters).extracting(FilterSpec::name).doesNotContain("additional-metrics.double_checkout_basket-value");
        }

        @Test
        void should_leave_out_the_detail_only_value_types() {
            metricDefinitions.givenDeclared("additional-metrics.json_fraud-signals");
            metricDefinitions.givenDeclared("additional-metrics.string_note");

            var filters = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of())).filters();

            assertThat(filters).noneMatch(spec -> spec.catalog() != null);
        }

        @Test
        void should_leave_out_a_record_a_built_in_now_reads() {
            metricDefinitions.givenDeclared("additional-metrics.keyword_fraud_verdict");
            metricDefinitions.ports.givenBuiltIn("keyword_fraud_verdict", "FRAUD_VERDICT");

            var filters = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, Set.of(), Set.of())).filters();

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
                .execute(new GetObservabilityFilterDefinitionsUseCase.Input(ENVIRONMENT, signals, apiTypes))
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
}
