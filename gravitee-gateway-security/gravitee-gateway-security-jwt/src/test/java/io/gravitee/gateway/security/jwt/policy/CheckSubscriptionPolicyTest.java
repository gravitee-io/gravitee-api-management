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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckSubscriptionPolicyTest {

    @Test
    public void shouldReturnUnauthorized_onException() throws PolicyException, TechnicalException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(executionContext.getComponent(SubscriptionRepository.class)).thenReturn(subscriptionRepository);

        when(subscriptionRepository.search(any(SubscriptionCriteria.class)))
                .thenThrow(TechnicalException.class);

        policy.onRequest(request, response, policyChain, executionContext);

        verify(policyChain, times(1)).failWith(argThat(statusCode(HttpStatusCode.UNAUTHORIZED_401)));
    }

    @Test
    public void shouldReturnUnauthorized_badClient() throws PolicyException, TechnicalException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");

        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(executionContext.getComponent(SubscriptionRepository.class)).thenReturn(subscriptionRepository);

        Subscription subscription = mock(Subscription.class);
        when(subscription.getClientId()).thenReturn("my-bad-client-id");

        when(subscriptionRepository.search(any(SubscriptionCriteria.class)))
                .thenReturn(Collections.singletonList(subscription));

        policy.onRequest(request, response, policyChain, executionContext);

        verify(policyChain, times(1)).failWith(argThat(statusCode(HttpStatusCode.UNAUTHORIZED_401)));
    }

    @Test
    public void shouldContinue() throws PolicyException, TechnicalException {
        CheckSubscriptionPolicy policy = new CheckSubscriptionPolicy();

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getAttribute(CheckSubscriptionPolicy.CONTEXT_ATTRIBUTE_CLIENT_ID)).thenReturn("my-client-id");

        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        when(executionContext.getComponent(SubscriptionRepository.class)).thenReturn(subscriptionRepository);

        Subscription subscription = mock(Subscription.class);
        when(subscription.getClientId()).thenReturn("my-client-id");

        when(subscriptionRepository.search(any(SubscriptionCriteria.class)))
                .thenReturn(Collections.singletonList(subscription));

        policy.onRequest(request, response, policyChain, executionContext);

        verify(policyChain, times(1)).doNext(request, response);
    }

    ArgumentMatcher<PolicyResult> statusCode(int statusCode) {
        return argument -> argument.statusCode() == statusCode;
    }
}
