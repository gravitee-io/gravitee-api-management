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
package io.gravitee.gateway.jupiter.handlers.api.security.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.Plan;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
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
    private RequestExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private SubscriptionService subscriptionService;

    @Test
    void shouldReturnTrueWhenPolicyCanHandleAndSelectionRuleIsNull() {
        when(policy.support(ctx)).thenReturn(Single.just(true));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
    }

    @Test
    void shouldReturnTrueWhenPolicyCanHandleAndSelectionRuleIsTrue() {
        when(plan.getSelectionRule()).thenReturn(SELECTION_RULE);
        when(policy.support(ctx)).thenReturn(Single.just(true));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(true));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
    }

    @Test
    void shouldReturnTrueWhenPolicyCanHandleWithV3SelectionRule() {
        when(plan.getSelectionRule()).thenReturn(V3_SELECTION_RULE);
        when(policy.support(ctx)).thenReturn(Single.just(true));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(true));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(true);
    }

    @Test
    void shouldReturnFalseWhenPolicyCanHandleAndSelectionRuleIsFalse() {
        when(plan.getSelectionRule()).thenReturn(SELECTION_RULE);
        when(policy.support(ctx)).thenReturn(Single.just(true));
        when(ctx.getTemplateEngine()).thenReturn(templateEngine);
        when(templateEngine.eval(SELECTION_RULE, Boolean.class)).thenReturn(Maybe.just(false));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(false);
    }

    @Test
    void shouldReturnFalseWhenPolicyCannotHandle() {
        when(policy.support(ctx)).thenReturn(Single.just(false));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Boolean> obs = cut.canExecute(ctx).test();

        obs.assertResult(false);
    }

    @Test
    void shouldReturnPolicyOrderWhenCallingOrder() {
        final int order = 123;
        when(policy.order()).thenReturn(order);

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        assertEquals(order, cut.order());
    }

    @Test
    void shouldExecutePolicyOnRequestWhenHandle() {
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(false);

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
    }

    @Test
    void shouldValidateSubscription_withApiAndClientIdInContext() {
        final Subscription subscription = mock(Subscription.class);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.isTimeValid(anyLong())).thenReturn(true);
        when(plan.getId()).thenReturn(PLAN_ID);

        when(ctx.request()).thenReturn(request);
        when(request.timestamp()).thenReturn(System.currentTimeMillis());
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(null);
        when(ctx.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(ctx.getAttribute(SecurityPlan.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).thenReturn(Optional.of(subscription));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy, times(0)).onInvalidSubscription(any());
    }

    @Test
    void shouldValidateSubscription_withSubscriptionIdInContext() {
        final Subscription subscription = mock(Subscription.class);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.getApplication()).thenReturn(APPLICATION_ID);
        when(subscription.isTimeValid(anyLong())).thenReturn(true);
        when(plan.getId()).thenReturn(PLAN_ID);

        when(ctx.request()).thenReturn(request);
        when(request.timestamp()).thenReturn(System.currentTimeMillis());
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(SUBSCRIPTION_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy, times(0)).onInvalidSubscription(any());
    }

    @Test
    void shouldCallOnSubscriptionInvalidWhenSubscriptionIsLinkedToAnotherPlan() {
        final Subscription subscription = mock(Subscription.class);

        when(subscription.getPlan()).thenReturn("other plan");
        when(plan.getId()).thenReturn(PLAN_ID);

        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(null);
        when(ctx.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(ctx.getAttribute(SecurityPlan.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(policy.onInvalidSubscription(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
    }

    @Test
    void shouldCallOnSubscriptionInvalidWhenSubscriptionIsExpired() {
        final Subscription subscription = mock(Subscription.class);

        when(subscription.getPlan()).thenReturn(PLAN_ID);
        when(subscription.isTimeValid(anyLong())).thenReturn(false);
        when(plan.getId()).thenReturn(PLAN_ID);

        when(ctx.request()).thenReturn(request);
        when(request.timestamp()).thenReturn(System.currentTimeMillis());
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(null);
        when(ctx.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(ctx.getAttribute(SecurityPlan.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).thenReturn(Optional.of(subscription));
        when(policy.onInvalidSubscription(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
    }

    @Test
    void shouldCallOnSubscriptionInvalidWhenNoSubscriptionFound() {
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(null);
        when(ctx.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(ctx.getAttribute(SecurityPlan.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).thenReturn(Optional.empty());
        when(policy.onInvalidSubscription(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
    }

    @Test
    void shouldCallOnSubscriptionInvalidWhenExceptionOccurredWhileSearchingSubscriptions() {
        when(policy.onRequest(ctx)).thenReturn(Completable.complete());
        when(policy.requireSubscription()).thenReturn(true);
        when(ctx.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(null);
        when(ctx.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(ctx.getAttribute(SecurityPlan.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(ctx.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(subscriptionService.getByApiAndClientId(API_ID, CLIENT_ID)).thenThrow(new RuntimeException("Mock TechnicalException"));
        when(policy.onInvalidSubscription(ctx)).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final SecurityPlan cut = new SecurityPlan(plan, policy);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
    }
}
