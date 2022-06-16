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
package io.gravitee.gateway.security.apikey.policy;

import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static java.lang.System.currentTimeMillis;
import static org.mockito.Mockito.*;

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
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSubscriptionPolicyTest {

    private static final String SUBSCRIPTION_ID = "subscription_id";

    private CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

    private long requestTimestamp;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private PolicyChain policyChain;

    @Mock
    private Subscription subscription;

    @Before
    public void init() {
        requestTimestamp = currentTimeMillis();
        when(request.timestamp()).thenReturn(requestTimestamp);
        when(executionContext.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID)).thenReturn(SUBSCRIPTION_ID);
        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);
        when(executionContext.getComponent(SubscriptionService.class)).thenReturn(subscriptionService);
    }

    @Test
    public void should_fail_unauthorized_cause_no_subscription_found_in_cache() throws PolicyException {
        when(subscriptionService.getById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        policy.execute(policyChain, executionContext);

        verify(policyChain).failWith(argThat(result -> result.statusCode() == UNAUTHORIZED_401 && "API_KEY_INVALID".equals(result.key())));
        verifyNoMoreInteractions(policyChain);
    }

    @Test
    public void should_fail_unauthorized_cause_subscription_found_in_cache_has_no_valid_time() throws PolicyException {
        when(subscription.isTimeValid(anyLong())).thenReturn(false);
        when(subscriptionService.getById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain).failWith(argThat(result -> result.statusCode() == UNAUTHORIZED_401 && "API_KEY_INVALID".equals(result.key())));
        verifyNoMoreInteractions(policyChain);
    }

    @Test
    public void should_continue_cause_subscription_found_in_cache_and_has_valid_time() throws PolicyException {
        when(subscription.isTimeValid(requestTimestamp)).thenReturn(true);
        when(subscriptionService.getById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        policy.execute(policyChain, executionContext);

        verify(policyChain).doNext(request, response);
        verifyNoMoreInteractions(policyChain);
    }
}
