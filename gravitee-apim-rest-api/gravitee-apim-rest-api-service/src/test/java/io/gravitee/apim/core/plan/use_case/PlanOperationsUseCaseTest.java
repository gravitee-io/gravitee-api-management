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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import inmemory.ApiProductCrudServiceInMemory;
import inmemory.PlanCrudServiceInMemory;
import inmemory.PlanSearchQueryServiceInMemory;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.api_product.domain_service.ValidateApiProductPlanOAuth2ResourcesDomainService;
import io.gravitee.apim.core.api_product.model.ApiProduct;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.OAuth2PlanSecurityConfigurationReader;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.plan.use_case.PlanOperationsUseCase.Operation;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
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

    @Mock
    private PlanSearchQueryService planSearchQueryService;

    @Mock
    private PlanCrudService planCrudService;

    @Mock
    private DeletePlanDomainService deletePlanDomainService;

    @Mock
    private PublishPlanDomainService publishPlanDomainService;

    @Mock
    private ClosePlanDomainService closePlanDomainService;

    @Mock
    private DeprecatePlanDomainService deprecatePlanDomainService;

    @Mock
    private AuditInfo auditInfo;

    @Mock
    private ApiProductCrudService apiProductCrudService;

    private ValidateApiProductPlanOAuth2ResourcesDomainService validateApiProductPlanOAuth2ResourcesDomainService;

    private PlanOperationsUseCase planOperationsUseCase;

    @BeforeEach
    void setUp() {
        validateApiProductPlanOAuth2ResourcesDomainService = new ValidateApiProductPlanOAuth2ResourcesDomainService(
            new OAuth2PlanSecurityConfigurationReader()
        );
        planOperationsUseCase = new PlanOperationsUseCase(
            planSearchQueryService,
            planCrudService,
            deletePlanDomainService,
            publishPlanDomainService,
            closePlanDomainService,
            deprecatePlanDomainService,
            apiProductCrudService,
            validateApiProductPlanOAuth2ResourcesDomainService
        );
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
                .operation(Operation.DELETE)
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(plan);
            assertThat(output.plan().getId()).isEqualTo(PLAN_ID);

            verify(deletePlanDomainService).delete(eq(plan), eq(auditInfo));
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
                .operation(Operation.CLOSE)
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
            when(apiProductCrudService.get(API_PRODUCT_ID)).thenReturn(ApiProduct.builder().id(API_PRODUCT_ID).build());
            when(publishPlanDomainService.publish(eq(auditInfo), eq(PLAN_ID))).thenReturn(publishedPlan);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation(Operation.PUBLISH)
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(publishedPlan);

            verify(publishPlanDomainService).publish(eq(auditInfo), eq(PLAN_ID));
        }
    }

    @Nested
    class PublishOAuth2ApiProductPlan {

        private final PlanCrudServiceInMemory planCrudServiceInMemory = new PlanCrudServiceInMemory();
        private final PlanSearchQueryServiceInMemory planSearchQueryServiceInMemory = new PlanSearchQueryServiceInMemory(
            planCrudServiceInMemory
        );
        private final ApiProductCrudServiceInMemory apiProductCrudServiceInMemory = new ApiProductCrudServiceInMemory();

        @BeforeEach
        void setUpOAuth2PlanOperations() {
            planOperationsUseCase = new PlanOperationsUseCase(
                planSearchQueryServiceInMemory,
                planCrudServiceInMemory,
                deletePlanDomainService,
                publishPlanDomainService,
                closePlanDomainService,
                deprecatePlanDomainService,
                apiProductCrudServiceInMemory,
                validateApiProductPlanOAuth2ResourcesDomainService
            );
            apiProductCrudServiceInMemory.initWith(
                List.of(
                    ApiProduct.builder()
                        .id(API_PRODUCT_ID)
                        .name("Product")
                        .resources(List.of(Resource.builder().name("oauth").type("oauth2").configuration("{}").enabled(true).build()))
                        .build()
                )
            );
        }

        private Plan stagingOAuth2Plan(String oauthResource) {
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
                        .status(PlanStatus.STAGING)
                        .mode(PlanMode.STANDARD)
                        .security(
                            PlanSecurity.builder().type("oauth2").configuration("{\"oauthResource\":\"" + oauthResource + "\"}").build()
                        )
                        .build()
                )
                .build();
        }

        @Test
        void should_reject_publish_and_keep_plan_staging_when_oauth_resource_is_invalid() {
            planCrudServiceInMemory.initWith(List.of(stagingOAuth2Plan("missing")));

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation(Operation.PUBLISH)
                .auditInfo(auditInfo)
                .build();

            assertThatThrownBy(() -> planOperationsUseCase.execute(input))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("unknown resource 'missing'");

            assertThat(planCrudServiceInMemory.getById(PLAN_ID).getPlanStatus()).isEqualTo(PlanStatus.STAGING);
            verify(publishPlanDomainService, never()).publish(eq(auditInfo), eq(PLAN_ID));
        }

        @Test
        void should_publish_when_oauth_resource_is_valid() {
            Plan staging = stagingOAuth2Plan("oauth");
            Plan published = staging.toBuilder().build().setPlanStatus(PlanStatus.PUBLISHED);
            planCrudServiceInMemory.initWith(List.of(staging));
            when(publishPlanDomainService.publish(eq(auditInfo), eq(PLAN_ID))).thenReturn(published);

            var input = PlanOperationsUseCase.Input.builder()
                .planId(PLAN_ID)
                .referenceId(API_PRODUCT_ID)
                .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
                .operation(Operation.PUBLISH)
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output.plan().getPlanStatus()).isEqualTo(PlanStatus.PUBLISHED);
            verify(publishPlanDomainService).publish(eq(auditInfo), eq(PLAN_ID));
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
                .operation(Operation.DEPRECATE)
                .auditInfo(auditInfo)
                .build();

            var output = planOperationsUseCase.execute(input);

            assertThat(output).isNotNull();
            assertThat(output.plan()).isEqualTo(deprecatedPlan);

            verify(deprecatePlanDomainService).deprecate(eq(PLAN_ID), eq(auditInfo), eq(false));
        }
    }
}
