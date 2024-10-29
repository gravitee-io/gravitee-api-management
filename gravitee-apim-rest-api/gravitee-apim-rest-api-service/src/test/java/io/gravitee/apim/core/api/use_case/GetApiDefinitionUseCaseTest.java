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
import static fixtures.core.model.ApiFixtures.aProxyApiV2;
import static fixtures.core.model.PlanFixtures.aPlanHttpV4;
import static fixtures.core.model.PlanFixtures.aPlanV2;
import static org.junit.jupiter.api.Assertions.*;

import inmemory.ApiCrudServiceInMemory;
import inmemory.FlowCrudServiceInMemory;
import inmemory.PlanQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
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
    class WithV4Api {

        private final Api API = aMessageApiV4().toBuilder().id(API_ID).environmentId(ENV_ID).build();

        @Test
        void should_return_api_definition_v4() {
            // Given
            apiCrudServiceInMemory.create(API);

            // When
            var output = getApiDefinitionUseCase.execute(new GetApiDefinitionUseCase.Input(API_ID));

            // Then
            assertNotNull(output);
            assertEquals(API.getApiDefinitionHttpV4(), output.apiDefinitionV4());
        }

        @Test
        void should_return_api_definition_with_flows() {
            // Given
            //   Api flow
            List<Flow> flows = List.of(Flow.builder().name("flow").selectors(List.of(new HttpSelector())).build());
            flowCrudServiceInMemory.saveApiFlows(API_ID, flows);

            //   Plan flow
            var planPublished = aPlanHttpV4().setPlanId("plan-push-id").toBuilder().apiId(API_ID).build();
            planPublished.setPlanStatus(PlanStatus.PUBLISHED);
            var planDeprecated = aPlanHttpV4().setPlanId("plan-deprecate-id").toBuilder().apiId(API_ID).build();
            planDeprecated.setPlanStatus(PlanStatus.DEPRECATED);
            var planStaging = aPlanHttpV4().setPlanId("plan-staging-id").toBuilder().apiId(API_ID).build();
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
            assertNotNull(output);
            assertEquals(API.getApiDefinitionHttpV4(), output.apiDefinitionV4());
            assertEquals(flows, output.apiDefinitionV4().getFlows());
            assertEquals(2, output.apiDefinitionV4().getPlans().size());
            assertEquals(planFlows, output.apiDefinitionV4().getPlans().get(0).getFlows());
            assertEquals(PlanStatus.PUBLISHED, output.apiDefinitionV4().getPlans().get(0).getStatus());
            assertEquals(planFlows, output.apiDefinitionV4().getPlans().get(1).getFlows());
            assertEquals(PlanStatus.DEPRECATED, output.apiDefinitionV4().getPlans().get(1).getStatus());
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
            assertNotNull(output);
            assertEquals(API.getApiDefinition(), output.apiDefinition());
        }

        @Test
        void should_return_api_definition_with_flows() {
            // Given
            //   Api flow
            var flows = List.of(io.gravitee.definition.model.flow.Flow.builder().name("flow").build());
            flowCrudServiceInMemory.saveApiFlowsV2(API_ID, flows);

            //   Plan flow
            var planPublished = aPlanV2().setPlanId("plan-push-id").toBuilder().apiId(API_ID).build();
            planPublished.setPlanStatus(PlanStatus.PUBLISHED);
            var planDeprecated = aPlanV2().setPlanId("plan-deprecate-id").toBuilder().apiId(API_ID).build();
            planDeprecated.setPlanStatus(PlanStatus.DEPRECATED);
            var planStaging = aPlanV2().setPlanId("plan-staging-id").toBuilder().apiId(API_ID).build();
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
            assertNotNull(output);
            assertEquals(API.getApiDefinition(), output.apiDefinition());
            assertEquals(flows, output.apiDefinition().getFlows());
            assertEquals(2, output.apiDefinition().getPlans().size());
            assertEquals(planFlows, output.apiDefinition().getPlans().get(0).getFlows());
            assertEquals("PUBLISHED", output.apiDefinition().getPlans().get(0).getStatus());
            assertEquals(planFlows, output.apiDefinition().getPlans().get(1).getFlows());
            assertEquals("DEPRECATED", output.apiDefinition().getPlans().get(1).getStatus());
        }
    }
}
