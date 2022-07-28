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
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Before
    public void init() {
        when(request.metrics()).thenReturn(on(currentTimeMillis()).build());
    }

    @Test
    public void shouldReturnUnauthorized_cause_no_subscription_found_in_cache() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn("plan-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_API)).thenReturn("api-id");
        when(executionContext.request()).thenReturn(request);

        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);

        // no subscription found in cache for this API / clientID
        when(subscriptionService.getByApiAndClientIdAndPlan("api-id", "my-client-id", "plan-id")).thenReturn(Optional.empty());

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1))
            .failWith(
                argThat(
                    result ->
                        result.statusCode() == HttpStatusCode.UNAUTHORIZED_401 &&
                        CheckSubscriptionPolicy.GATEWAY_OAUTH2_ACCESS_DENIED_KEY.equals(result.key())
                )
            );
    }

    @Test
    public void shouldReturnUnauthorized_cause_subscription_found_in_cache_has_no_valid_time() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn("plan-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_API)).thenReturn("api-id");
        when(executionContext.request()).thenReturn(request);

        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);

        // subscription found in cache, with invalid time
        Subscription subscription = mock(Subscription.class);
        when(subscription.isTimeValid(anyLong())).thenReturn(false);
        when(subscriptionService.getByApiAndClientIdAndPlan("api-id", "my-client-id", "plan-id")).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1))
            .failWith(
                argThat(
                    result ->
                        result.statusCode() == HttpStatusCode.UNAUTHORIZED_401 &&
                        CheckSubscriptionPolicy.GATEWAY_OAUTH2_ACCESS_DENIED_KEY.equals(result.key())
                )
            );
    }

    @Test
    public void shouldContinue() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn("plan-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_API)).thenReturn("api-id");
        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);

        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);

        // subscription found in cache, with a valid plan, and time
        Subscription subscription = mock(Subscription.class);
        when(subscription.isTimeValid(anyLong())).thenReturn(true);
        when(subscription.getPlan()).thenReturn("plan-id");
        when(subscriptionService.getByApiAndClientIdAndPlan("api-id", "my-client-id", "plan-id")).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1)).doNext(request, response);
    }

    @Test
    public void shouldReturnUnauthorized_cause_subscription_found_in_cache_has_bad_plan() throws PolicyException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_PLAN_SELECTION_RULE_BASED)).thenReturn(true);
        when(executionContext.getAttribute(ExecutionContext.ATTR_PLAN)).thenReturn("plan-id");
        when(executionContext.getAttribute(ExecutionContext.ATTR_API)).thenReturn("api-id");
        when(executionContext.request()).thenReturn(request);

        SubscriptionService subscriptionService = mock(SubscriptionService.class);
        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);

        // subscription found in cache, with an invalid plan
        Subscription subscription = mock(Subscription.class);
        when(subscription.getPlan()).thenReturn("plan2-id");
        when(subscriptionService.getByApiAndClientIdAndPlan("api-id", "my-client-id", "plan-id")).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain, times(1))
            .failWith(
                argThat(
                    result ->
                        result.statusCode() == HttpStatusCode.UNAUTHORIZED_401 &&
                        CheckSubscriptionPolicy.GATEWAY_OAUTH2_ACCESS_DENIED_KEY.equals(result.key())
                )
            );
    }
}
