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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.query_service.analytics_engine.BuiltInAdditionalMetricsQueryServiceImpl;
import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricDiscovery;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricField;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricValueType;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogFlags;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.model.DiscoveredAdditionalMetric;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.ListMetricDefinitionsUseCase;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.BuiltInAdditionalMetricKeys;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * The list's built-in entries from the real chain: the ES field resolvers, the filter-values mapping, the Gamma
 * filter catalog and the analytics definition, with no key stubbed.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class BuiltInAdditionalMetricCatalogTest {

    /**
     * Edge filters missing from {@code StaticFilters} and absent from the analytics definition's metrics and facets:
     * nothing describes them, so they are listed with a derived label and no API kind.
     */
    private static final Set<String> UNDESCRIBED = Set.of("EDGE_MODEL", "EDGE_TOOL", "EDGE_VERSION");

    private final List<AdditionalMetricCatalogEntry> builtIns = builtInEntries();

    @Test
    void should_list_each_built_in_name_once() {
        assertThat(builtIns).isNotEmpty();
        assertThat(builtIns.stream().collect(Collectors.groupingBy(AdditionalMetricCatalogEntry::name, Collectors.counting()))).allSatisfy(
            (name, count) -> assertThat(count).as(name).isEqualTo(1L)
        );
    }

    @Test
    void should_list_the_companion_fields_nowhere() {
        assertThat(builtIns)
            .extracting(entry -> entry.field().rawKey())
            .doesNotContain("keyword_authz_subject-type", "keyword_authz_resource-type")
            .contains("keyword_authz_subject-id", "keyword_mcp-proxy_tools/call", "keyword_native-kafka_client-software-version");
    }

    @Test
    void should_label_and_scope_every_built_in_entry_from_the_filter_catalog_or_the_analytics_definition_but_the_undescribed() {
        var byName = builtIns.stream().collect(Collectors.toMap(AdditionalMetricCatalogEntry::name, Function.identity()));
        var filterCatalogLabels = new SpiFilterRegistry()
            .getFilters(Set.of(), Set.of())
            .stream()
            .collect(Collectors.toMap(FilterSpec::name, FilterSpec::label));

        assertThat(byName.keySet()).containsAll(UNDESCRIBED);
        assertThat(byName.get("EDGE_MODEL").label()).as("an undescribed built-in keeps its namespace in the label").isEqualTo("Edge model");
        assertThat(byName.values())
            .filteredOn(entry -> !UNDESCRIBED.contains(entry.name()))
            .allSatisfy(entry -> {
                assertThat(entry.apiTypes()).as(entry.name()).isNotEmpty();
                // The filter catalog's label, which may read like the derived one ("Operation"); else the analytics
                // definition's, which never does.
                if (filterCatalogLabels.containsKey(entry.name())) {
                    assertThat(entry.label()).as(entry.name()).isEqualTo(filterCatalogLabels.get(entry.name()));
                } else {
                    assertThat(entry.label()).as(entry.name()).isNotEqualTo(entry.field().derivedLabel());
                }
            });
        assertThat(byName.get("MCP_PROXY_TOOL").label()).isEqualTo("MCP Tool");
        assertThat(byName.get("MCP_PROXY_TOOL").type()).isNotNull();
        assertThat(byName.get("LLM_PROMPT_TOKEN_SENT").apiTypes()).isNotEmpty();
    }

    /**
     * The filter catalog on the same real set: a discovered key a built-in reads, the logs-only one and an AuthZ
     * companion included, is never a {@code DISCOVERED} entry, and every built-in reading a key is flagged with it.
     */
    @Test
    void should_flag_the_built_ins_and_list_no_key_they_read_as_discovered_in_the_filter_catalog() {
        var builtInPort = platformBuiltIns();
        var builtInNamesByRawKey = builtInPort.builtInNamesByRawKey();
        var keys = new MetricDefinitionKeyDomainService(new AdditionalMetricGrammarPortAdapter(), builtInPort);
        var seen = Instant.parse("2026-10-30T08:00:00Z");
        var discovery = new AdditionalMetricDiscovery(
            List.of(
                new DiscoveredAdditionalMetric("keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD, seen),
                new DiscoveredAdditionalMetric("keyword_native-kafka_client-software-version", AdditionalMetricValueType.KEYWORD, seen),
                new DiscoveredAdditionalMetric("keyword_llm-proxy_model", AdditionalMetricValueType.KEYWORD, seen),
                new DiscoveredAdditionalMetric("keyword_authz_subject-type", AdditionalMetricValueType.KEYWORD, seen),
                new DiscoveredAdditionalMetric("long_failover_count", AdditionalMetricValueType.LONG, seen)
            ),
            seen,
            false
        );

        var registry = new SpiFilterRegistry();
        var catalog = new AdditionalMetricCatalogDomainService(keys).filterCatalog(
            registry.getFilters(Set.of(), Set.of()),
            filterNames(registry),
            analyticsPort(registry).builtInMetrics(),
            builtInNamesByRawKey,
            List.of(),
            Optional.of(discovery),
            Set.of(),
            Set.of()
        );

        var byName = catalog.stream().collect(Collectors.toMap(FilterSpec::name, Function.identity()));
        assertThat(catalog)
            .filteredOn(spec -> spec.catalog() != null && spec.catalog().origin() == CatalogOrigin.DISCOVERED)
            .extracting(spec -> spec.catalog().rawKey())
            .containsExactly("long_failover_count");
        assertThat(byName.get("MCP_PROXY_TOOL").catalog()).isEqualTo(
            new CatalogFlags(CatalogOrigin.BUILT_IN, "keyword_mcp-proxy_tools/call", AdditionalMetricValueType.KEYWORD, seen, true, null)
        );
        assertThat(byName.get("NATIVE_CLIENT_SOFTWARE_VERSION").catalog().rawKey()).isEqualTo(
            "keyword_native-kafka_client-software-version"
        );
        assertThat(byName.get("LLM_PROXY_MODEL").catalog().rawKey()).isEqualTo("keyword_llm-proxy_model");
        assertThat(catalog)
            .filteredOn(spec -> spec.catalog() != null && spec.catalog().origin() == CatalogOrigin.BUILT_IN)
            .allSatisfy(spec -> {
                if (spec.name().startsWith(AdditionalMetricField.NAME_PREFIX)) {
                    assertThat(spec.name()).isEqualTo(AdditionalMetricField.NAME_PREFIX + spec.catalog().rawKey());
                } else {
                    assertThat(builtInNamesByRawKey.get(spec.catalog().rawKey())).as(spec.name()).contains(spec.name());
                }
            });
    }

    /**
     * The filter catalog on the same real set: the numeric keys the built-in metrics read, and no built-in filter
     * reads, are filters under their dynamic name with the metric's label and kinds, an Edge one on the analytics
     * signal only; the keyword keys the facets read are not; {@code metric-definitions} keeps listing every key under
     * its built-in name (OBS-193, second PR).
     */
    @Test
    void should_list_the_numeric_keys_the_built_in_metrics_read_as_filters_and_keep_the_metric_definitions_rows() {
        var registry = new SpiFilterRegistry();
        var builtInPort = platformBuiltIns();
        var keys = new MetricDefinitionKeyDomainService(new AdditionalMetricGrammarPortAdapter(), builtInPort);
        var metricsByName = analyticsPort(registry)
            .builtInMetrics()
            .stream()
            .collect(Collectors.toMap(MetricDefinition::name, Function.identity()));

        var catalog = new AdditionalMetricCatalogDomainService(keys).filterCatalog(
            registry.getFilters(Set.of(), Set.of()),
            filterNames(registry),
            metricsByName.values(),
            builtInPort.builtInNamesByRawKey(),
            List.of(),
            Optional.empty(),
            Set.of(),
            Set.of()
        );

        var builtInKeyFilters = catalog
            .stream()
            .filter(spec -> spec.origin() == CatalogOrigin.BUILT_IN && spec.name().startsWith(AdditionalMetricField.NAME_PREFIX))
            .collect(Collectors.toMap(FilterSpec::name, Function.identity()));
        assertThat(builtInKeyFilters.keySet()).containsExactlyInAnyOrder(
            "additional-metrics.long_llm-proxy_tokens-sent",
            "additional-metrics.long_llm-proxy_tokens-received",
            "additional-metrics.double_llm-proxy_sent-cost",
            "additional-metrics.double_llm-proxy_received-cost",
            "additional-metrics.long_edge_tokens_in",
            "additional-metrics.long_edge_tokens_out",
            "additional-metrics.long_edge_count"
        );
        assertThat(builtInKeyFilters.get("additional-metrics.long_llm-proxy_tokens-sent")).satisfies(spec -> {
            assertThat(spec.label()).isEqualTo(metricsByName.get("LLM_PROMPT_TOKEN_SENT").label());
            assertThat(spec.apiTypes()).containsExactly(ApiType.LLM);
            assertThat(spec.signals()).containsExactlyInAnyOrder(Signal.LOGS, Signal.ANALYTICS);
            assertThat(spec.catalog().rawKey()).isEqualTo("long_llm-proxy_tokens-sent");
        });
        assertThat(builtInKeyFilters.get("additional-metrics.double_llm-proxy_sent-cost").takesDecimals()).isTrue();
        assertThat(builtInKeyFilters.values())
            .filteredOn(spec -> spec.apiTypes().equals(Set.of(ApiType.EDGE)))
            .hasSize(3)
            .allSatisfy(spec -> assertThat(spec.signals()).containsExactly(Signal.ANALYTICS));
        assertThat(catalog)
            .extracting(FilterSpec::name)
            .doesNotHaveDuplicates()
            .doesNotContain("additional-metrics.keyword_edge_model", "additional-metrics.keyword_llm-proxy_model");
        var registryNames = filterNames(registry);
        assertThat(registryNames)
            .as("no registry filter under the dynamic form: the entries come from the keys, never from StaticFilters")
            .noneMatch(name -> name.startsWith(AdditionalMetricField.NAME_PREFIX));
        builtInKeyFilters
            .values()
            .forEach(spec ->
                assertThat(builtInPort.builtInNamesByRawKey().get(spec.catalog().rawKey()))
                    .as(spec.name() + " is read by no registry filter")
                    .noneMatch(registryNames::contains)
            );
        assertThat(builtIns)
            .as("the metric-definitions rows keep the built-in names")
            .extracting(AdditionalMetricCatalogEntry::name)
            .noneMatch(name -> name.startsWith(AdditionalMetricField.NAME_PREFIX));
        assertThat(builtIns)
            .extracting(entry -> entry.field().rawKey())
            .containsAll(
                builtInKeyFilters
                    .values()
                    .stream()
                    .map(spec -> spec.catalog().rawKey())
                    .toList()
            );
    }

    /**
     * The analytics definition on the same real set: a discovered numeric key a built-in metric reads is never an
     * {@code ADDITIONAL} metric, the built-in is; a numeric key no built-in reads is.
     */
    @Test
    void should_list_no_key_a_built_in_metric_reads_as_an_additional_metric_of_the_definition() {
        var builtInPort = platformBuiltIns();
        var builtInNamesByRawKey = builtInPort.builtInNamesByRawKey();
        var keys = new MetricDefinitionKeyDomainService(new AdditionalMetricGrammarPortAdapter(), builtInPort);
        var seen = Instant.parse("2026-11-03T08:00:00Z");
        var discovery = new AdditionalMetricDiscovery(
            List.of(
                new DiscoveredAdditionalMetric("long_llm-proxy_tokens-sent", AdditionalMetricValueType.LONG, seen),
                new DiscoveredAdditionalMetric("double_llm-proxy_sent-cost", AdditionalMetricValueType.DOUBLE, seen),
                new DiscoveredAdditionalMetric("long_failover_count", AdditionalMetricValueType.LONG, seen)
            ),
            seen,
            false
        );
        assertThat(builtInNamesByRawKey).containsKeys("long_llm-proxy_tokens-sent", "double_llm-proxy_sent-cost");

        var metrics = new AdditionalMetricCatalogDomainService(keys)
            .analyticsCatalog(
                builtInNamesByRawKey,
                List.of(),
                Optional.of(discovery),
                new InMemoryCatalogPorts().additionalMetricSupport(),
                Set.of()
            )
            .metrics();

        assertThat(metrics).extracting(MetricDefinition::name).containsExactly("additional-metrics.long_failover_count");
    }

    /** The key set the platform builds: the ES field resolvers and the logs-path adapters. */
    private static BuiltInAdditionalMetricsPortAdapter platformBuiltIns() {
        var analyticsRepository = mock(AnalyticsRepository.class);
        when(analyticsRepository.builtInAdditionalMetricKeys()).thenReturn(BuiltInAdditionalMetricKeys.all());
        return new BuiltInAdditionalMetricsPortAdapter(new BuiltInAdditionalMetricsQueryServiceImpl(analyticsRepository));
    }

    private static Set<String> filterNames(SpiFilterRegistry registry) {
        return registry.getFilters(Set.of(), Set.of()).stream().map(FilterSpec::name).collect(Collectors.toSet());
    }

    private static AnalyticsDefinitionPortAdapter analyticsPort(SpiFilterRegistry filterRegistry) {
        var definition = new AnalyticsDefinitionYAMLQueryService();
        return new AnalyticsDefinitionPortAdapter(
            new GetApiMetricSpecUseCase(definition),
            new GetMetricFacetSpecUseCase(definition),
            new GetMetricFilterSpecUseCase(definition),
            filterRegistry,
            AnalyticsEngineRouting.provider()
        );
    }

    private static List<AdditionalMetricCatalogEntry> builtInEntries() {
        var filterRegistry = new SpiFilterRegistry();
        var platform = new InMemoryCatalogPorts();
        var builtInPort = platformBuiltIns();
        var list = new ListMetricDefinitionsUseCase(
            new InMemoryAdditionalMetricDefinitionRepository(),
            builtInPort,
            platform,
            platform,
            filterRegistry,
            analyticsPort(filterRegistry),
            new AdditionalMetricCatalogDomainService(
                new MetricDefinitionKeyDomainService(new AdditionalMetricGrammarPortAdapter(), builtInPort)
            )
        );
        return list
            .execute(new ListMetricDefinitionsUseCase.Input("DEFAULT", "DEFAULT"))
            .catalog()
            .entries()
            .stream()
            .filter(entry -> entry.origin() == CatalogOrigin.BUILT_IN)
            .toList();
    }
}
