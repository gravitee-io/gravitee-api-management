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

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.apim.core.plan.query_service.ApiProductPlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class UpdateApiProductPlanUseCaseTest {

    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";
    private static final String PLAN_ID = "plan-id";
    private static final String ORG_ID = "org-id";
    private static final String ENV_ID = "env-id";

    @Mock
    private UpdatePlanDomainService updatePlanDomainService;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private ApiProductCrudService apiProductCrudService;

    @Mock
    private ApiProductPlanSearchQueryService apiProductPlanSearchQueryService;

    private UpdateApiProductPlanUseCase updateApiProductPlanUseCase;

    @BeforeEach
    void setUp() {
        updateApiProductPlanUseCase = new UpdateApiProductPlanUseCase(
            updatePlanDomainService,
            apiProductPlanSearchQueryService,
            planCrudService,
            apiProductCrudService
        );
        GraviteeContext.fromExecutionContext(new io.gravitee.rest.api.service.common.ExecutionContext(ORG_ID, ENV_ID));
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class UpdatePlan {

        @Test
        void should_update_plan_successfully() {
            Plan existingPlan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();
            Plan updatedPlan = existingPlan.toBuilder().name("Updated name").build();
            ApiProduct apiProduct = ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).build();

            when(
                planCrudService.findByPlanIdAndReferenceIdAndReferenceType(
                    PLAN_ID,
                    API_PRODUCT_ID,
                    GenericPlanEntity.ReferenceType.API_PRODUCT.name()
                )
            ).thenReturn(Optional.of(existingPlan));
            when(apiProductCrudService.get(API_PRODUCT_ID)).thenReturn(apiProduct);
            when(updatePlanDomainService.updatePlanForApiProduct(any(), any(), eq(apiProduct), any())).thenReturn(updatedPlan);

            PlanUpdates planUpdates = PlanUpdates.builder()
                .id(PLAN_ID)
                .name("Updated name")
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .order(1)
                .build();
            AuditInfo auditInfo = new AuditInfo("user-id", "user-name", AuditActor.builder().build());
            var input = new UpdateApiProductPlanUseCase.Input(planUpdates, API_PRODUCT_ID, auditInfo);

            var output = updateApiProductPlanUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.updated()).isEqualTo(updatedPlan);
            assertThat(output.updated().getName()).isEqualTo("Updated name");

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(updatePlanDomainService).updatePlanForApiProduct(
                planCaptor.capture(),
                eq(java.util.Map.of()),
                eq(apiProduct),
                eq(auditInfo)
            );
            assertThat(planCaptor.getValue().getName()).isEqualTo("Updated name");
        }

        @Test
        void should_set_validation_from_input_when_plan_validation_is_null() {
            Plan existingPlan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .validation(null)
                .build();
            ApiProduct apiProduct = ApiProduct.builder().id(API_PRODUCT_ID).environmentId(ENV_ID).build();

            when(
                planCrudService.findByPlanIdAndReferenceIdAndReferenceType(
                    PLAN_ID,
                    API_PRODUCT_ID,
                    GenericPlanEntity.ReferenceType.API_PRODUCT.name()
                )
            ).thenReturn(Optional.of(existingPlan));
            when(apiProductCrudService.get(API_PRODUCT_ID)).thenReturn(apiProduct);
            when(updatePlanDomainService.updatePlanForApiProduct(any(), any(), eq(apiProduct), any())).thenAnswer(invocation ->
                invocation.getArgument(0)
            );

            PlanUpdates planUpdates = PlanUpdates.builder()
                .id(PLAN_ID)
                .name("Plan")
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .validation(Plan.PlanValidationType.MANUAL)
                .order(1)
                .build();
            var input = new UpdateApiProductPlanUseCase.Input(
                planUpdates,
                API_PRODUCT_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            updateApiProductPlanUseCase.execute(input);

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(updatePlanDomainService).updatePlanForApiProduct(planCaptor.capture(), any(), any(), any());
            assertThat(planCaptor.getValue().getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
        }
    }

    @Nested
    class PlanNotFound {

        @Test
        void should_throw_PlanNotFoundException_when_plan_belongs_to_another_api_product_in_search() {
            Plan existingPlan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId("another-api-product-id")
                .validation(null)
                .build();

            PlanUpdates planUpdates = PlanUpdates.builder().id(PLAN_ID).referenceId(API_PRODUCT_ID).build();
            var input = new UpdateApiProductPlanUseCase.Input(
                planUpdates,
                API_PRODUCT_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            assertThatThrownBy(() -> updateApiProductPlanUseCase.execute(input))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }

        @Test
        void should_throw_PlanNotFoundException_when_plan_not_found_in_crud_service() {
            Plan existingPlan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .validation(null)
                .build();

            when(
                planCrudService.findByPlanIdAndReferenceIdAndReferenceType(
                    PLAN_ID,
                    API_PRODUCT_ID,
                    GenericPlanEntity.ReferenceType.API_PRODUCT.name()
                )
            ).thenReturn(Optional.empty());

            PlanUpdates planUpdates = PlanUpdates.builder().id(PLAN_ID).referenceId(API_PRODUCT_ID).build();
            var input = new UpdateApiProductPlanUseCase.Input(
                planUpdates,
                API_PRODUCT_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            assertThatThrownBy(() -> updateApiProductPlanUseCase.execute(input))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }

        @Test
        void should_throw_PlanNotFoundException_when_plan_entity_from_crud_has_different_reference_id() {
            Plan existingPlan = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .referenceId(API_PRODUCT_ID)
                .validation(null)
                .build();
            Plan planFromCrud = PlanFixtures.aPlanHttpV4()
                .toBuilder()
                .id(PLAN_ID)
                .referenceId("other-api-product-id")
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .build();

            when(
                planCrudService.findByPlanIdAndReferenceIdAndReferenceType(
                    PLAN_ID,
                    API_PRODUCT_ID,
                    GenericPlanEntity.ReferenceType.API_PRODUCT.name()
                )
            ).thenReturn(Optional.of(planFromCrud));

            PlanUpdates planUpdates = PlanUpdates.builder().id(PLAN_ID).referenceId(API_PRODUCT_ID).build();
            var input = new UpdateApiProductPlanUseCase.Input(
                planUpdates,
                API_PRODUCT_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            assertThatThrownBy(() -> updateApiProductPlanUseCase.execute(input))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }
    }
}
