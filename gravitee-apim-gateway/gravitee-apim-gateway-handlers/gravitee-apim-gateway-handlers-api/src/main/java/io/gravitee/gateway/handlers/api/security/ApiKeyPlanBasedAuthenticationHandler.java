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
package io.gravitee.gateway.handlers.api.security;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyPlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";

    public ApiKeyPlanBasedAuthenticationHandler(AuthenticationHandler handler, Plan plan) {
        super(handler, plan);
    }

    /**
     * For now, we only check that the provided API key matches the plan.
     * Since Gravitee 3.17, related subscription is also checked.
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleSubscription(AuthenticationContext authenticationContext) {
        if (!authenticationContext.contains(APIKEY_CONTEXT_ATTRIBUTE)) {
            return false;
        }
        Optional<ApiKey> optApikey = (Optional<ApiKey>) authenticationContext.get(APIKEY_CONTEXT_ATTRIBUTE);
        return optApikey.isPresent() && optApikey.get().getPlan().equals(plan.getId());
    }
}
