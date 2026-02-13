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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct;

import static io.gravitee.gateway.services.sync.process.common.model.SyncAction.DEPLOY;
import static io.gravitee.gateway.services.sync.process.common.model.SyncAction.UNDEPLOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApiProductPlanAppenderTest {

    private static final String API_PRODUCT_ID = "api-product-1";
    private static final String ENV_ID = "env-1";

    @Mock
    private PlanRepository planRepository;

    private ApiProductPlanAppender cut;

    @BeforeEach
    void setUp() {
        cut = new ApiProductPlanAppender(planRepository);
    }

    @Nested
    class EmptyInputTest {

        @Test
        void should_return_null_when_deployables_null() {
            List<ApiProductReactorDeployable> result = cut.appends(null, Set.of(ENV_ID));
            assertThat(result).isNull();
        }

        @Test
        void should_return_empty_when_deployables_empty() {
            List<ApiProductReactorDeployable> result = cut.appends(List.of(), Set.of(ENV_ID));
            assertThat(result).isEmpty();
        }

        @Test
        void should_return_deployables_unchanged_when_no_deploy_action() {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(UNDEPLOY)
                .build();

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), Set.of(ENV_ID));

            assertThat(result).hasSize(1).contains(deployable);
        }

        @Test
        void should_return_deployables_with_empty_plans_when_environment_empty() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    Set.of()
                )
            ).thenReturn(List.of());

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), Set.of());

            assertThat(result).hasSize(1).contains(deployable);
            assertThat(deployable.subscribablePlans()).isEmpty();
            assertThat(deployable.definitionPlans()).isEmpty();
        }

        @Test
        void should_return_deployables_with_empty_plans_when_environment_null() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    null
                )
            ).thenReturn(List.of());

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), null);

            assertThat(result).hasSize(1).contains(deployable);
            assertThat(deployable.subscribablePlans()).isEmpty();
            assertThat(deployable.definitionPlans()).isEmpty();
        }
    }

    @Nested
    class SuccessTest {

        @Test
        void should_populate_plans_for_deploy_deployables() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            Plan plan1 = createPlan("plan-1", API_PRODUCT_ID, Plan.Status.PUBLISHED);
            Plan plan2 = createPlan("plan-2", API_PRODUCT_ID, Plan.Status.DEPRECATED);

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    Set.of(ENV_ID)
                )
            ).thenReturn(List.of(plan1, plan2));

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), Set.of(ENV_ID));

            assertThat(result).hasSize(1);
            assertThat(deployable.subscribablePlans()).containsExactlyInAnyOrder("plan-1", "plan-2");
            assertThat(deployable.definitionPlans()).hasSize(2);
        }

        @Test
        void should_filter_out_staging_and_closed_plans_from_definition() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            Plan publishedPlan = createPlan("plan-published", API_PRODUCT_ID, Plan.Status.PUBLISHED);
            Plan stagingPlan = createPlan("plan-staging", API_PRODUCT_ID, Plan.Status.STAGING);
            Plan closedPlan = createPlan("plan-closed", API_PRODUCT_ID, Plan.Status.CLOSED);

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    Set.of(ENV_ID)
                )
            ).thenReturn(List.of(publishedPlan, stagingPlan, closedPlan));

            cut.appends(List.of(deployable), Set.of(ENV_ID));

            assertThat(deployable.subscribablePlans()).containsExactlyInAnyOrder("plan-published", "plan-staging", "plan-closed");
            assertThat(deployable.definitionPlans()).hasSize(1);
            assertThat(deployable.definitionPlans().get(0).getId()).isEqualTo("plan-published");
        }

        @Test
        void should_populate_plans_when_environment_empty_repository_returns_plans() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            Plan plan1 = createPlan("plan-1", API_PRODUCT_ID, Plan.Status.PUBLISHED);

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    Set.of()
                )
            ).thenReturn(List.of(plan1));

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), Set.of());

            assertThat(result).hasSize(1);
            assertThat(deployable.subscribablePlans()).containsExactly("plan-1");
            assertThat(deployable.definitionPlans()).hasSize(1);
        }

        @Test
        void should_skip_undeploy_deployables() throws TechnicalException {
            ApiProductReactorDeployable deployDeployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();
            ApiProductReactorDeployable undeployDeployable = ApiProductReactorDeployable.builder()
                .apiProductId("api-product-2")
                .syncAction(UNDEPLOY)
                .build();

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(
                    List.of(API_PRODUCT_ID),
                    Plan.PlanReferenceType.API_PRODUCT,
                    Set.of(ENV_ID)
                )
            ).thenReturn(List.of(createPlan("plan-1", API_PRODUCT_ID, Plan.Status.PUBLISHED)));

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployDeployable, undeployDeployable), Set.of(ENV_ID));

            assertThat(result).hasSize(2);
            assertThat(deployDeployable.subscribablePlans()).containsExactly("plan-1");
            assertThat(undeployDeployable.subscribablePlans()).isNullOrEmpty();
        }
    }

    @Nested
    class ErrorHandlingTest {

        @Test
        void should_set_empty_plans_on_repository_exception() throws TechnicalException {
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId(API_PRODUCT_ID)
                .syncAction(DEPLOY)
                .build();

            when(
                planRepository.findByReferenceIdsAndReferenceTypeAndEnvironment(any(), eq(Plan.PlanReferenceType.API_PRODUCT), any())
            ).thenThrow(new TechnicalException("DB error"));

            List<ApiProductReactorDeployable> result = cut.appends(List.of(deployable), Set.of(ENV_ID));

            assertThat(result).hasSize(1);
            assertThat(deployable.subscribablePlans()).isEmpty();
            assertThat(deployable.definitionPlans()).isEmpty();
        }
    }

    private Plan createPlan(String id, String referenceId, Plan.Status status) {
        Plan plan = new Plan();
        plan.setId(id);
        plan.setName("plan-" + id);
        plan.setReferenceId(referenceId);
        plan.setReferenceType(Plan.PlanReferenceType.API_PRODUCT);
        plan.setStatus(status);
        plan.setMode(Plan.PlanMode.STANDARD);
        plan.setSecurity(Plan.PlanSecurityType.API_KEY);
        return plan;
    }
}
