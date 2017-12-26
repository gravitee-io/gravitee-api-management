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
import io.gravitee.gateway.handlers.api.definition.Plan;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationHandlerEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanBasedAuthenticationHandlerEnhancer implements AuthenticationHandlerEnhancer {

    private final Logger logger = LoggerFactory.getLogger(PlanBasedAuthenticationHandlerEnhancer.class);

    @Autowired
    private Api api;

    @Override
    public List<AuthenticationHandler> filter(List<AuthenticationHandler> securityProviders) {
        logger.debug("Filtering security providers according to published API's plans");

        List<AuthenticationHandler> providers = new ArrayList<>();

        // Look into all plans for required authentication providers.
        Collection<Plan> plans = api.getPlans();
        securityProviders.forEach(provider -> {
            Optional<Plan> first = plans
                    .stream()
                    .filter(plan -> provider.name().equalsIgnoreCase(plan.getSecurity()))
                    .findFirst();
            if (first.isPresent()) {
                logger.debug("Security provider [{}] is required by, at least, one plan. Installing...", provider.name());
                providers.add(new PlanBasedAuthenticationHandler(provider, first.get()));
            }
        });

        if (! providers.isEmpty()) {
            logger.info("API [{} ({})] requires the following security providers:", api.getName(), api.getVersion());
            providers.forEach(authenticationProvider -> logger.info("\t* {}", authenticationProvider.name()));
        } else {
            logger.warn("No security provider is provided for API [{} ({})]", api.getName(), api.getVersion());
        }
        return providers;
    }

    public void setApi(Api api) {
        this.api = api;
    }
}
