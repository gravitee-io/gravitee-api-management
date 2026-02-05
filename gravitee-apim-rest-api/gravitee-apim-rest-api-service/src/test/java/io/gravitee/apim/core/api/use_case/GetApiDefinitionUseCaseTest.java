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
package io.gravitee.apim.core.api.use_case;

import static fixtures.core.model.ApiFixtures.aMessageApiV4;
import static fixtures.core.model.ApiFixtures.aNativeApi;
import static fixtures.core.model.ApiFixtures.aProxyApiV2;
import static fixtures.core.model.PlanFixtures.aPlanHttpV4;
import static fixtures.core.model.PlanFixtures.aPlanNativeV4;
import static fixtures.core.model.PlanFixtures.aPlanV2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import inmemory.ApiCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetApiDefinitionUseCaseTest {

    private final String API_ID = "api-id";
    private final String ENV_ID = "env-id";

    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private final FlowCrudServiceInMemory flowCrudServiceInMemory = new FlowCrudServiceInMemory();
    private final PlanQueryServiceInMemory planQueryServiceInMemory = new PlanQueryServiceInMemory();

    private GetApiDefinitionUseCase getApiDefinitionUseCase;

    @BeforeEach
    void setUp() {
        getApiDefinitionUseCase = new GetApiDefinitionUseCase(apiCrudServiceInMemory, flowCrudServiceInMemory, planQueryServiceInMemory);
    }

    @Nested
    class WithHttpV4Api {

        private final Api API = aMessageApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_api_definition_v4() {
            // Given
            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinitionHttpV4());
        }

        @Test
        void should_return_api_definition_with_flows() {
            // Given
            //   Api flow
            List<Flow> flows = List.of(Flow.builder().name("flow").selectors(List.of(new HttpSelector())).build());
            flowCrudServiceInMemory.saveApiFlows(API_ID, flows);

            //   Plan flow
            var planPublished = aPlanHttpV4().setPlanId("plan-push-id").toBuilder().referenceId(API_ID).build();
            planPublished.setPlanStatus(PlanStatus.PUBLISHED);
            var planDeprecated = aPlanHttpV4().setPlanId("plan-deprecate-id").toBuilder().referenceId(API_ID).build();
            planDeprecated.setPlanStatus(PlanStatus.DEPRECATED);
            var planStaging = aPlanHttpV4().setPlanId("plan-staging-id").toBuilder().referenceId(API_ID).build();
            planStaging.setPlanStatus(PlanStatus.STAGING);
            planQueryServiceInMemory.initWith(List.of(planPublished, planDeprecated, planStaging));

            List<Flow> planFlows = List.of(Flow.builder().name("plan-flow").selectors(List.of(new HttpSelector())).build());
            flowCrudServiceInMemory.savePlanFlows(planPublished.getId(), planFlows);
            flowCrudServiceInMemory.savePlanFlows(planDeprecated.getId(), planFlows);
            flowCrudServiceInMemory.savePlanFlows(planStaging.getId(), planFlows);

            apiCrudServiceInMemory.create(API);

            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinitionHttpV4());
            if (output.apiDefinition() instanceof io.gravitee.definition.model.v4.Api definition) {
                assertThat(definition.getFlows()).isEqualTo(flows);
                assertThat(definition.getPlans()).hasSize(2);
                assertThat(definition.getPlans().getFirst().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getFirst().getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                assertThat(definition.getPlans().getLast().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getLast().getStatus()).isEqualTo(PlanStatus.DEPRECATED);
            } else {
                fail("Unexpected type of api definition");
            }
        }
    }

    @Nested
    class WithNativeV4Api {

        private final Api API = aNativeApi().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_api_definition_v4() {
            // Given
            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertNotNull(output);
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinitionNativeV4());
        }

        @Test
        void should_return_api_definition_with_flows() {
            // Given
            //   Api flow
            List<NativeFlow> flows = List.of(NativeFlow.builder().name("flow").build());
            flowCrudServiceInMemory.saveNativeApiFlows(API_ID, flows);

            //   Plan flow
            var planPublished = aPlanNativeV4().setPlanId("plan-push-id").toBuilder().referenceId(API_ID).build();
            planPublished.setPlanStatus(PlanStatus.PUBLISHED);
            var planDeprecated = aPlanNativeV4().setPlanId("plan-deprecate-id").toBuilder().referenceId(API_ID).build();
            planDeprecated.setPlanStatus(PlanStatus.DEPRECATED);
            var planStaging = aPlanNativeV4().setPlanId("plan-staging-id").toBuilder().referenceId(API_ID).build();
            planStaging.setPlanStatus(PlanStatus.STAGING);
            planQueryServiceInMemory.initWith(List.of(planPublished, planDeprecated, planStaging));

            List<NativeFlow> planFlows = List.of(NativeFlow.builder().name("plan-flow").build());
            flowCrudServiceInMemory.saveNativePlanFlows(planPublished.getId(), planFlows);
            flowCrudServiceInMemory.saveNativePlanFlows(planDeprecated.getId(), planFlows);
            flowCrudServiceInMemory.saveNativePlanFlows(planStaging.getId(), planFlows);

            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinitionNativeV4());
            if (output.apiDefinition() instanceof NativeApi definition) {
                assertThat(definition.getFlows()).isEqualTo(flows);
                assertThat(definition.getPlans()).hasSize(2);
                assertThat(definition.getPlans().getFirst().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getFirst().getStatus()).isEqualTo(PlanStatus.PUBLISHED);
                assertThat(definition.getPlans().getLast().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getLast().getStatus()).isEqualTo(PlanStatus.DEPRECATED);
            } else {
                fail("Unexpected type of api definition");
            }
        }
    }

    @Nested
    class WithV2Api {

        private final Api API = aProxyApiV2().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_api_definition_v2() {
            // Given
            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinition());
        }

        @Test
        void should_return_api_definition_with_flows() {
            // Given
            //   Api flow
            var flows = List.of(io.gravitee.definition.model.flow.Flow.builder().name("flow").build());
            flowCrudServiceInMemory.saveApiFlowsV2(API_ID, flows);

            //   Plan flow
            var planPublished = aPlanV2().setPlanId("plan-push-id").toBuilder().referenceId(API_ID).build();
            planPublished.setPlanStatus(PlanStatus.PUBLISHED);
            var planDeprecated = aPlanV2().setPlanId("plan-deprecate-id").toBuilder().referenceId(API_ID).build();
            planDeprecated.setPlanStatus(PlanStatus.DEPRECATED);
            var planStaging = aPlanV2().setPlanId("plan-staging-id").toBuilder().referenceId(API_ID).build();
            planStaging.setPlanStatus(PlanStatus.STAGING);
            planQueryServiceInMemory.initWith(List.of(planPublished, planDeprecated, planStaging));

            var planFlows = List.of(io.gravitee.definition.model.flow.Flow.builder().name("plan-flow").build());
            flowCrudServiceInMemory.savePlanFlowsV2(planPublished.getId(), planFlows);
            flowCrudServiceInMemory.savePlanFlowsV2(planDeprecated.getId(), planFlows);
            flowCrudServiceInMemory.savePlanFlowsV2(planStaging.getId(), planFlows);

            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertThat(output.apiDefinition()).isEqualTo(API.getApiDefinition());

            if (output.apiDefinition() instanceof io.gravitee.definition.model.Api definition) {
                assertThat(definition.getFlows()).isEqualTo(flows);
                assertThat(definition.getPlans()).hasSize(2);
                assertThat(definition.getPlans().getFirst().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getFirst().getStatus()).isEqualTo("PUBLISHED");
                assertThat(definition.getPlans().getLast().getFlows()).isEqualTo(planFlows);
                assertThat(definition.getPlans().getLast().getStatus()).isEqualTo("DEPRECATED");
            } else {
                fail("Unexpected type of api definition");
            }
        }
    }
}
