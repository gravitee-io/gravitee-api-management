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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class is used to load multiple implementations of {@link AuthenticationHandler}.
 * Loaded implementations are then filtered according to published plan to select only accurate security
 * authentication implementations.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AuthenticationHandlerManager implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(AuthenticationHandlerManager.class);

    @Autowired
    private SecurityProviderLoader securityProviderLoader;

    @Autowired(required = false)
    private AuthenticationHandlerEnhancer authenticationHandlerEnhancer;

    private List<AuthenticationHandler> authenticationHandlers;

    public void afterPropertiesSet() {
        logger.debug("Loading security providers...");
        List<AuthenticationHandler> availableSecurityProviders =
                securityProviderLoader.getSecurityProviders();

        availableSecurityProviders.forEach(authenticationHandler -> {
            if (authenticationHandler instanceof InitializingBean) {
                try {
                    ((InitializingBean) authenticationHandler).afterPropertiesSet();
                } catch (Exception e) {
                    logger.debug("An error occurs while loading security provider [{}]: {}", authenticationHandler.name(), e.getMessage());
                }
            }
        });

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

    public void setSecurityProviderLoader(SecurityProviderLoader securityProviderLoader) {
        this.securityProviderLoader = securityProviderLoader;
    }

    public void setAuthenticationHandlerEnhancer(AuthenticationHandlerEnhancer authenticationHandlerEnhancer) {
        this.authenticationHandlerEnhancer = authenticationHandlerEnhancer;
    }
}
