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
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.AdditionalMetricCatalogDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.domain_service.MetricDefinitionKeyDomainService;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryAdditionalMetricDefinitionRepository;
import io.gravitee.gamma.rest.core.observability.catalog.inmemory.InMemoryCatalogPorts;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricCatalogEntry;
import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.catalog.use_case.ListMetricDefinitionsUseCase;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.BuiltInAdditionalMetricKeys;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import java.util.List;
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

        assertThat(byName.keySet()).containsAll(UNDESCRIBED);
        assertThat(byName.values())
            .filteredOn(entry -> !UNDESCRIBED.contains(entry.name()))
            .allSatisfy(entry -> {
                assertThat(entry.apiTypes()).as(entry.name()).isNotEmpty();
                assertThat(entry.label()).as(entry.name()).isNotEqualTo(entry.field().derivedLabel());
            });
        assertThat(byName.get("MCP_PROXY_TOOL").label()).isEqualTo("MCP Tool");
        assertThat(byName.get("MCP_PROXY_TOOL").type()).isNotNull();
        assertThat(byName.get("LLM_PROMPT_TOKEN_SENT").apiTypes()).isNotEmpty();
    }

    private static List<AdditionalMetricCatalogEntry> builtInEntries() {
        var analyticsRepository = mock(AnalyticsRepository.class);
        when(analyticsRepository.builtInAdditionalMetricKeys()).thenReturn(BuiltInAdditionalMetricKeys.all());
        var definition = new AnalyticsDefinitionYAMLQueryService();
        var filterRegistry = new SpiFilterRegistry();
        var platform = new InMemoryCatalogPorts();
        var builtInPort = new BuiltInAdditionalMetricsPortAdapter(new BuiltInAdditionalMetricsQueryServiceImpl(analyticsRepository));
        var list = new ListMetricDefinitionsUseCase(
            new InMemoryAdditionalMetricDefinitionRepository(),
            builtInPort,
            platform,
            platform,
            filterRegistry,
            new AnalyticsDefinitionPortAdapter(
                new GetApiMetricSpecUseCase(definition),
                new GetMetricFacetSpecUseCase(definition),
                new GetMetricFilterSpecUseCase(definition),
                filterRegistry
            ),
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
