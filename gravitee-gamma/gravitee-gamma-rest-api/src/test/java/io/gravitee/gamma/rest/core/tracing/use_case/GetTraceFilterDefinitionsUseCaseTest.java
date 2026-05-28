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
package io.gravitee.gamma.rest.core.tracing.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.gamma.rest.core.tracing.model.FilterOperator;
import io.gravitee.gamma.rest.core.tracing.model.FilterType;
import io.gravitee.gamma.rest.core.tracing.model.TraceFilterSpec;
import io.gravitee.gamma.rest.core.tracing.port.service_provider.TraceFilterContributor;
import io.gravitee.gamma.rest.infra.adapter.SpiTraceFilterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link GetTraceFilterDefinitionsUseCase} against a {@link SpiTraceFilterRegistry}
 * constructed with hand-crafted contributors — sidesteps {@link java.util.ServiceLoader} entirely
 * so the test stays deterministic and independent of whatever's on the test classpath.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetTraceFilterDefinitionsUseCaseTest {

    private static final TraceFilterSpec COMMON_STATUS = filter("STATUS", "Status", null);
    private static final TraceFilterSpec COMMON_DURATION = filter("DURATION_NANOS", "Duration", null);
    private static final TraceFilterSpec AIM_LLM_MODEL = filter("LLM_MODEL", "LLM model", null);
    private static final TraceFilterSpec APIM_PLAN_ID = filter("PLAN_ID", "Plan", null);

    private final TraceFilterContributor commonContributor = contributor(null, List.of(COMMON_STATUS, COMMON_DURATION));
    private final TraceFilterContributor aimContributor = contributor("aim", List.of(AIM_LLM_MODEL));
    private final TraceFilterContributor apimContributor = contributor("apim", List.of(APIM_PLAN_ID));

    @Test
    void should_return_only_cross_module_filters_when_module_is_null() {
        GetTraceFilterDefinitionsUseCase useCase = useCaseWith(commonContributor, aimContributor, apimContributor);

        GetTraceFilterDefinitionsUseCase.Output output = useCase.execute(new GetTraceFilterDefinitionsUseCase.Input(null));

        assertThat(output.filters()).extracting(TraceFilterSpec::name).containsExactly("STATUS", "DURATION_NANOS");
    }

    @Test
    void should_union_cross_module_with_module_specific_filters_when_module_is_specified() {
        GetTraceFilterDefinitionsUseCase useCase = useCaseWith(commonContributor, aimContributor, apimContributor);

        GetTraceFilterDefinitionsUseCase.Output output = useCase.execute(new GetTraceFilterDefinitionsUseCase.Input("aim"));

        assertThat(output.filters()).extracting(TraceFilterSpec::name).containsExactly("STATUS", "DURATION_NANOS", "LLM_MODEL");
    }

    @Test
    void should_ignore_contributors_for_unmatched_modules() {
        GetTraceFilterDefinitionsUseCase useCase = useCaseWith(commonContributor, aimContributor, apimContributor);

        GetTraceFilterDefinitionsUseCase.Output output = useCase.execute(new GetTraceFilterDefinitionsUseCase.Input("aim"));

        assertThat(output.filters()).extracting(TraceFilterSpec::name).doesNotContain("PLAN_ID");
    }

    @Test
    void should_let_module_contributor_override_a_cross_module_filter_by_name() {
        TraceFilterSpec aimOverride = filter("STATUS", "AIM-specific status", null);
        GetTraceFilterDefinitionsUseCase useCase = useCaseWith(commonContributor, contributor("aim", List.of(aimOverride)));

        GetTraceFilterDefinitionsUseCase.Output output = useCase.execute(new GetTraceFilterDefinitionsUseCase.Input("aim"));

        // The override replaces in place, not append — order preserved by the cross-module entry's position.
        assertThat(output.filters())
            .extracting(TraceFilterSpec::name, TraceFilterSpec::label)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("STATUS", "AIM-specific status"),
                org.assertj.core.groups.Tuple.tuple("DURATION_NANOS", "Duration")
            );
    }

    @Test
    void should_return_empty_list_when_no_contributors_match() {
        GetTraceFilterDefinitionsUseCase useCase = useCaseWith(); // no contributors at all

        GetTraceFilterDefinitionsUseCase.Output output = useCase.execute(new GetTraceFilterDefinitionsUseCase.Input("aim"));

        assertThat(output.filters()).isEmpty();
    }

    private static GetTraceFilterDefinitionsUseCase useCaseWith(TraceFilterContributor... contributors) {
        return new GetTraceFilterDefinitionsUseCase(new SpiTraceFilterRegistry(List.of(contributors)));
    }

    private static TraceFilterSpec filter(String name, String label, List<String> enumValues) {
        return new TraceFilterSpec(name, label, FilterType.KEYWORD, List.of(FilterOperator.EQ), enumValues, null);
    }

    private static TraceFilterContributor contributor(String moduleId, List<TraceFilterSpec> filters) {
        return new TraceFilterContributor() {
            @Override
            public String moduleId() {
                return moduleId;
            }

            @Override
            public List<TraceFilterSpec> getFilters() {
                return filters;
            }
        };
    }
}
