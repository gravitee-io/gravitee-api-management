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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.PlanReferenceType;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
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
class PlanOperationsDomainServiceImplTest {

    private static final String PLAN_ID = "plan-id";
    private static final String API_ID = "api-id";
    private static final String API_PRODUCT_ID = "api-product-id";

    @Mock
    PlanRepository planRepository;

    @Mock
    AuditService auditService;

    @Mock
    ExecutionContext executionContext;

    PlanOperationsDomainServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlanOperationsDomainServiceImpl(planRepository, auditService);
    }

    @Test
    void closePlanForApiProduct_should_throw_when_plan_not_found() throws Exception {
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.closePlanForApiProduct(executionContext, PLAN_ID)).isInstanceOf(PlanNotFoundException.class);

        verify(planRepository).findById(eq(PLAN_ID));
        verify(planRepository, never()).update(any());
    }

    @Test
    void closePlanForApiProduct_should_throw_when_already_closed() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .api(API_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(PlanReferenceType.API_PRODUCT)
            .status(io.gravitee.repository.management.model.Plan.Status.CLOSED)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.closePlanForApiProduct(executionContext, PLAN_ID)).isInstanceOf(PlanAlreadyClosedException.class);

        verify(planRepository).findById(eq(PLAN_ID));
        verify(planRepository, never()).update(any());
    }

    @Test
    void closePlanForApiProduct_should_close_a_plan_and_reorder() throws Exception {
        var toClose = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(PlanReferenceType.API_PRODUCT)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(1)
            .build();

        var publishedAfter = io.gravitee.repository.management.model.Plan.builder()
            .id("plan-2")
            .referenceId(API_PRODUCT_ID)
            .referenceType(PlanReferenceType.API_PRODUCT)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(2)
            .build();

        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(toClose));
        when(planRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));
        when(planRepository.findByReferenceIdAndReferenceType(eq(API_PRODUCT_ID), eq(PlanReferenceType.API_PRODUCT))).thenReturn(
            Set.of(toClose, publishedAfter)
        );

        var result = service.closePlanForApiProduct(executionContext, PLAN_ID);

        assertThat(result.getId()).isEqualTo(PLAN_ID);
        verify(planRepository).update(any());
        verify(auditService).createApiProductAuditLog(eq(executionContext), any(AuditService.AuditLogData.class), eq(API_PRODUCT_ID));
        verify(planRepository).updateOrder(eq("plan-2"), eq(1));
    }

    @Test
    void closePlanForApiProduct_should_wrap_technical_exception() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_PRODUCT_ID)
            .referenceType(PlanReferenceType.API_PRODUCT)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenThrow(new TechnicalException("boom"));

        assertThatThrownBy(() -> service.closePlanForApiProduct(executionContext, PLAN_ID)).isInstanceOf(
            TechnicalManagementException.class
        );
    }

    @Test
    void publish_should_throw_when_already_published() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .api(API_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.publish(executionContext, PLAN_ID)).isInstanceOf(PlanAlreadyPublishedException.class);
    }

    @Test
    void publish_should_throw_when_keyless_already_published() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceType(PlanReferenceType.API)
            .referenceId(API_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS)
            .build();
        var existingKeyless = io.gravitee.repository.management.model.Plan.builder()
            .id("existing-keyless")
            .referenceType(PlanReferenceType.API)
            .referenceId(API_ID)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS)
            .order(1)
            .build();

        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(planRepository.findByReferenceIdAndReferenceType(eq(API_ID), eq(PlanReferenceType.API))).thenReturn(Set.of(existingKeyless));

        assertThatThrownBy(() -> service.publish(executionContext, PLAN_ID)).isInstanceOf(KeylessPlanAlreadyPublishedException.class);
    }

    @Test
    void publish_should_publish_and_set_next_order() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_ID)
            .referenceType(PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(0)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .build();
        var alreadyPublished = io.gravitee.repository.management.model.Plan.builder()
            .id("published-1")
            .referenceId(API_ID)
            .referenceType(PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .order(3)
            .build();

        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(planRepository.findByReferenceIdAndReferenceType(eq(API_ID), eq(PlanReferenceType.API))).thenReturn(Set.of(alreadyPublished));
        when(planRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.publish(executionContext, PLAN_ID);

        assertThat(result.getId()).isEqualTo(PLAN_ID);
        // PlanAdapter maps repository order => core order; ensure it was set after existing published plan (3 -> 4)
        assertThat(result.getOrder()).isEqualTo(4);
        verify(auditService).createApiAuditLog(eq(executionContext), any(AuditService.AuditLogData.class), eq(API_ID));
    }

    @Test
    void deprecate_should_throw_when_staging() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_ID)
            .referenceType(PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.STAGING)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.deprecate(executionContext, PLAN_ID)).isInstanceOf(PlanNotYetPublishedException.class);
    }

    @Test
    void deprecate_should_deprecate_and_audit() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_ID)
            .referenceType(PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.PUBLISHED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .definitionVersion(DefinitionVersion.V4)
            .apiType(ApiType.PROXY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));
        when(planRepository.update(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.deprecate(executionContext, PLAN_ID);

        assertThat(result.getId()).isEqualTo(PLAN_ID);
        verify(auditService).createApiAuditLog(eq(executionContext), any(AuditService.AuditLogData.class), eq(API_ID));
    }

    @Test
    void deprecate_should_throw_when_already_deprecated() throws Exception {
        var plan = io.gravitee.repository.management.model.Plan.builder()
            .id(PLAN_ID)
            .referenceId(API_ID)
            .referenceType(PlanReferenceType.API)
            .status(io.gravitee.repository.management.model.Plan.Status.DEPRECATED)
            .security(io.gravitee.repository.management.model.Plan.PlanSecurityType.API_KEY)
            .build();
        when(planRepository.findById(eq(PLAN_ID))).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.deprecate(executionContext, PLAN_ID)).isInstanceOf(PlanAlreadyDeprecatedException.class);
    }
}
