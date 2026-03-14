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
package io.gravitee.apim.core.plan.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
class PlanOperationsUseCaseTest {

    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String PLAN_ID = "plan-id";
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    @Mock
    private PlanSearchQueryService planSearchQueryService;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private PublishPlanDomainService publishPlanDomainService;

    @Mock
    private ClosePlanDomainService closePlanDomainService;

    @Mock
    private DeprecatePlanDomainService deprecatePlanDomainService;

    @Mock
    private AuditInfo auditInfo;

    private PlanOperationsUseCase planOperationsUseCase;

    @BeforeEach
    void setUp() {
        planOperationsUseCase = new PlanOperationsUseCase(
            planSearchQueryService,
            planCrudService,
            publishPlanDomainService,
            closePlanDomainService,
            deprecatePlanDomainService
        );
        GraviteeContext.fromExecutionContext(new io.gravitee.rest.api.service.common.ExecutionContext(ORG_ID, ENV_ID));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    private Plan planForApiProduct() {
        return PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .referenceId(API_PRODUCT_ID)
            .build();
    }

    @Nested
    class DeleteOperation {

        @Test
        void should_delete_plan_and_return_plan() {
            Plan plan = planForApiProduct();
            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation("DELETE")
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(plan);
            assertThat(output.plan().getId()).isEqualTo(PLAN_ID);

            verify(planCrudService).delete(eq(PLAN_ID));
        }
    }

    @Nested
    class CloseOperation {

        @Test
        void should_close_plan_and_return_updated_plan() {
            Plan plan = planForApiProduct();
            Plan closedPlan = planForApiProduct();

            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);
            doNothing().when(closePlanDomainService).close(eq(PLAN_ID), eq(auditInfo));
            when(planCrudService.getById(PLAN_ID)).thenReturn(closedPlan);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation("CLOSE")
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(closedPlan);

            verify(closePlanDomainService).close(eq(PLAN_ID), eq(auditInfo));
        }
    }

    @Nested
    class PublishOperation {

        @Test
        void should_publish_plan_and_return_updated_plan() {
            Plan plan = planForApiProduct();
            Plan publishedPlan = planForApiProduct();

            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);
            when(publishPlanDomainService.publish(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(publishedPlan);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation("PUBLISH")
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(publishedPlan);

            verify(publishPlanDomainService).publish(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        }
    }

    @Nested
    class DeprecateOperation {

        @Test
        void should_deprecate_plan_and_return_updated_plan() {
            Plan plan = planForApiProduct();
            Plan deprecatedPlan = planForApiProduct();

            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                    eq(PLAN_ID),
                    eq(API_PRODUCT_ID),
                    eq(GenericPlanEntity.ReferenceType.API_PRODUCT)
                )
            ).thenReturn(plan);
            doNothing().when(deprecatePlanDomainService).deprecate(eq(PLAN_ID), eq(auditInfo), eq(false));
            when(planCrudService.getById(PLAN_ID)).thenReturn(deprecatedPlan);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation("DEPRECATE")
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(deprecatedPlan);

            verify(deprecatePlanDomainService).deprecate(eq(PLAN_ID), eq(auditInfo), eq(false));
        }
    }
}
