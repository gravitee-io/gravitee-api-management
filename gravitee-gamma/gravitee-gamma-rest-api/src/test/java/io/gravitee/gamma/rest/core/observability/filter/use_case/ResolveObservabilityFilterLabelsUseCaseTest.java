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

import io.gravitee.gamma.rest.core.observability.filter.model.FilterValuesPage;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.ObservabilityFilterDataPort;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ResolveObservabilityFilterLabelsUseCaseTest {

    private final StubDataPort dataPort = new StubDataPort();
    private final ResolveObservabilityFilterLabelsUseCase useCase = new ResolveObservabilityFilterLabelsUseCase(dataPort);

    @Test
    void should_resolve_labels_grouped_by_filter_via_the_port() {
        dataPort.labelsByFilter = Map.of("API", Map.of("api-1", "Petstore"), "APPLICATION", Map.of("app-1", "Web app"));

        var output = useCase.execute(
            new ResolveObservabilityFilterLabelsUseCase.Input(
                List.of(
                    new ResolveObservabilityFilterLabelsUseCase.Entry("API", List.of("api-1")),
                    new ResolveObservabilityFilterLabelsUseCase.Entry("APPLICATION", List.of("app-1"))
                )
            )
        );

        assertThat(output.entries())
            .extracting(ResolveObservabilityFilterLabelsUseCase.ResolvedEntry::filterName)
            .containsExactly("API", "APPLICATION");
        assertThat(output.entries().get(0).labels()).containsEntry("api-1", "Petstore");
        assertThat(output.entries().get(1).labels()).containsEntry("app-1", "Web app");
    }

    @Test
    void should_handle_a_null_entries_input_gracefully() {
        var output = useCase.execute(new ResolveObservabilityFilterLabelsUseCase.Input(null));

        assertThat(output.entries()).isEmpty();
    }

    private static final class StubDataPort implements ObservabilityFilterDataPort {

        private Map<String, Map<String, String>> labelsByFilter = Map.of();

        @Override
        public FilterValuesPage listKeywordValues(
            String filterName,
            String query,
            Long from,
            Long to,
            int page,
            int perPage,
            java.util.Set<io.gravitee.gamma.rest.core.observability.filter.model.ApiType> apiTypes
        ) {
            return new FilterValuesPage(List.of(), 0L);
        }

        @Override
        public List<ResolvedLabels> resolveLabels(List<ResolveRequest> requests) {
            return requests
                .stream()
                .map(r -> new ResolvedLabels(r.filterName(), labelsByFilter.getOrDefault(r.filterName(), Map.of())))
                .toList();
        }
    }
}
