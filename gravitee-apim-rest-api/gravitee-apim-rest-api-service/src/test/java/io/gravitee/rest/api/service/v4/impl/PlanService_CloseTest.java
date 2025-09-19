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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
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
public class PlanService_CloseTest {

    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String API_ID = "my-api";

    @InjectMocks
    private PlanService planService = new PlanServiceImpl();

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private CloseSubscriptionDomainService closeSubscriptionDomainService;

    @Mock
    private SubscriptionEntity subscription;

    @Mock
    private AuditService auditService;

    @Mock
    private PlanMapper planMapper;

    @Mock
    private FlowService flowService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private GenericPlanMapper genericPlanMapper;

    @Test(expected = PlanNotFoundException.class)
    public void shouldNotCloseBecauseNotFound() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = PlanAlreadyClosedException.class)
    public void shouldNotCloseBecauseAlreadyClosed() throws TechnicalException {
        var plan = Plan.builder().status(Plan.Status.CLOSED).build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCloseBecauseTechnicalException() throws TechnicalException {
        when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
    }

    @Test
    public void shouldClosePlanV4AndAcceptedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        var api = new Api();
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        var planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setApiId(API_ID);
        when(genericPlanMapper.toGenericPlan(eq(api), eq(plan))).thenReturn(planEntity);

        GenericPlanEntity genericPlanEntity = planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertThat(genericPlanEntity.getId()).isEqualTo(PLAN_ID);
        assertThat(genericPlanEntity.getApiId()).isEqualTo(API_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());

        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull(AuditInfo.class));
    }

    @Test
    public void shouldClosePlanV2AndPendingSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        var api = new Api();
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        var planEntity = new io.gravitee.rest.api.model.PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setApi(API_ID);
        when(genericPlanMapper.toGenericPlan(eq(api), eq(plan))).thenReturn(planEntity);

        GenericPlanEntity genericPlanEntity = planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        assertThat(genericPlanEntity.getId()).isEqualTo(PLAN_ID);
        assertThat(genericPlanEntity.getApiId()).isEqualTo(API_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());

        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull(AuditInfo.class));
    }

    @Test
    public void shouldClosePlanAndPausedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        var api = new Api();
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());

        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull(AuditInfo.class));
    }

    @Test
    public void shouldClosePlanButNotClosedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .api(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        var api = new Api();
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());
    }
}
