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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.rest.core.observability.filter.exception.ObservabilityFilterNotFoundException;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValue;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityFilterValuesUseCaseTest {

    private final RecordingDataPort dataPort = new RecordingDataPort();
    private final GetObservabilityFilterValuesUseCase useCase = new GetObservabilityFilterValuesUseCase(new SpiFilterRegistry(), dataPort);

    @Test
    void should_return_enum_values_with_labels_for_an_enum_filter() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, null, null));

        assertThat(output.values().totalElements()).isEqualTo(6L);
        assertThat(output.values().data())
            .extracting(FilterValue::value, FilterValue::label)
            .contains(
                org.assertj.core.api.Assertions.tuple("NATIVE", "Kafka (native)"),
                org.assertj.core.api.Assertions.tuple("HTTP_PROXY", "HTTP Proxy")
            );
    }

    @Test
    void should_substring_filter_enum_values_case_insensitively_on_value_or_label() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", "kafka", null, null, null, null));

        // Matches the label "Kafka (native)" even though the value is NATIVE.
        assertThat(output.values().data()).extracting(FilterValue::value).containsExactly("NATIVE");
    }

    @Test
    void should_paginate_enum_values_1_based() {
        var firstPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 1, 2));
        var secondPage = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 2, 2));

        assertThat(firstPage.values().data()).hasSize(2);
        assertThat(secondPage.values().data()).hasSize(2);
        assertThat(firstPage.values().totalElements()).isEqualTo(6L);
        assertThat(firstPage.values().data()).isNotEqualTo(secondPage.values().data());
    }

    @Test
    void should_throw_404_when_filter_is_unknown() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("DOES_NOT_EXIST", null, null, null, null, null))
        ).isInstanceOf(ObservabilityFilterNotFoundException.class);
    }

    @Test
    void should_delegate_keyword_filter_to_the_data_port_with_resolved_pagination() {
        dataPort.nextPage = new FilterValuesPage(List.of(new FilterValue("api-1", "Petstore")), 1L);

        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API", "pet", null, null, null, null));

        assertThat(output.values()).isSameAs(dataPort.nextPage);
        assertThat(dataPort.lastFilterName).isEqualTo("API");
        assertThat(dataPort.lastQuery).isEqualTo("pet");
        // null page/perPage are resolved to the defaults (1 / 10) before reaching the port.
        assertThat(dataPort.lastPage).isEqualTo(1);
        assertThat(dataPort.lastPerPage).isEqualTo(10);
        // The resolved pagination is surfaced on the output so the REST layer builds a consistent envelope.
        assertThat(output.page()).isEqualTo(1);
        assertThat(output.perPage()).isEqualTo(10);
    }

    @Test
    void should_clamp_per_page_to_the_max_and_surface_it_on_the_output() {
        var output = useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, 1, 500));

        // perPage is clamped to MAX_PER_PAGE (100) and the clamped value is what the REST layer must use.
        assertThat(output.perPage()).isEqualTo(100);
        assertThat(output.page()).isEqualTo(1);
    }

    @Test
    void should_not_touch_the_data_port_for_number_filter() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("HTTP_STATUS", null, null, null, null, null))
        ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        assertThat(dataPort.lastFilterName).isNull();
    }

    @Test
    void should_not_touch_the_data_port_for_string_filter() {
        assertThatThrownBy(() ->
            useCase.execute(new GetObservabilityFilterValuesUseCase.Input("URI", null, null, null, null, null))
        ).isInstanceOf(UnsupportedObservabilityFilterException.class);
        assertThat(dataPort.lastFilterName).isNull();
    }

    @Test
    void should_restrict_api_type_enum_values_when_apiTypes_constraint_is_provided() {
        var output = useCase.execute(
            new GetObservabilityFilterValuesUseCase.Input("API_TYPE", null, null, null, null, null, Set.of(ApiType.MCP, ApiType.LLM))
        );

        assertThat(output.values().totalElements()).isEqualTo(2L);
        assertThat(output.values().data()).extracting(FilterValue::value).containsExactlyInAnyOrder("MCP", "LLM");
    }

    @Test
    void should_propagate_apiTypes_to_the_data_port_for_keyword_filters() {
        dataPort.nextPage = new FilterValuesPage(List.of(new FilterValue("api-1", "Petstore")), 1L);

        useCase.execute(new GetObservabilityFilterValuesUseCase.Input("API", null, null, null, null, null, Set.of(ApiType.HTTP_PROXY)));

        assertThat(dataPort.lastApiTypes).containsExactly(ApiType.HTTP_PROXY);
    }

    private static final class RecordingDataPort implements ObservabilityFilterDataPort {

        private FilterValuesPage nextPage = new FilterValuesPage(List.of(), 0L);
        private String lastFilterName;
        private String lastQuery;
        private Long lastFrom;
        private Long lastTo;
        private Integer lastPage;
        private Integer lastPerPage;
        private Set<ApiType> lastApiTypes;

        @Override
        public FilterValuesPage listKeywordValues(
            String filterName,
            String query,
            Long from,
            Long to,
            int page,
            int perPage,
            Set<ApiType> apiTypes
        ) {
            this.lastFilterName = filterName;
            this.lastQuery = query;
            this.lastFrom = from;
            this.lastTo = to;
            this.lastPage = page;
            this.lastPerPage = perPage;
            this.lastApiTypes = apiTypes;
            return nextPage;
        }

        @Override
        public List<ResolvedLabels> resolveLabels(List<ResolveRequest> requests) {
            return requests
                .stream()
                .map(r -> new ResolvedLabels(r.filterName(), Map.of()))
                .toList();
        }
    }
}
