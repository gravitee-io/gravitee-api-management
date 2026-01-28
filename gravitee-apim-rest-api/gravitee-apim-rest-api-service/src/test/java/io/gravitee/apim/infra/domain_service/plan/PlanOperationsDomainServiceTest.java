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
package io.gravitee.apim.infra.domain_service.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.ClosePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeprecatePlanDomainService;
import io.gravitee.apim.core.plan.exception.InvalidPlanStatusForDeprecationException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PlanOperationsDomainServiceTest {

    private static final String PLAN_ID = "plan-id";
    private static final String API_ID = "api-id";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Mock
    PlanRepository planRepository;

    @Mock
    AuditService auditService;

    @Mock
    PlanCrudService planCrudService;

    @Mock
    AuditInfo auditInfo;

    @Mock
    SubscriptionQueryService subscriptionQueryService;

    @Mock
    AuditDomainService auditDomainService;

    @Mock
    ExecutionContext executionContext;

    @Mock
    CloseSubscriptionDomainService closeSubscriptionDomainService;

    PublishPlanDomainServiceImpl publicationsService;
    ClosePlanDomainService closePlanDomainService;
    DeprecatePlanDomainService deprecatePlanDomainService;

    @BeforeEach
    void setUp() {
        publicationsService = new PublishPlanDomainServiceImpl(planRepository, auditService);
        closePlanDomainService = new ClosePlanDomainService(
            planCrudService,
            subscriptionQueryService,
            closeSubscriptionDomainService,
            auditDomainService
        );
        deprecatePlanDomainService = new DeprecatePlanDomainService(planCrudService, auditDomainService);
    }

    private Plan apiProductPlanPublished() {
        return PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.PUBLISHED).build())
            .build();
    }

    @Test
    void closePlanForApiProduct_should_close_and_create_api_product_audit() {
        Plan publishedPlan = apiProductPlanPublished();
        when(planCrudService.getById(eq(PLAN_ID))).thenReturn(publishedPlan);
        when(subscriptionQueryService.findActiveSubscriptionsByPlan(eq(PLAN_ID))).thenReturn(Collections.emptyList());
        when(planCrudService.update(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        closePlanDomainService.close(PLAN_ID, auditInfo);

        verify(planCrudService).update(any(Plan.class));
        verify(auditDomainService).createApiProductAuditLog(any());
    }

    @Test
    void publish_should_throw_when_already_published() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> publicationsService.publish(executionContext, PLAN_ID)).isInstanceOf(PlanAlreadyPublishedException.class);
    }

    @Test
    void publish_should_throw_when_keyless_already_published() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            .referenceId(API_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS)
            .build();
        var existingKeyless = io.gravitee.repository.management.model.Plan.builder()
            .id("existing-keyless")
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            .referenceId(API_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS)
            .order(1)
            .build();

        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(
            planRepository.findByReferenceIdAndReferenceType(
                eq(API_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            )
        ).thenReturn(Set.of(existingKeyless));

        assertThatThrownBy(() -> publicationsService.publish(executionContext, PLAN_ID)).isInstanceOf(
            KeylessPlanAlreadyPublishedException.class
        );
    }

    @Test
    void publish_should_publish_api_product_plan_and_audit() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(0)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(
            planRepository.findByReferenceIdAndReferenceType(
                eq(API_PRODUCT_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
            )
        ).thenReturn(Set.of());
        when(planRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = publicationsService.publish(executionContext, PLAN_ID);

        assertThat(result.getId()).isEqualTo(PLAN_ID);
        assertThat(result.getOrder()).isEqualTo(1);
        verify(auditService).createApiAuditLog(eq(executionContext), any(AuditService.AuditLogData.class), eq(API_PRODUCT_ID));
    }

    @Test
    void publish_should_publish_api_plan_and_set_next_order() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(0)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .build();
        var alreadyPublished = io.gravitee.repository.management.model.Plan.builder()
            .id("published-1")
            .referenceId(API_ID)
            .referenceType(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(3)
            .build();

        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(
            planRepository.findByReferenceIdAndReferenceType(
                eq(API_ID),
                eq(io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
            )
        ).thenReturn(Set.of(alreadyPublished));
        when(planRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = publicationsService.publish(executionContext, PLAN_ID);

        assertThat(result.getId()).isEqualTo(PLAN_ID);
        assertThat(result.getOrder()).isEqualTo(4);
        verify(auditService).createApiAuditLog(eq(executionContext), any(AuditService.AuditLogData.class), eq(API_ID));
    }

    @Test
    void deprecateForApiProduct_should_throw_when_staging() {
        Plan stagingPlan = PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.STAGING).build())
            .build();
        when(planCrudService.getById(eq(PLAN_ID))).thenReturn(stagingPlan);

        assertThatThrownBy(() -> deprecatePlanDomainService.deprecate(PLAN_ID, auditInfo, false)).isInstanceOf(
            InvalidPlanStatusForDeprecationException.class
        );
    }

    @Test
    void deprecateForApiProduct_should_deprecate_and_create_api_product_audit() {
        when(planCrudService.getById(eq(PLAN_ID))).thenReturn(apiProductPlanPublished());
        when(planCrudService.update(any(Plan.class))).thenAnswer(inv -> inv.getArgument(0));

        deprecatePlanDomainService.deprecate(PLAN_ID, auditInfo, false);

        verify(planCrudService).update(any(Plan.class));
        verify(auditDomainService).createApiProductAuditLog(any());
    }

    @Test
    void deprecateForApiProduct_should_throw_when_already_deprecated() {
        Plan deprecatedPlan = PlanFixtures.HttpV4.anApiKey()
            .toBuilder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(GenericPlanEntity.ReferenceType.API_PRODUCT)
            .planDefinitionHttpV4(
                PlanFixtures.HttpV4.anApiKey().getPlanDefinitionHttpV4().toBuilder().status(PlanStatus.DEPRECATED).build()
            )
            .build();
        when(planCrudService.getById(eq(PLAN_ID))).thenReturn(deprecatedPlan);

        assertThatThrownBy(() -> deprecatePlanDomainService.deprecate(PLAN_ID, auditInfo, false)).isInstanceOf(
            InvalidPlanStatusForDeprecationException.class
        );
    }
}
