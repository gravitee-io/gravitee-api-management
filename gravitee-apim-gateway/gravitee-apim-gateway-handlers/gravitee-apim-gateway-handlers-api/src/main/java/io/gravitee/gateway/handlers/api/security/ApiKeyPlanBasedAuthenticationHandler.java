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
package io.gravitee.gateway.handlers.api.security;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.reactive.api.policy.SecurityToken;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyPlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";

    private SubscriptionService subscriptionService;

    public ApiKeyPlanBasedAuthenticationHandler(AuthenticationHandler handler, Plan plan, SubscriptionService subscriptionService) {
        super(handler, plan);
        this.subscriptionService = subscriptionService;
    }

    @Override
    protected boolean preCheckSubscription(AuthenticationContext authenticationContext) {
        if (!authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)) {
            // There is no apikey at all, so no subscription to pre check.
            return true;
        }
        Optional<ApiKey> optApikey = (Optional<ApiKey>) authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE);
        if (optApikey.isEmpty() || !optApikey.get().getPlan().equals(plan.getId())) {
            return false;
        }

        Optional<Subscription> optSubscription = subscriptionService.getByApiAndSecurityToken(
            plan.getApi(),
            SecurityToken.forApiKey(optApikey.get().getKey()),
            plan.getId()
        );
        return optSubscription.isPresent() && optSubscription.get().isTimeValid(authenticationContext.request().timestamp());
    }
}
