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

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.ExtensibleFilters;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.model.StaticFilters;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetObservabilityFilterDefinitionsUseCaseTest {

    // Real registry → exercises the actual unified vocabulary shipped with the host catalog.
    private final GetObservabilityFilterDefinitionsUseCase useCase = new GetObservabilityFilterDefinitionsUseCase(new SpiFilterRegistry());

    @Test
    void should_return_the_full_catalog_when_both_axes_are_empty() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(Set.of(), Set.of()));

        assertThat(output.filters()).hasSize(StaticFilters.values().length + ExtensibleFilters.values().length);
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "API_TYPE", "HTTP_GATEWAY_RESPONSE_TIME", "MCP_PROXY_METHOD", "URI")
            .doesNotContain("HTTP_PATH", "HTTP_PATH_MAPPING");
    }

    @Test
    void should_narrow_to_logs_served_filters_when_signal_is_logs() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(Set.of(Signal.LOGS), Set.of()));

        // Every returned spec must declare LOGS; analytics-only filters (e.g. GATEWAY) drop out.
        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.signals()).contains(Signal.LOGS));
        assertThat(output.filters()).extracting(FilterSpec::name).contains("API", "URI").doesNotContain("GATEWAY", "HOST");
    }

    @Test
    void should_narrow_to_llm_relevant_filters_when_api_type_is_llm() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(Set.of(), Set.of(ApiType.LLM)));

        assertThat(output.filters()).allSatisfy(spec -> assertThat(spec.apiTypes()).contains(ApiType.LLM));
        assertThat(output.filters())
            .extracting(FilterSpec::name)
            .contains("API", "LLM_PROXY_MODEL")
            .doesNotContain("MESSAGE_SIZE", "EDGE_TYPE");
    }

    @Test
    void should_intersect_both_axes_independently() {
        var output = useCase.execute(new GetObservabilityFilterDefinitionsUseCase.Input(Set.of(Signal.ANALYTICS), Set.of(ApiType.MESSAGE)));

        List<String> names = output.filters().stream().map(FilterSpec::name).toList();
        assertThat(names).contains("MESSAGE_OPERATION_TYPE", "MESSAGE_SIZE", "API_TYPE");
        // URI is HTTP_PROXY/LLM/MCP only → excluded for MESSAGE; REQUEST_ID is LOGS-only → excluded for ANALYTICS.
        assertThat(names).doesNotContain("URI", "REQUEST_ID");
    }
}
