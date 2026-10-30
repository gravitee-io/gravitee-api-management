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
package io.gravitee.gamma.rest.core.observability.catalog.use_case;

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.ENVIRONMENT;
import static io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture.OTHER_ENVIRONMENT;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.MetricDefinitionFixture;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ListMetricDefinitionsUseCaseTest {

    private static final String BASKET = "additional-metrics.double_checkout_basket-value";
    private static final Instant SEEN = Instant.parse("2026-10-29T08:00:00Z");

    private MetricDefinitionFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new MetricDefinitionFixture();
    }

    @Test
    void should_list_a_key_declared_before_any_data_as_declared_without_last_seen_and_with_the_refresh_time() {
        fixture.givenDeclared(ENVIRONMENT, BASKET, "Basket value", "€", true);

        var catalog = fixture.listed();

        assertThat(entry(catalog.entries(), BASKET)).hasValueSatisfying(entry -> {
            assertThat(entry.origin()).isEqualTo(CatalogOrigin.DECLARED);
            assertThat(entry.label()).isEqualTo("Basket value");
            assertThat(entry.unit()).isEqualTo("€");
            assertThat(entry.lastSeen()).isNull();
            assertThat(entry.apiTypes()).isEmpty();
        });
        assertThat(catalog.refreshedAt()).isEqualTo(InMemoryCatalogPorts.REFRESHED_AT);
    }

    @Test
    void should_keep_a_declared_key_declared_once_documents_carry_it_and_set_its_last_seen() {
        fixture.givenDeclared(BASKET);
        fixture.ports.givenDiscovered(List.of(discovered("double_checkout_basket-value", AdditionalMetricValueType.DOUBLE)), false);

        var entries = fixture.listed().entries();

        assertThat(entries)
            .filteredOn(entry -> entry.field().rawKey().equals("double_checkout_basket-value"))
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.origin()).isEqualTo(CatalogOrigin.DECLARED);
                assertThat(entry.lastSeen()).isEqualTo(SEEN);
            });
    }

    @Test
    void should_list_a_discovered_key_with_its_derived_label_and_back_to_discovered_once_its_record_is_deleted() {
        fixture.ports.givenDiscovered(List.of(discovered("long_my-policy_score", AdditionalMetricValueType.LONG)), false);
        fixture.givenDeclared(ENVIRONMENT, "additional-metrics.long_my-policy_score", "Policy score", null, true);
        assertThat(entry(fixture.listed().entries(), "additional-metrics.long_my-policy_score"))
            .get()
            .extracting(AdditionalMetricCatalogEntry::origin)
            .isEqualTo(CatalogOrigin.DECLARED);

        fixture.delete.execute(new DeleteMetricDefinitionUseCase.Input(ENVIRONMENT, "additional-metrics.long_my-policy_score"));

        assertThat(entry(fixture.listed().entries(), "additional-metrics.long_my-policy_score")).hasValueSatisfying(entry -> {
            assertThat(entry.origin()).isEqualTo(CatalogOrigin.DISCOVERED);
            assertThat(entry.label()).isEqualTo("My policy score");
            assertThat(entry.lastSeen()).isEqualTo(SEEN);
            assertThat(entry.exposed()).isTrue();
        });
    }

    @Test
    void should_drop_a_deleted_record_without_data() {
        fixture.givenDeclared(BASKET);
        fixture.delete.execute(new DeleteMetricDefinitionUseCase.Input(ENVIRONMENT, BASKET));

        assertThat(entry(fixture.listed().entries(), BASKET)).isEmpty();
    }

    @Test
    void should_list_exposed_off_records_too() {
        fixture.givenDeclared(ENVIRONMENT, BASKET, null, null, false);

        assertThat(entry(fixture.listed().entries(), BASKET)).get().extracting(AdditionalMetricCatalogEntry::exposed).isEqualTo(false);
    }

    @Test
    void should_serve_a_built_in_key_under_its_built_in_name_label_and_api_types_once() {
        fixture.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
        fixture.ports.givenDiscovered(List.of(discovered("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD)), false);

        var entries = fixture.listed().entries();

        assertThat(entries)
            .filteredOn(entry -> entry.field().rawKey().equals("keyword_mcp-proxy_tools/call"))
            .singleElement()
            .satisfies(entry -> {
                assertThat(entry.name()).isEqualTo("MCP_PROXY_TOOL");
                assertThat(entry.origin()).isEqualTo(CatalogOrigin.BUILT_IN);
                assertThat(entry.label()).isEqualTo("MCP Tool");
                assertThat(entry.apiTypes()).contains(ApiType.MCP);
                assertThat(entry.lastSeen()).isEqualTo(SEEN);
            });
    }

    @Test
    void should_label_and_scope_a_built_in_metric_the_filter_catalog_does_not_describe_from_the_analytics_definition() {
        fixture.ports.givenBuiltIn("long_llm_tokens-sent", "LLM_PROMPT_TOKEN_SENT");
        fixture.ports.givenBuiltInMetric(aMetric("LLM_PROMPT_TOKEN_SENT", List.of(ApiType.LLM)));

        assertThat(builtIn("long_llm_tokens-sent")).satisfies(entry -> {
            assertThat(entry.name()).isEqualTo("LLM_PROMPT_TOKEN_SENT");
            assertThat(entry.label()).isEqualTo("LLM_PROMPT_TOKEN_SENT label");
            assertThat(entry.apiTypes()).containsExactly(ApiType.LLM);
            assertThat(entry.type()).isNull();
        });
    }

    @Test
    void should_label_a_built_in_facet_from_the_analytics_definition_and_scope_it_to_every_kind_its_metrics_serve() {
        fixture.ports.givenBuiltIn("keyword_edge_model", "EDGE_MODEL");
        fixture.ports.givenBuiltInMetric(aMetric("EDGE_TOKENS_IN", List.of(ApiType.EDGE), "EDGE_MODEL"));
        fixture.ports.givenBuiltInMetric(aMetric("LLM_TOKENS_IN", List.of(ApiType.LLM), "EDGE_MODEL"));

        assertThat(builtIn("keyword_edge_model")).satisfies(entry -> {
            assertThat(entry.label()).isEqualTo("EDGE_MODEL label");
            assertThat(entry.apiTypes()).containsExactlyInAnyOrder(ApiType.EDGE, ApiType.LLM);
        });
    }

    @Test
    void should_prefer_the_filter_catalog_over_the_analytics_definition_for_a_name_both_describe() {
        fixture.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
        fixture.ports.givenBuiltInMetric(aMetric("MCP_TOOL_CALLS", List.of(ApiType.LLM), "MCP_PROXY_TOOL"));

        assertThat(builtIn("keyword_mcp-proxy_tools/call")).satisfies(entry -> {
            assertThat(entry.label()).isEqualTo("MCP Tool");
            assertThat(entry.apiTypes()).contains(ApiType.MCP).doesNotContain(ApiType.LLM);
        });
    }

    @Test
    void should_not_list_a_key_read_through_a_constant_only_nor_its_discovered_duplicate() {
        fixture.ports.givenBuiltIn("keyword_native-kafka_broker-id");
        fixture.ports.givenDiscovered(List.of(discovered("keyword_native-kafka_broker-id", AdditionalMetricValueType.KEYWORD)), false);

        assertThat(fixture.listed().entries()).noneMatch(entry -> entry.field().rawKey().equals("keyword_native-kafka_broker-id"));
    }

    @Test
    void should_skip_a_discovered_key_outside_the_grammar_or_whose_type_contradicts_its_prefix() {
        fixture.ports.givenDiscovered(
            List.of(
                discovered("long_a\"b", AdditionalMetricValueType.LONG),
                discovered("long_score", AdditionalMetricValueType.DOUBLE),
                discovered("float_score", AdditionalMetricValueType.DOUBLE)
            ),
            false
        );

        assertThat(fixture.listed().entries()).isEmpty();
    }

    @Test
    void should_skip_a_discovered_key_of_the_reserved_custom_form() {
        fixture.ports.givenDiscovered(List.of(discovered("double_custom_basket-value", AdditionalMetricValueType.DOUBLE)), false);

        assertThat(fixture.listed().entries()).isEmpty();
    }

    @Test
    void should_serve_no_discovered_entry_no_last_seen_and_no_truncation_and_run_no_discovery_for_a_caller_who_does_not_read_traffic() {
        fixture.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
        fixture.givenDeclared(BASKET);
        fixture.ports.givenDiscovered(
            List.of(
                discovered("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD),
                discovered("double_checkout_basket-value", AdditionalMetricValueType.DOUBLE),
                discovered("long_failover_count", AdditionalMetricValueType.LONG)
            ),
            true
        );
        fixture.ports.givenReadsTraffic(false);

        var catalog = fixture.listed();

        assertThat(catalog.entries())
            .extracting(AdditionalMetricCatalogEntry::origin)
            .containsExactly(CatalogOrigin.BUILT_IN, CatalogOrigin.DECLARED);
        assertThat(catalog.entries()).allSatisfy(entry -> assertThat(entry.lastSeen()).isNull());
        assertThat(catalog.truncated()).isFalse();
        assertThat(fixture.ports.discoveries()).isZero();
    }

    @Test
    void should_report_a_truncated_pass_to_a_traffic_reader() {
        fixture.ports.givenDiscovered(List.of(discovered("long_failover_count", AdditionalMetricValueType.LONG)), true);

        assertThat(fixture.listed().truncated()).isTrue();
    }

    @Test
    void should_never_list_the_records_of_another_environment() {
        fixture.givenDeclared(OTHER_ENVIRONMENT, BASKET, null, null, true);

        assertThat(fixture.listed().entries()).isEmpty();
    }

    @Test
    void should_list_the_200_most_recently_seen_discovered_keys_never_counting_built_in_or_declared_ones() {
        fixture.givenDeclared(BASKET);
        fixture.ports.givenBuiltIn("keyword_mcp-proxy_tools/call", "MCP_PROXY_TOOL");
        List<DiscoveredAdditionalMetric> found = new ArrayList<>();
        IntStream.range(0, 201).forEach(i ->
            found.add(new DiscoveredAdditionalMetric("long_key-%03d".formatted(i), AdditionalMetricValueType.LONG, SEEN.minusSeconds(i)))
        );
        found.add(new DiscoveredAdditionalMetric("double_checkout_basket-value", AdditionalMetricValueType.DOUBLE, SEEN.minusSeconds(500)));
        found.add(
            new DiscoveredAdditionalMetric("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD, SEEN.minusSeconds(600))
        );
        fixture.ports.givenDiscovered(found, false);

        var catalog = fixture.listed();

        assertThat(catalog.entries())
            .filteredOn(entry -> entry.origin() == CatalogOrigin.DISCOVERED)
            .hasSize(200);
        assertThat(entry(catalog.entries(), "additional-metrics.long_key-200")).isEmpty();
        assertThat(entry(catalog.entries(), BASKET)).hasValueSatisfying(entry ->
            assertThat(entry.lastSeen()).isEqualTo(SEEN.minusSeconds(500))
        );
        assertThat(builtIn("keyword_mcp-proxy_tools/call").lastSeen()).isEqualTo(SEEN.minusSeconds(600));
        assertThat(catalog.truncated()).isTrue();
        assertThat(fixture.ports.lastUncountedKeys()).containsExactlyInAnyOrder(
            "double_checkout_basket-value",
            "keyword_mcp-proxy_tools/call"
        );
    }

    private static DiscoveredAdditionalMetric discovered(String rawKey, AdditionalMetricValueType valueType) {
        return new DiscoveredAdditionalMetric(rawKey, valueType, SEEN);
    }

    private static Optional<AdditionalMetricCatalogEntry> entry(List<AdditionalMetricCatalogEntry> entries, String name) {
        return entries
            .stream()
            .filter(entry -> entry.name().equals(name))
            .findFirst();
    }

    private AdditionalMetricCatalogEntry builtIn(String rawKey) {
        return fixture
            .listed()
            .entries()
            .stream()
            .filter(entry -> entry.field().rawKey().equals(rawKey))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No entry for " + rawKey));
    }
}
