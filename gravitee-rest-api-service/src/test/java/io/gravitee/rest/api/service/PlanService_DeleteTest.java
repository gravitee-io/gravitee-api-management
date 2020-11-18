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

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.exceptions.PlanWithSubscriptionsException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.PlanServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class PlanService_DeleteTest {

    private static final String API_ID = "my-api";
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

    @Mock
    private ApiService apiService;

    @Mock
    private ApiEntity api;


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
        when(plan.getApi()).thenReturn(API_ID);
        when(apiService.findById(API_ID)).thenReturn(api);
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
        when(plan.getApi()).thenReturn(API_ID);
        when(apiService.findById(API_ID)).thenReturn(api);
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDeleteBecausePublishedWithNoSubscription() throws TechnicalException {
        when(plan.getStatus()).thenReturn(Plan.Status.PUBLISHED);
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn(API_ID);
        when(apiService.findById(API_ID)).thenReturn(api);
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn(API_ID);
        when(apiService.findById(API_ID)).thenReturn(api);
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
        verify(apiService, times(0)).update(anyString(), any(UpdateApiEntity.class));
    }


    @Test
    public void shouldDeleteAndUpdateApiDefinition() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(subscriptionService.findByPlan(PLAN_ID)).thenReturn(Collections.emptySet());
        when(plan.getApi()).thenReturn(API_ID);
        io.gravitee.definition.model.Plan planDefinition = mock(io.gravitee.definition.model.Plan.class);
        when(planDefinition.getId()).thenReturn(PLAN_ID);
        when(api.getPlans()).thenReturn(Collections.singletonList(planDefinition));
        when(api.getGraviteeDefinitionVersion()).thenReturn(DefinitionVersion.V2.getLabel());
        when(apiService.findById(API_ID)).thenReturn(api);
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.delete(PLAN_ID);

        verify(planRepository, times(1)).delete(PLAN_ID);
        verify(apiService, times(1)).update(anyString(), any(UpdateApiEntity.class));
    }
}
