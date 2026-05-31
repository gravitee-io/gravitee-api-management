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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
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
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlanService_CloseTest {

    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String USER = "user";
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
    private PlanConverter planConverter;

    @Mock
    private FlowService flowService;

    @Test
    public void shouldNotCloseBecauseNotFound() throws TechnicalException {
        assertThrows(PlanNotFoundException.class, () -> {
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.empty());

            planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldNotCloseBecauseAlreadyClosed() throws TechnicalException {
        assertThrows(PlanAlreadyClosedException.class, () -> {
            var plan = Plan.builder().status(Plan.Status.CLOSED).build();
            when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));

            planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldNotCloseBecauseTechnicalException() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            when(planRepository.findById(PLAN_ID)).thenThrow(TechnicalException.class);

            planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);
        });
    }

    @Test
    public void shouldClosePlanAndAcceptedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());
        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull());
    }

    @Test
    public void shouldClosePlanAndPendingSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());
        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull());
    }

    @Test
    public void shouldClosePlanAndPausedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscription.getId()).thenReturn(SUBSCRIPTION_ID);
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());
        verify(closeSubscriptionDomainService, times(1)).closeSubscription(eq(SUBSCRIPTION_ID), notNull());
    }

    @Test
    public void shouldClosePlanButNotClosedSubscription() throws TechnicalException {
        var plan = Plan.builder()
            .status(Plan.Status.PUBLISHED)
            .type(Plan.PlanType.API)
            .validation(Plan.PlanValidationType.AUTO)
            .referenceId(API_ID)
            .build();
        when(planRepository.findById(PLAN_ID)).thenReturn(Optional.of(plan));
        when(planRepository.update(plan)).thenAnswer(returnsFirstArg());
        when(subscriptionService.findByPlan(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(
            Collections.singleton(subscription)
        );
        when(planRepository.findByApi(any())).thenReturn(Collections.emptySet());

        planService.close(GraviteeContext.getExecutionContext(), PLAN_ID);

        verify(planRepository, times(1)).update(plan.toBuilder().status(Plan.Status.CLOSED).build());
    }
}
