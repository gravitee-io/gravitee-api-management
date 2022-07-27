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
package io.gravitee.gateway.handlers.api.policy.security;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FreePlanAuthenticationHandlerEnhancer extends PlanBasedAuthenticationHandlerEnhancer {

    private final Logger logger = LoggerFactory.getLogger(FreePlanAuthenticationHandlerEnhancer.class);

    public FreePlanAuthenticationHandlerEnhancer(Api api) {
        super(api);
    }

    @Override
    public List<AuthenticationHandler> filter(List<AuthenticationHandler> authenticationHandlers) {
        if (!getApi().getDefinition().getPlans().isEmpty()) {
            // plan are not required but there are some definition.
            // plans have highest priority
            return super.filter(authenticationHandlers);
        } else {
            logger.debug("Filtering authentication handlers according to API security options");
            List<AuthenticationHandler> providers = new ArrayList<>();

            Optional<AuthenticationHandler> optionalProvider = authenticationHandlers
                .stream()
                .filter(provider -> provider.name().equalsIgnoreCase("KEY_LESS"))
                .findFirst();

            if (optionalProvider.isPresent()) {
                AuthenticationHandler provider = optionalProvider.get();
                logger.debug("Authentication handler [{}] is required by the api [{}]. Installing...", provider.name(), getApi().getName());

                // Override the default api_key handler to validate the key against the current plan
                if (provider.name().equals("api_key")) {
                    //provider = new ApiKeyPlanBasedAuthenticationHandler(provider, plan);
                    throw new IllegalArgumentException("API KEY not yet managed");
                }

                providers.add(new FreePlanAuthenticationHandler(provider, getApi()));
            }

            if (!providers.isEmpty()) {
                logger.debug("{} requires the following authentication handlers:", getApi());
                providers.forEach(authenticationProvider -> logger.debug("\t* {}", authenticationProvider.name()));
            } else {
                logger.warn("No authentication handler is provided for {}", getApi());
            }

            return providers;
        }
    }
}
