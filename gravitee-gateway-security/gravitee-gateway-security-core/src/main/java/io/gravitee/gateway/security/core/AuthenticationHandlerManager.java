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
package io.gravitee.gateway.security.core;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.ComponentResolver;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to load multiple implementations of {@link AuthenticationHandler}.
 * Loaded implementations are then filtered according to published plan to select only accurate security
 * authentication implementations.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationHandlerManager {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationHandlerManager.class);

    private final SecurityProviderLoader securityProviderLoader;

    private final ComponentProvider componentProvider;

    private AuthenticationHandlerEnhancer authenticationHandlerEnhancer;

    public AuthenticationHandlerManager(SecurityProviderLoader securityProviderLoader, ComponentProvider componentProvider) {
        this.securityProviderLoader = securityProviderLoader;
        this.componentProvider = componentProvider;
    }

    private List<AuthenticationHandler> authenticationHandlers;

    public void afterPropertiesSet() {
        logger.debug("Loading security providers...");
        List<AuthenticationHandler> availableSecurityProviders = securityProviderLoader.getSecurityProviders();

        availableSecurityProviders.forEach(
            authenticationHandler -> {
                if (authenticationHandler instanceof ComponentResolver) {
                    try {
                        ((ComponentResolver) authenticationHandler).resolve(componentProvider);
                    } catch (Exception e) {
                        logger.debug(
                            "An error occurs while loading security provider [{}]: {}",
                            authenticationHandler.name(),
                            e.getMessage()
                        );
                    }
                }
            }
        );

        // Filter security providers if a filter is defined
        if (authenticationHandlerEnhancer != null) {
            authenticationHandlers = authenticationHandlerEnhancer.filter(availableSecurityProviders);
        } else {
            authenticationHandlers = availableSecurityProviders;
        }

        // Sort by order
        Collections.sort(authenticationHandlers, Comparator.comparingInt(AuthenticationHandler::order));
    }

    public List<AuthenticationHandler> getAuthenticationHandlers() {
        return authenticationHandlers;
    }

    public void setAuthenticationHandlerEnhancer(AuthenticationHandlerEnhancer authenticationHandlerEnhancer) {
        this.authenticationHandlerEnhancer = authenticationHandlerEnhancer;
    }
}
