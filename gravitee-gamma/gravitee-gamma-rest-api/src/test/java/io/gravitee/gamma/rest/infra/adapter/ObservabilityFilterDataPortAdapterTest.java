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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ResolveFilterLabelsUseCase;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.filter.exception.FilterCatalogDriftException;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ObservabilityFilterDataPortAdapterTest {

    private final GetFilterValuesUseCase getFilterValuesUseCase = mock(GetFilterValuesUseCase.class);
    private final ResolveFilterLabelsUseCase resolveFilterLabelsUseCase = mock(ResolveFilterLabelsUseCase.class);
    private final ObservabilityFilterDataPortAdapter adapter = new ObservabilityFilterDataPortAdapter(
        getFilterValuesUseCase,
        resolveFilterLabelsUseCase
    );

    /** Two {@code FilterValue} records, one per catalog: the platform one is spelled out here, once. */
    private static io.gravitee.apim.core.analytics_engine.model.FilterValue platformValue(String value, String label) {
        return new io.gravitee.apim.core.analytics_engine.model.FilterValue(value, label);
    }

    @Test
    void should_map_platform_values_to_core_values_and_keep_the_reported_total() {
        when(getFilterValuesUseCase.execute(any())).thenReturn(
            new GetFilterValuesUseCase.Output(
                new FilterValuesPage(List.of(platformValue("mcp-studio", null), platformValue("Petstore", "api-1")), Map.of(), 7L)
            )
        );

        var page = adapter.listKeywordValues("ENTRYPOINT", null, null, null, 1, 10, Set.of());

        assertThat(page.data()).containsExactly(new FilterValue("mcp-studio", null), new FilterValue("api-1", "Petstore"));
        assertThat(page.totalElements()).isEqualTo(7L);
    }

    @Test
    void should_surface_a_platform_catalog_miss_as_a_drift_error_not_a_400() {
        // "Filter not found" from the platform means the observability catalog advertises a filter the
        // platform catalog does not declare: a catalog bug, never something the caller can fix.
        when(getFilterValuesUseCase.execute(any())).thenThrow(
            new ValidationDomainException("Filter not found", Map.of("filterName", "ENTRYPOINT"))
        );

        assertThatThrownBy(() -> adapter.listKeywordValues("ENTRYPOINT", null, null, null, 1, 10, Set.of()))
            .isInstanceOf(FilterCatalogDriftException.class)
            .hasMessageContaining("ENTRYPOINT");
    }

    @Test
    void should_surface_a_missing_store_mapping_as_a_drift_error() {
        // The platform knows the filter but has no field to aggregate on: the same drift, seen one layer lower.
        when(getFilterValuesUseCase.execute(any())).thenThrow(
            new UnsupportedOperationException("No ES field mapping for filter: NATIVE_TOPIC")
        );

        assertThatThrownBy(() -> adapter.listKeywordValues("NATIVE_TOPIC", null, null, null, 1, 10, Set.of()))
            .isInstanceOf(FilterCatalogDriftException.class)
            .hasMessageContaining("NATIVE_TOPIC");
    }
}
