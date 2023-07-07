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
package io.gravitee.gateway.security.jwt.policy;

import static io.gravitee.reporter.api.http.Metrics.on;
import static java.lang.System.currentTimeMillis;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSubscriptionPolicyTest {

    @Mock
    private Request request;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ExecutionContext executionContext;

    private static final String API_ID = "api-id";
    private static final String PLAN_ID = "plan-id";
    private static final String CLIENT_ID = "client-id";

    @Before
    public void init() {
        when(request.metrics()).thenReturn(on(currentTimeMillis()).build());
        when(request.timestamp()).thenReturn(currentTimeMillis());

        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn(PLAN_ID);
        when(executionContext.getAttribute(ExecutionContext.ATTR_API)).thenReturn(API_ID);
        when(executionContext.request()).thenReturn(request);
    }

    @Test
    public void shouldReturnUnauthorized_noSubscription() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        PolicyChain policyChain = mock(PolicyChain.class);

        // Search subscription now includes all criteria so the result is empty in case of bad clientId or planId.
        when(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).thenReturn(Optional.empty());

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1))
            .failWith(
                argThat(result ->
                    result.statusCode() == HttpStatusCode.UNAUTHORIZED_401 &&
                    CheckSubscriptionPolicy.GATEWAY_OAUTH2_ACCESS_DENIED_KEY.equals(result.key())
                )
            );
    }

    @Test
    public void shouldContinueWhenNoSelectionRule() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED)).thenReturn(false);

        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);

        Subscription subscription = new Subscription();

        when(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1)).doNext(request, response);
    }

    @Test
    public void shouldContinueAndUseRightPlanWhenSelectionRuleBasedPlan() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED)).thenReturn(true);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn(CLIENT_ID);
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn(PLAN_ID);
        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);

        final Subscription subscription = new Subscription();
        subscription.setId("subscription-id");
        subscription.setPlan(PLAN_ID);
        subscription.setApplication("application-id");

        when(subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID)).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(executionContext).setAttribute(ExecutionContext.ATTR_APPLICATION, subscription.getApplication());
        verify(executionContext).setAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID, subscription.getId());
        verify(executionContext).setAttribute(ExecutionContext.ATTR_PLAN, subscription.getPlan());
        verify(policyChain, times(1)).doNext(request, response);
    }

    ArgumentMatcher<PolicyResult> statusCode(int statusCode) {
        return argument -> argument.statusCode() == statusCode;
    }
}
