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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import java.util.Optional;

/**
 * @author GraviteeSource Team
 */
public class CheckSubscriptionPolicy implements Policy {

    private static final String API_KEY_INVALID_KEY = "API_KEY_INVALID";
    private static final String API_KEY_INVALID_MESSAGE = "API Key is not valid or is expired / revoked.";

    @Override
    public void execute(PolicyChain policyChain, ExecutionContext executionContext) throws PolicyException {
        SubscriptionService subscriptionService = executionContext.getComponent(SubscriptionService.class);
        String subscriptionId = (String) executionContext.getAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID);

        Optional<Subscription> subscriptionOpt = subscriptionService.getById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            if (subscription.isTimeValid(executionContext.request().timestamp())) {
                executionContext.setAttribute(ExecutionContext.ATTR_APPLICATION, subscription.getApplication());
                executionContext.setAttribute(ExecutionContext.ATTR_SUBSCRIPTION_ID, subscription.getId());
                executionContext.setAttribute(ExecutionContext.ATTR_PLAN, subscription.getPlan());
                policyChain.doNext(executionContext.request(), executionContext.response());
                return;
            }
        }

        policyChain.failWith(PolicyResult.failure(API_KEY_INVALID_KEY, UNAUTHORIZED_401, API_KEY_INVALID_MESSAGE));
    }

    @Override
    public String id() {
        return "check-subscription";
    }
}
