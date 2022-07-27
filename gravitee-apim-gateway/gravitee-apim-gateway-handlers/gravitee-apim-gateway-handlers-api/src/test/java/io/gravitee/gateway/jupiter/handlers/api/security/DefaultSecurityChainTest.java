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
package io.gravitee.gateway.jupiter.handlers.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.policy.SecurityPolicy;
import io.gravitee.gateway.jupiter.policy.PolicyManager;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
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
class DefaultSecurityChainTest {

    protected static final String MOCK_POLICY = "mock-policy";
    protected static final String MOCK_POLICY_CONFIG = "mock-policy-configuration";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private Api api;

    @Mock
    private PolicyManager policyManager;

    @Mock
    private RequestExecutionContext ctx;

    @Test
    void shouldExecuteSecurityPolicyWhenCanHandle() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", false, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", true, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getDefinition().getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(policy2.onRequest(ctx)).thenReturn(Completable.complete());

        final DefaultSecurityChain cut = new DefaultSecurityChain(api, policyManager);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
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

        when(api.getDefinition().getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(policy1.onRequest(ctx)).thenReturn(Completable.complete());

        final DefaultSecurityChain cut = new DefaultSecurityChain(api, policyManager);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy2).order();
        verifyNoMoreInteractions(policy2);
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

        when(api.getDefinition().getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(ctx.getAttribute(AbstractSecurityChain.SKIP_SECURITY_CHAIN)).thenReturn(true);

        final DefaultSecurityChain cut = new DefaultSecurityChain(api, policyManager);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertResult();
        verify(policy1).order();
        verify(policy2).order();
        verifyNoMoreInteractions(policy1, policy2);
    }

    @Test
    void shouldInterrupt401WhenNoPlan() {
        when(api.getDefinition().getPlans()).thenReturn(new ArrayList<>());
        when(ctx.interruptWith(any())).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final DefaultSecurityChain cut = new DefaultSecurityChain(api, policyManager);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertError(Throwable.class);
        verifyUnauthorized();
    }

    @Test
    void shouldInterrupt401WhenNoPolicyCanHandle() {
        final Plan plan1 = mockPlan("plan1");
        final Plan plan2 = mockPlan("plan2");
        final SecurityPolicy policy1 = mockSecurityPolicy("plan1", false, false);
        final SecurityPolicy policy2 = mockSecurityPolicy("plan2", false, false);

        final ArrayList<Plan> plans = new ArrayList<>();
        plans.add(plan1);
        plans.add(plan2);

        when(api.getDefinition().getPlans()).thenReturn(plans);
        when(policy1.order()).thenReturn(0);
        when(policy2.order()).thenReturn(1);

        when(ctx.interruptWith(any())).thenReturn(Completable.error(new RuntimeException(MOCK_EXCEPTION)));

        final DefaultSecurityChain cut = new DefaultSecurityChain(api, policyManager);
        final TestObserver<Void> obs = cut.execute(ctx).test();

        obs.assertError(Throwable.class);
        verifyUnauthorized();
    }

    private void verifyUnauthorized() {
        verify(ctx)
            .interruptWith(
                argThat(
                    failure -> {
                        assertEquals(HttpStatusCode.UNAUTHORIZED_401, failure.statusCode());
                        Assertions.assertEquals(AbstractSecurityChain.UNAUTHORIZED_MESSAGE, failure.message());
                        Assertions.assertEquals(AbstractSecurityChain.PLAN_UNRESOLVABLE, failure.key());
                        assertNull(failure.parameters());
                        assertNull(failure.contentType());

                        return true;
                    }
                )
            );
    }

    private Plan mockPlan(String name) {
        final Plan plan = mock(Plan.class);

        when(plan.getSecurity()).thenReturn(MOCK_POLICY + "-" + name);
        when(plan.getSecurityDefinition()).thenReturn(MOCK_POLICY_CONFIG + '-' + name);

        return plan;
    }

    private SecurityPolicy mockSecurityPolicy(String name, boolean canHandle, boolean validateSubscription) {
        final SecurityPolicy policy = mock(SecurityPolicy.class);

        when(
            policyManager.create(
                eq(ExecutionPhase.REQUEST),
                argThat(
                    meta ->
                        meta.getName().equals(MOCK_POLICY + "-" + name) && meta.getConfiguration().equals(MOCK_POLICY_CONFIG + '-' + name)
                )
            )
        )
            .thenReturn(policy);

        lenient().when(policy.support(ctx)).thenReturn(Single.just(canHandle));

        if (canHandle) {
            lenient().when(policy.requireSubscription()).thenReturn(validateSubscription);
        }

        return policy;
    }
}
