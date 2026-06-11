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
package io.gravitee.gamma.rest.core.observability.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterContributor;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SpiFilterRegistryTest {

    @Test
    void should_expose_the_host_catalog_by_default() {
        FilterRegistry registry = registryWith();

        List<FilterSpec> result = registry.getFilters(null, null);

        // Full unified vocabulary: every static filter plus the extensible API_TYPE (no contributors).
        assertThat(result).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
        assertThat(result)
            .extracting(FilterSpec::name)
            .contains("API", "HTTP_STATUS", "API_TYPE", "HTTP_GATEWAY_RESPONSE_TIME", "MCP_PROXY_METHOD", "URI")
            .doesNotContain("HTTP_PATH", "HTTP_PATH_MAPPING");
    }

    @Test
    void should_seed_api_type_with_the_api_type_baseline() {
        FilterRegistry registry = registryWith();

        FilterSpec apiType = byName(registry, "API_TYPE");

        assertThat(apiType.type()).isEqualTo(FilterType.ENUM);
        assertThat(apiType.operators()).containsExactly(FilterOperator.EQ, FilterOperator.IN);
        assertThat(apiType.enumValues())
            .extracting(FilterSpec.EnumValue::value)
            .containsExactly(Arrays.stream(ApiType.values()).map(Enum::name).toArray(String[]::new));
        assertThat(apiType.enumValues())
            .filteredOn(v -> v.value().equals("NATIVE"))
            .singleElement()
            .extracting(FilterSpec.EnumValue::label)
            .isEqualTo("Kafka (native)");
    }

    @Test
    void should_scope_http_status_to_http_based_api_kinds() {
        FilterRegistry registry = registryWith();

        // NATIVE (Kafka) context: cross-cutting + native-relevant filters only; HTTP / gateway filters drop out.
        List<FilterSpec> result = registry.getFilters(null, Set.of(ApiType.NATIVE));

        assertThat(result)
            .extracting(FilterSpec::name)
            .contains("API", "APPLICATION", "PLAN", "NATIVE_CONNECTION_STATUS", "API_TYPE")
            .doesNotContain("HTTP_STATUS", "GATEWAY", "API_PRODUCT", "MCP_PROXY_METHOD");
    }

    @Test
    void should_add_a_contributor_filter() {
        FilterRegistry registry = registryWith(filtersContributor(llmModel()));

        List<FilterSpec> result = registry.getFilters(null, null);

        // Contributor filters are appended after the host catalog.
        assertThat(result).extracting(FilterSpec::name).contains("API", "API_TYPE", "LLM_MODEL");
        assertThat(result).last().extracting(FilterSpec::name).isEqualTo("LLM_MODEL");
    }

    @Test
    void should_reject_a_module_filter_that_redefines_a_host_owned_name() {
        FilterSpec rogueApi = new FilterSpec(
            "API",
            "Rogue API",
            FilterType.KEYWORD,
            List.of(FilterOperator.EQ),
            null,
            null,
            Set.of(Signal.LOGS),
            Set.of(ApiType.LLM)
        );
        FilterRegistry registry = registryWith(filtersContributor(rogueApi));

        FilterSpec api = byName(registry, "API");

        // Host definition wins (cross-cutting), the module's is ignored.
        assertThat(api.label()).isEqualTo("API");
        assertThat(api.apiTypes()).isEqualTo(ApiType.ALL);
        assertThat(registry.getFilters(null, null)).extracting(FilterSpec::name).filteredOn("API"::equals).hasSize(1);
    }

    @Test
    void should_reject_a_module_filter_that_redefines_an_extensible_host_filter() {
        FilterSpec rogueApiType = new FilterSpec(
            "API_TYPE",
            "Rogue",
            FilterType.ENUM,
            List.of(FilterOperator.EQ),
            List.of(new FilterSpec.EnumValue("X", "X")),
            null,
            Signal.ALL,
            ApiType.ALL
        );
        FilterRegistry registry = registryWith(filtersContributor(rogueApiType));

        // API_TYPE stays the host extensible filter (baseline values), the module's is ignored.
        assertThat(byName(registry, "API_TYPE").enumValues()).hasSize(ApiType.values().length);
    }

    @Test
    void should_extend_api_type_with_contributed_values() {
        FilterRegistry registry = registryWith(
            valuesContributor(ExtensibleFilters.API_TYPE, new FilterSpec.EnumValue("CUSTOM", "Custom kind"))
        );

        assertThat(byName(registry, "API_TYPE").enumValues())
            .extracting(FilterSpec.EnumValue::value, FilterSpec.EnumValue::label)
            .contains(tuple("NATIVE", "Kafka (native)"), tuple("CUSTOM", "Custom kind"));
    }

    @Test
    void should_deduplicate_a_contributed_value_already_in_the_baseline() {
        FilterRegistry registry = registryWith(
            valuesContributor(ExtensibleFilters.API_TYPE, new FilterSpec.EnumValue("LLM", "duplicate label"))
        );

        FilterSpec apiType = byName(registry, "API_TYPE");

        assertThat(apiType.enumValues()).hasSize(ApiType.values().length); // no duplicate added
        assertThat(apiType.enumValues())
            .filteredOn(v -> v.value().equals("LLM"))
            .singleElement()
            .extracting(FilterSpec.EnumValue::label)
            .isEqualTo("LLM"); // baseline label wins
    }

    @Test
    void should_apply_both_axes_to_host_and_contributor_filters() {
        FilterRegistry registry = registryWith(filtersContributor(llmModel()));

        // LOGS + NATIVE: logs-served filters relevant to native APIs. ANALYTICS-only (NATIVE_CONNECTION_STATUS),
        // HTTP-only and LLM_MODEL (LLM-only) are all excluded by one axis or the other.
        List<FilterSpec> result = registry.getFilters(Set.of(Signal.LOGS), Set.of(ApiType.NATIVE));

        assertThat(result).extracting(FilterSpec::name).containsExactly("API", "APPLICATION", "PLAN", "API_TYPE");
    }

    private static FilterRegistry registryWith(FilterContributor... contributors) {
        return new SpiFilterRegistry(List.of(contributors));
    }

    private static FilterSpec byName(FilterRegistry registry, String name) {
        return registry
            .getFilters(null, null)
            .stream()
            .filter(spec -> spec.name().equals(name))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Filter '" + name + "' not found"));
    }

    private static FilterSpec llmModel() {
        return new FilterSpec(
            "LLM_MODEL",
            "LLM model",
            FilterType.KEYWORD,
            List.of(FilterOperator.EQ, FilterOperator.IN),
            null,
            null,
            Set.of(Signal.LOGS, Signal.ANALYTICS),
            Set.of(ApiType.LLM)
        );
    }

    private static FilterContributor filtersContributor(FilterSpec... filters) {
        return new FilterContributor() {
            @Override
            public List<FilterSpec> filters() {
                return List.of(filters);
            }
        };
    }

    private static FilterContributor valuesContributor(ExtensibleFilters key, FilterSpec.EnumValue... values) {
        return new FilterContributor() {
            @Override
            public Map<ExtensibleFilters, List<FilterSpec.EnumValue>> enumValues() {
                return Map.of(key, List.of(values));
            }
        };
    }
}
