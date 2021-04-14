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

import static org.mockito.Mockito.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.exceptions.PlanWithSubscriptionsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import java.util.Collections;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_DeleteTest {

    private static final String PLAN_ID = "my-plan";

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
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotDeleteBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecauseClosedState() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.CLOSED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecausePublishedWithNoSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn("id");
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }
}
