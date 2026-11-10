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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductPlanOAuth2ResourcesDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.OAuth2PlanSecurityConfigurationReader;
import io.gravitee.apim.core.plan.domain_service.PlanExcludedGroupsDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.List;
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
    private PlanExcludedGroupsDomainService planExcludedGroupsDomainService;

    private ValidateApiProductPlanOAuth2ResourcesDomainService validateApiProductPlanOAuth2ResourcesDomainService;

    private UpdateApiProductPlanUseCase updatePlanUseCase;

    @BeforeEach
    void setUp() {
        validateApiProductPlanOAuth2ResourcesDomainService = new ValidateApiProductPlanOAuth2ResourcesDomainService(
            new OAuth2PlanSecurityConfigurationReader()
        );
        updatePlanUseCase = new UpdateApiProductPlanUseCase(
            updatePlanDomainService,
            planCrudService,
            apiProductCrudService,
            planExcludedGroupsDomainService,
            validateApiProductPlanOAuth2ResourcesDomainService
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

            var output = updatePlanUseCase.execute(input);

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

            updatePlanUseCase.execute(input);

            ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
            verify(updatePlanDomainService).updatePlanForApiProduct(planCaptor.capture(), any(), any(), any());
            assertThat(planCaptor.getValue().getValidation()).isEqualTo(Plan.PlanValidationType.MANUAL);
        }
    }

    @Nested
    class OAuth2PublishedPlanUpdate {

        private final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
        private final ApiProductCrudServiceInMemory apiProductCrudServiceInMemory = new ApiProductCrudServiceInMemory();

        @BeforeEach
        void setUpInMemory() {
            updatePlanUseCase = new UpdateApiProductPlanUseCase(
                updatePlanDomainService,
                planCrudServiceInMemory,
                apiProductCrudServiceInMemory,
                planExcludedGroupsDomainService,
                validateApiProductPlanOAuth2ResourcesDomainService
            );
            apiProductCrudServiceInMemory.initWith(
                List.of(
                    ApiProduct.builder()
                        .id(API_PRODUCT_ID)
                        .environmentId(ENV_ID)
                        .resources(List.of(Resource.builder().name("oauth").type("oauth2").configuration("{}").enabled(true).build()))
                        .build()
                )
            );
        }

        private Plan publishedOAuth2Plan() {
            return Plan.builder()
                .id(PLAN_ID)
                .name("OAuth plan")
                .definitionVersion(DefinitionVersion.V4)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .planDefinitionHttpV4(
                    io.gravitee.definition.model.v4.plan.Plan.builder()
                        .id(PLAN_ID)
                        .name("OAuth plan")
                        .status(PlanStatus.PUBLISHED)
                        .mode(PlanMode.STANDARD)
                        .security(PlanSecurity.builder().type("oauth2").configuration("{\"oauthResource\":\"oauth\"}").build())
                        .build()
                )
                .build();
        }

        @Test
        void should_reject_update_and_leave_stored_plan_unchanged_when_oauth_resource_is_invalid() {
            planCrudServiceInMemory.initWith(List.of(publishedOAuth2Plan()));

            PlanUpdates planUpdates = PlanUpdates.builder()
                .id(PLAN_ID)
                .name("OAuth plan")
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .securityConfiguration("{\"oauthResource\":\"missing\"}")
                .order(1)
                .build();

            var input = new UpdateApiProductPlanUseCase.Input(
                planUpdates,
                API_PRODUCT_ID,
                new AuditInfo("user-id", "user-name", AuditActor.builder().build())
            );

            assertThatThrownBy(() -> updatePlanUseCase.execute(input))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("unknown resource 'missing'");

            assertThat(planCrudServiceInMemory.getById(PLAN_ID).getPlanSecurity().getConfiguration()).contains("oauth");
            verify(updatePlanDomainService, never()).updatePlanForApiProduct(any(), any(), any(), any());
        }

        @Test
        void should_allow_staging_oauth2_plan_update_without_resource_validation() {
            Plan staging = publishedOAuth2Plan()
                .toBuilder()
                .planDefinitionHttpV4(publishedOAuth2Plan().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build())
                .build();
            planCrudServiceInMemory.initWith(List.of(staging));
            ApiProduct apiProduct = apiProductCrudServiceInMemory.get(API_PRODUCT_ID);
            when(updatePlanDomainService.updatePlanForApiProduct(any(), any(), eq(apiProduct), any())).thenAnswer(invocation ->
                invocation.getArgument(0)
            );

            PlanUpdates planUpdates = PlanUpdates.builder()
                .id(PLAN_ID)
                .name("OAuth plan")
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .securityConfiguration("{\"oauthResource\":\"missing\"}")
                .order(1)
                .build();

            updatePlanUseCase.execute(
                new UpdateApiProductPlanUseCase.Input(
                    planUpdates,
                    API_PRODUCT_ID,
                    new AuditInfo("user-id", "user-name", AuditActor.builder().build())
                )
            );

            verify(updatePlanDomainService).updatePlanForApiProduct(any(), any(), eq(apiProduct), any());
        }
    }

    @Nested
    class PlanNotFound {

        @Test
        void should_throw_PlanNotFoundException_when_plan_not_found() {
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

            assertThatThrownBy(() -> updatePlanUseCase.execute(input))
                .isInstanceOf(PlanNotFoundException.class)
                .hasMessageContaining(PLAN_ID);
        }
    }
}
