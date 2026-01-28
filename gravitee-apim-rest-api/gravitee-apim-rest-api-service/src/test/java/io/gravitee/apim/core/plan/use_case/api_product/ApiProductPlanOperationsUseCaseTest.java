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
package io.gravitee.apim.core.plan.use_case.api_product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.PlanModelFixtures;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ApiProductPlanOperationsUseCaseTest {

    private static final String API_PRODUCT_ID = "api-product-id";
    private static final String PLAN_ID = "plan-id";
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private PlanService planService;

    private ApiProductPlanOperationsUseCase apiProductPlanOperationsUseCase;

    @BeforeEach
    void setUp() {
        apiProductPlanOperationsUseCase = new ApiProductPlanOperationsUseCase(planSearchService, planService);
        GraviteeContext.fromExecutionContext(new io.gravitee.rest.api.service.common.ExecutionContext(ORG_ID, ENV_ID));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    private PlanEntity planEntityForApiProduct() {
        return PlanModelFixtures.aPlanEntityV4()
            .toBuilder()
            .id(PLAN_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .referenceId(API_PRODUCT_ID)
            .build();
    }

    @Nested
    class DeleteOperation {

        @Test
        void should_delete_plan_and_return_plan_entity() {
            PlanEntity plan = planEntityForApiProduct();
            when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);

            var input = ApiProductPlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .apiProductId(API_PRODUCT_ID)
                .operation("DELETE")
                .build();

            var output = apiProductPlanOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.planEntity()).isEqualTo(plan);
            assertThat(output.planEntity().getId()).isEqualTo(PLAN_ID);

            verify(planService).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        }
    }

    @Nested
    class CloseOperation {

        @Test
        void should_close_plan_and_return_updated_entity() {
            PlanEntity plan = planEntityForApiProduct();
            PlanEntity closedPlan = planEntityForApiProduct();

            when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);
            when(planService.closePlanForApiProduct(any(), eq(PLAN_ID))).thenReturn(closedPlan);

            var input = ApiProductPlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .apiProductId(API_PRODUCT_ID)
                .operation("CLOSE")
                .build();

            var output = apiProductPlanOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.planEntity()).isEqualTo(closedPlan);

            verify(planService).closePlanForApiProduct(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        }
    }

    @Nested
    class PublishOperation {

        @Test
        void should_publish_plan_and_return_updated_entity() {
            PlanEntity plan = planEntityForApiProduct();
            PlanEntity publishedPlan = planEntityForApiProduct();

            when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);
            when(planService.publish(any(), eq(PLAN_ID))).thenReturn(publishedPlan);

            var input = ApiProductPlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .apiProductId(API_PRODUCT_ID)
                .operation("PUBLISH")
                .build();

            var output = apiProductPlanOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.planEntity()).isEqualTo(publishedPlan);

            verify(planService).publish(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        }
    }

    @Nested
    class DeprecateOperation {

        @Test
        void should_deprecate_plan_and_return_updated_entity() {
            PlanEntity plan = planEntityForApiProduct();
            PlanEntity deprecatedPlan = planEntityForApiProduct();

            when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(plan);
            when(planService.deprecate(any(), eq(PLAN_ID))).thenReturn(deprecatedPlan);

            var input = ApiProductPlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .apiProductId(API_PRODUCT_ID)
                .operation("DEPRECATE")
                .build();

            var output = apiProductPlanOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.planEntity()).isEqualTo(deprecatedPlan);

            verify(planService).deprecate(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        }
    }

    @Nested
    class PlanNotFound {

        @Test
        void should_throw_PlanNotFoundException_when_plan_belongs_to_another_api_product() {
            PlanEntity planFromOtherProduct = PlanModelFixtures.aPlanEntityV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId("other-api-product-id")
                .build();

            when(planSearchService.findByPlanIdIdForApiProduct(any(), eq(PLAN_ID), eq(API_PRODUCT_ID))).thenReturn(planFromOtherProduct);

            var input = ApiProductPlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .apiProductId(API_PRODUCT_ID)
                .operation("CLOSE")
                .build();

            assertThatThrownBy(() -> apiProductPlanOperationsUseCase.execute(input))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }
    }
}
