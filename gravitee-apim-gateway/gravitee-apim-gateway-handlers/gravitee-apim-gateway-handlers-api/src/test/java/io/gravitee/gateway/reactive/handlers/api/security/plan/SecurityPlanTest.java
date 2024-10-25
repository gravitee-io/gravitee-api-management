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
package io.gravitee.gateway.reactive.handlers.api.security.plan;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_TOKEN;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_VALIDATE_SUBSCRIPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.Plan;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.api.policy.SecurityPolicy;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SecurityPlanTest {

    protected static final String SELECTION_RULE = "{#selection-rule}";
    protected static final String V3_SELECTION_RULE = "#selection-rule";
    protected static final String API_ID = "apiId";
    protected static final String CLIENT_ID = "clientId";
    protected static final String PLAN_ID = "planId";
    protected static final String APPLICATION_ID = "applicationId";
    protected static final String SUBSCRIPTION_ID = "subscriptionId";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private Plan plan;

    @Mock
    private SecurityPolicy policy;

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainRequest request;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private SecurityToken securityToken;

    @Test
    void canExecute_shouldReturnTrue_whenPolicyHasSecurityTokenAndSelectionRuleIsNull() {
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnTrue_whenPolicyHasSecurityTokenAndSelectionRuleIsTrue() {
        when(plan.getSelectionRule()).thenReturn(SELECTION_RULE);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(true));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnTrue_whenPolicyHasSecurityTokenWithV3SelectionRule() {
        when(plan.getSelectionRule()).thenReturn(V3_SELECTION_RULE);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(true));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnTrue_whenPolicyHasSecurityTokenAndSelectionRuleIsFalse() {
        when(plan.getSelectionRule()).thenReturn(SELECTION_RULE);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(false));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(false);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnFalse_whenPolicyDontHaveSecurityToken() {
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.empty());

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(false);
        verify(ctx, never()).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnFalse_whenPolicyHasSecurityToken_butSubscriptionNotFound() {
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(policy.requireSubscription()).thenReturn(true);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(plan.getId()).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_API)).thenReturn(API_ID);

        // no subscription found with this security token
        when(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID)).thenReturn(Optional.empty());

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        verify(subscriptionService).getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        obs.assertResult(false);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnFalse_whenPolicyHasSecurityToken_butFoundSubscriptionIsNotOnSamePlan() {
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(policy.requireSubscription()).thenReturn(true);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(plan.getId()).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_API)).thenReturn(API_ID);

        // subscription found with this security token
        final Subscription subscription = mock(Subscription.class);
        when(subscription.getPlan()).thenReturn("another-plan-id");
        when(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID)).thenReturn(Optional.of(subscription));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        verify(subscriptionService).getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        obs.assertResult(false);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnFalse_whenPolicyHasSecurityToken_butFoundSubscriptionIsExpired() {
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(policy.requireSubscription()).thenReturn(true);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(plan.getId()).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_API)).thenReturn(API_ID);
        when(ctx.timestamp()).thenReturn(System.currentTimeMillis());

        // subscription found with this security token
        final Subscription subscription = mock(Subscription.class);
        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.isTimeValid(anyLong())).thenReturn(false); // subscription time is not valid
        when(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID)).thenReturn(Optional.of(subscription));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        verify(subscriptionService).getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        obs.assertResult(false);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void canExecute_shouldReturnTrue_whenPolicyHasSecurityToken_butFoundSubscriptionIsNotExpired() {
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(policy.requireSubscription()).thenReturn(true);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(plan.getId()).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_API)).thenReturn(API_ID);
        when(ctx.timestamp()).thenReturn(System.currentTimeMillis());

        // subscription found with this security token
        final Subscription subscription = mock(Subscription.class);
        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.isTimeValid(anyLong())).thenReturn(true); // subscription time is OK
        when(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID)).thenReturn(Optional.of(subscription));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        verify(subscriptionService).getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        obs.assertResult(true);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void execute_shouldReturnFalse_whenExceptionOccurredWhileSearchingSubscriptions() {
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(policy.requireSubscription()).thenReturn(true);
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(plan.getId()).thenReturn(PLAN_ID);
        when(ctx.getAttribute(ContextAttributes.ATTR_API)).thenReturn(API_ID);

        when(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID))
            .thenThrow(new RuntimeException("Mock TechnicalException"));

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(false);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }

    @Test
    void execute_shouldReturnPolicyOrderWhenCallingOrder() {
        final int order = 123;
        when(policy.order()).thenReturn(order);

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        assertEquals(order, cut.order());
    }

    @Test
    void execute_shouldExecutePolicyOnRequest() {
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Void> obs = cut.execute(ctx, ExecutionPhase.REQUEST).test();

        obs.assertResult();
    }

    @Test
    void canExecute_shouldReturnTrue_whenValidateSubscriptionIsEscaped() {
        when(policy.extractSecurityToken(ctx)).thenReturn(Maybe.just(securityToken));
        when(ctx.getInternalAttribute(ATTR_INTERNAL_VALIDATE_SUBSCRIPTION)).thenReturn(false);
        when(plan.getId()).thenReturn(PLAN_ID);

        final SecurityPlan cut = new SecurityPlan(plan.getId(), policy, plan.getSelectionRule());
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
        verify(ctx, times(1)).setInternalAttribute(eq(ATTR_INTERNAL_SECURITY_TOKEN), any());
    }
}
