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
package io.gravitee.rest.api.service.impl;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_DeprecateTest {

    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PlanConverter planConverter;

    @Mock
    private FlowService flowService;

    @Test(expected = PlanAlreadyDeprecatedException.class)
    public void shouldNotDepreciateBecauseAlreadyDepreciated() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.DEPRECATED).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotDepreciateBecauseAlreadyClosed() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.CLOSED).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = PlanNotYetPublishedException.class)
    public void shouldNotDepreciateBecauseNotPublished() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.STAGING).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldDepreciateWithStagingPlanAndAllowStaging() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID, true);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
    }

    @Test(expected = PlanNotYetPublishedException.class)
    public void shouldNotDepreciateWithStagingPlanAndNotAllowStaging() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.STAGING)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID, false);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDepreciateBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldDepreciate() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());

        planService.deprecate(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.DEPRECATED).build());
    }
}
