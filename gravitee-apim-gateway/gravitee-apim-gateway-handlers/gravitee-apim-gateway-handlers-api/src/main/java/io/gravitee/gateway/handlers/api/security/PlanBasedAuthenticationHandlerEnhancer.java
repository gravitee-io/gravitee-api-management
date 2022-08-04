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

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationHandlerEnhancer;
import io.gravitee.repository.management.api.SubscriptionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PlanBasedAuthenticationHandlerEnhancer implements AuthenticationHandlerEnhancer {

    private final Logger logger = LoggerFactory.getLogger(PlanBasedAuthenticationHandlerEnhancer.class);

    @Autowired
    protected SubscriptionRepository subscriptionRepository;

    private final Api api;

    public PlanBasedAuthenticationHandlerEnhancer(Api api) {
        this.api = api;
    }

    @Override
    public List<AuthenticationHandler> filter(List<AuthenticationHandler> authenticationHandlers) {
        logger.debug("Filtering authentication handlers according to published API's plans");

        List<AuthenticationHandler> providers = new ArrayList<>();

        // Look into all plans for required authentication providers.
        api
            .getPlans()
            .forEach(
                plan -> {
                    Optional<AuthenticationHandler> optionalProvider = authenticationHandlers
                        .stream()
                        .filter(provider -> provider.name().equalsIgnoreCase(plan.getSecurity()))
                        .findFirst();
                    if (optionalProvider.isPresent()) {
                        AuthenticationHandler provider = optionalProvider.get();
                        logger.debug(
                            "Authentication handler [{}] is required by the plan [{}]. Installing...",
                            provider.name(),
                            plan.getName()
                        );

                        if ("api_key".equals(provider.name())) {
                            providers.add(new ApiKeyPlanBasedAuthenticationHandler(provider, plan));
                        } else if ("jwt".equals(provider.name())) {
                            providers.add(new JwtPlanBasedAuthenticationHandler(provider, plan, subscriptionRepository));
                        } else {
                            providers.add(new DefaultPlanBasedAuthenticationHandler(provider, plan));
                        }
                    }
                }
            );

        if (!providers.isEmpty()) {
            logger.debug("{} requires the following authentication handlers:", api);
            providers.forEach(authenticationProvider -> logger.debug("\t* {}", authenticationProvider.name()));
        } else {
            logger.warn("No authentication handler is provided for {}", api);
        }

        return providers;
    }

    protected Api getApi() {
        return this.api;
    }
}
