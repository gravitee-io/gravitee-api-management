/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_CloseTest {

    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String USER = "user";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private Plan plan;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;


    @Test(expected = PlanNotFoundException.class)
    public void shouldNotCloseBecauseNotFound() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        planService.close(PLAN_ID, USER);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCloseBecauseAlreadyClosed() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.close(PLAN_ID, USER);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCloseBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.close(PLAN_ID, USER);
    }

    @Test
    public void shouldClosePlanAndAcceptedSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(PLAN_ID, USER);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, times(1)).close(SUBSCRIPTION_ID);
        verify(subscriptionService, never()).process(any(), any());
    }

    @Test
    public void shouldClosePlanAndPendingSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(PLAN_ID, USER);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, times(1)).close(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldClosePlanAndPausedSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(PLAN_ID, USER);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, times(1)).close(SUBSCRIPTION_ID);
        verify(subscriptionService, never()).process(any(), any());
    }


    @Test
    public void shouldClosePlanButNotClosedSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(PLAN_ID, USER);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, never()).process(any(), any());
    }
}
