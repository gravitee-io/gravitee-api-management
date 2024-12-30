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
package io.gravitee.gateway.reactive.handlers.api.security;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_SKIP;
import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_SECURITY_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.policy.SecurityPolicy;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpSecurityChainTest {

    protected static final String MOCK_POLICY = "mock-policy";
    protected static final String MOCK_POLICY_CONFIG = "mock-policy-configuration";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private Api api;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private HttpExecutionContext ctx;

    @Test
    void shouldExecuteSecurityPolicyWhenHasRelevantSecurityToken() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", false, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", true, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(policy2.onRequest(ctx)).thenReturn(Completable.complete());

        final HttpSecurityChain cut = new HttpSecurityChain(api, policyManager, ExecutionPhase.REQUEST);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(ctx, times(1)).removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
    }

    @Test
    void shouldNotExecuteSecondSecurityPolicyWhenFirstHandled() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", true, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", true, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());

        final HttpSecurityChain cut = new HttpSecurityChain(api, policyManager, ExecutionPhase.REQUEST);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy2).order();
        verifyNoMoreInteractions(policy2);
        verify(ctx, times(1)).removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
    }

    @Test
    void shouldSkipSecurityWhenSkipSecurityChainAttributeIsDefined() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", true, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", true, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(ctx.getInternalAttribute(ATTR_INTERNAL_SECURITY_SKIP)).thenReturn(true);

        final HttpSecurityChain cut = new HttpSecurityChain(api, policyManager, ExecutionPhase.REQUEST);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy1).order();
        verify(policy2).order();
        verifyNoMoreInteractions(policy1, policy2);
        verify(ctx, never()).removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
    }

    @Test
    void shouldInterrupt401WhenNoPlan() {
        when(api.getPlans()).thenReturn(new ArrayList<>());
        when(ctx.interruptWith(any())).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final HttpSecurityChain cut = new HttpSecurityChain(api, policyManager, ExecutionPhase.REQUEST);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertError(Throwable.class);
        verifyUnauthorized();
        verify(ctx, times(1)).removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
    }

    @Test
    void shouldInterrupt401WhenNoPolicyHasRelevantSecurityToken() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", false, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", false, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(ctx.interruptWith(any())).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final HttpSecurityChain cut = new HttpSecurityChain(api, policyManager, ExecutionPhase.REQUEST);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertError(Throwable.class);
        verifyUnauthorized();
        verify(ctx, times(1)).removeInternalAttribute(ATTR_INTERNAL_SECURITY_TOKEN);
    }

    private void verifyUnauthorized() {
        verify(ctx)
            .interruptWith(
                argThat(failure -> {
                    assertEquals(HttpStatusCode.UNAUTHORIZED_401, failure.statusCode());
                    Assertions.assertEquals(HttpSecurityChain.UNAUTHORIZED_MESSAGE, failure.message());
                    Assertions.assertEquals(HttpSecurityChain.PLAN_UNRESOLVABLE, failure.key());
                    assertNull(failure.parameters());
                    assertNull(failure.contentType());

                    return true;
                })
            );
    }

    private Plan mockPlan(String name) {
        final Plan plan = mock(Plan.class);

        when(plan.getSecurity()).thenReturn(MOCK_POLICY + "-" + name);
        when(plan.getSecurityDefinition()).thenReturn(MOCK_POLICY_CONFIG + '-' + name);

        return plan;
    }

    private SecurityPolicy mockSecurityPolicy(String name, boolean hasSecurityToken, boolean validateSubscription) {
        final SecurityPolicy policy = mock(SecurityPolicy.class);

        when(
            policyManager.create(
                eq(ExecutionPhase.REQUEST),
                argThat(meta ->
                    meta.getName().equals(MOCK_POLICY + "-" + name) && meta.getConfiguration().equals(MOCK_POLICY_CONFIG + '-' + name)
                )
            )
        )
            .thenReturn(policy);

        Maybe<SecurityToken> securityTokenMaybe = hasSecurityToken ? Maybe.just(SecurityToken.forApiKey("testApiKey")) : Maybe.empty();
        lenient().when(policy.extractSecurityToken(ctx)).thenReturn(securityTokenMaybe);

        if (hasSecurityToken) {
            lenient().when(policy.requireSubscription()).thenReturn(validateSubscription);
        }

        return policy;
    }
}
