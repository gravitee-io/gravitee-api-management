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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.exception.UnsupportedPlanOperationException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanOperationsUseCaseTest {

    private static final String PLAN_ID = "plan-id";
    private static final String REFERENCE_ID = "ref-id";
    private static final String REFERENCE_TYPE_API_PRODUCT = GenericPlanEntity.ReferenceType.API_PRODUCT.name();

    private PlanSearchQueryService planSearchQueryService;
    private PlanCrudService planCrudService;
    private PublishPlanDomainService publishPlanDomainService;
    private ClosePlanDomainService closePlanDomainService;
    private DeprecatePlanDomainService deprecatePlanDomainService;

    private PlanOperationsUseCase useCase;

    private Plan plan;

    @BeforeEach
    void setUp() {
        planSearchQueryService = mock(PlanSearchQueryService.class);
        planCrudService = mock(PlanCrudService.class);
        publishPlanDomainService = mock(PublishPlanDomainService.class);
        closePlanDomainService = mock(ClosePlanDomainService.class);
        deprecatePlanDomainService = mock(DeprecatePlanDomainService.class);
        useCase = new PlanOperationsUseCase(
            planSearchQueryService,
            planCrudService,
            publishPlanDomainService,
            closePlanDomainService,
            deprecatePlanDomainService
        );
        plan = PlanFixtures.aPlanHttpV4()
            .toBuilder()
            .id(PLAN_ID)
            .referenceId(REFERENCE_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .build();
        when(
            planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(PLAN_ID, REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT)
        ).thenReturn(plan);
        GraviteeContext.setCurrentEnvironment("env-id");
        GraviteeContext.setCurrentOrganization("org-id");
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    private PlanOperationsUseCase.Input input(String operation) {
        return PlanOperationsUseCase.Input.builder()
            .planId(PLAN_ID)
            .referenceId(REFERENCE_ID)
            .referenceType(REFERENCE_TYPE_API_PRODUCT)
            .operation(operation)
            .auditInfo(AuditInfoFixtures.anAuditInfo("org", "env", "user"))
            .build();
    }

    @Nested
    class Delete {

        @Test
        void should_delete_plan_and_return_output() {
            var output = useCase.execute(input(PlanOperationsUseCase.Operation.DELETE.name()));

            assertThat(output.plan()).isEqualTo(plan);
            verify(planCrudService).delete(PLAN_ID);
        }
    }

    @Nested
    class Close {

        @Test
        void should_close_plan_and_return_output() {
            var output = useCase.execute(input(PlanOperationsUseCase.Operation.CLOSE.name()));

            assertThat(output.plan()).isEqualTo(plan);
            verify(closePlanDomainService).close(eq(PLAN_ID), any(AuditInfo.class));
        }
    }

    @Nested
    class Publish {

        @Test
        void should_publish_plan_and_return_output() {
            Plan publishedPlan = plan.toBuilder().id(PLAN_ID).build();
            when(publishPlanDomainService.publish(any(), eq(PLAN_ID))).thenReturn(publishedPlan);

            var output = useCase.execute(input(PlanOperationsUseCase.Operation.PUBLISH.name()));

            assertThat(output.plan()).isEqualTo(publishedPlan);
            verify(publishPlanDomainService).publish(any(), eq(PLAN_ID));
        }
    }

    @Nested
    class Deprecate {

        @Test
        void should_deprecate_plan_and_return_output() {
            var output = useCase.execute(input(PlanOperationsUseCase.Operation.DEPRECATE.name()));

            assertThat(output.plan()).isEqualTo(plan);
            verify(deprecatePlanDomainService).deprecate(eq(PLAN_ID), any(AuditInfo.class), eq(false));
        }
    }

    @Nested
    class UnsupportedOperation {

        @Test
        void should_throw_when_operation_is_unknown() {
            assertThatThrownBy(() -> useCase.execute(input("UNKNOWN")))
                .isInstanceOf(UnsupportedPlanOperationException.class)
                .hasMessageContaining("UNKNOWN");
        }

        @Test
        void should_throw_when_operation_is_empty() {
            assertThatThrownBy(() -> useCase.execute(input("")))
                .isInstanceOf(UnsupportedPlanOperationException.class)
                .hasMessageContaining("");
        }
    }

    @Nested
    class Validation {

        @Test
        void should_throw_PlanNotFoundException_when_plan_belongs_to_different_api_product() {
            Plan planOtherProduct = plan.toBuilder().referenceId("other-product-id").build();
            when(
                planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(PLAN_ID, REFERENCE_ID, REFERENCE_TYPE_API_PRODUCT)
            ).thenReturn(planOtherProduct);

            assertThatThrownBy(() -> useCase.execute(input(PlanOperationsUseCase.Operation.CLOSE.name())))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }
    }
}
