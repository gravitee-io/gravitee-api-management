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
package io.gravitee.management.service;

import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.SubscriptionEntity;
import io.gravitee.management.model.SubscriptionStatus;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.management.service.impl.PlanServiceImpl;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanServiceTest {

    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";

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

    @Test
    public void shouldFindById() throws TechnicalException {
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        final PlanEntity planEntity = planService.findById(PLAN_ID);

        assertNotNull(planEntity);
    }

    @Test(expected = PlanNotFoundException.class)
    public void shouldNotFindByIdBecauseNotExists() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        planService.findById(PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.findById(PLAN_ID);
    }

    @Test(expected = PlanWithSubscriptionsException.class)
    public void shouldNotDeleteBecauseSubscriptionsExist() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));

        planService.delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecauseStagingState() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecauseClosedState() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecausePublishedWithNoSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCloseBecauseAlreadyClosed() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));

        planService.close(PLAN_ID);
    }

    @Test
    public void shouldClosePlanAndAcceptedSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.ACCEPTED);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        planService.close(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, times(1)).close(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldClosePlanAndPendingSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.PENDING);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        planService.close(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
        verify(subscriptionService, times(1)).close(SUBSCRIPTION_ID);
    }

    @Test
    public void shouldClosePlanButNotClosedSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscription.getStatus()).thenReturn(SubscriptionStatus.CLOSED);
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        planService.close(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.CLOSED);
        verify(planRepository, times(1)).update(plan);
    }

    @Test(expected = PlanAlreadyPublishedException.class)
    public void shouldNotPublishBecauseAlreadyPublished() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotPublishBecauseAlreadyClose() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.publish(PLAN_ID);
    }

    @Test
    public void shouldPublish() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.STAGING);
        when(plan.getType()).thenReturn(Plan.PlanType.API);
        when(plan.getValidation()).thenReturn(Plan.PlanValidationType.AUTO);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.singleton(subscription));

        planService.publish(PLAN_ID);

        verify(plan, times(1)).setStatus(Plan.Status.PUBLISHED);
        verify(planRepository, times(1)).update(plan);
    }
}
