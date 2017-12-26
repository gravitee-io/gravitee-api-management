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


import io.gravitee.gateway.api.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
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
public class SecurityProviderManager {

    private final Logger logger = LoggerFactory.getLogger(SecurityProviderManager.class);

    @Autowired
    private SecurityProviderLoader securityProviderLoader;

    @Autowired(required = false)
    private AuthenticationHandlerEnhancer securityProviderFilter;

    private List<AuthenticationHandler> securityProviders;

    /**
     * Get an {@link AuthenticationHandler} from the incoming HTTP request.
     * @param request Incoming HTTP request.
     * @return The security provider to apply got the incoming request.
     */
    public AuthenticationHandler resolve(Request request) {
        for(AuthenticationHandler securityProvider : securityProviders) {
            if (securityProvider.canHandle(request)) {
                return securityProvider;
            }
        }

        return null;
    }

    @PostConstruct
    public void initializeSecurityProviders() {
        logger.debug("Loading security providers...");
        List<AuthenticationHandler> availableSecurityProviders =
                securityProviderLoader.getSecurityProviders();

        // Sort by order
        Collections.sort(availableSecurityProviders, Comparator.comparingInt(AuthenticationHandler::order));

        // Filter security providers if a filter is defined
        if (securityProviderFilter != null) {
            securityProviders = securityProviderFilter.filter(availableSecurityProviders);
        } else {
            securityProviders = availableSecurityProviders;
        }
    }

    public List<AuthenticationHandler> getSecurityProviders() {
        return securityProviders;
    }

    public void setSecurityProviderLoader(SecurityProviderLoader securityProviderLoader) {
        this.securityProviderLoader = securityProviderLoader;
    }

    public void setSecurityProviderFilter(AuthenticationHandlerEnhancer securityProviderFilter) {
        this.securityProviderFilter = securityProviderFilter;
    }
}
