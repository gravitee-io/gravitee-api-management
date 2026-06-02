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
package io.gravitee.rest.api.service.v4.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlanService_DeprecateTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanServiceImpl planService;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private FlowService flowService;

    @Mock
    private FlowCrudService flowCrudService;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private AuditService auditService;

    @BeforeEach
    public void setUp() {}

    @Test
    public void shouldNotDepreciateBecauseAlreadyDepreciated() throws TechnicalException {
        assertThrows(PlanAlreadyDeprecatedException.class, () -> {
            var plan = Plan.builder().id(PLAN_ID).status(Plan.Status.DEPRECATED).build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldNotDepreciateBecauseAlreadyClosed() throws TechnicalException {
        assertThrows(PlanAlreadyClosedException.class, () -> {
            var plan = Plan.builder().id(PLAN_ID).status(Plan.Status.CLOSED).build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldNotDepreciateBecauseNotPublished() throws TechnicalException {
        assertThrows(PlanNotYetPublishedException.class, () -> {
            var plan = Plan.builder().id(PLAN_ID).status(Plan.Status.STAGING).build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldDepreciateWithStagingPlanAndAllowStaging() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .id(PLAN_ID)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID, true);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
    }

    @Test
    public void shouldDepreciateNativePlanWithStagingAndAllowStaging() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .referenceType(Plan.PlanReferenceType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .id(PLAN_ID)
            .referenceId(API_ID)
            .apiType(ApiType.NATIVE)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID, true);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
        verify(flowCrudService, times(1)).getNativePlanFlows(any(String.class));
        verify(flowService, never()).findByReference(any(), any());
    }

    @Test
    public void shouldNotDepreciateWithStagingPlanAndNotAllowStaging() throws TechnicalException {
        assertThrows(PlanNotYetPublishedException.class, () -> {
            var plan = Plan.builder()
                .status(Plan.Status.STAGING)
                .referenceType(Plan.PlanReferenceType.API)
                .validation(Plan.PlanValidationType.AUTO)
                .id(PLAN_ID)
                .referenceId(API_ID)
                .build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID, false);

            verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
        });
    }

    @Test
    public void shouldNotDepreciateBecauseTechnicalException() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

            planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldDepreciate() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .referenceType(Plan.PlanReferenceType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .id(PLAN_ID)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
    }
}
