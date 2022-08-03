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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultAuthenticationHandlerSelector implements AuthenticationHandlerSelector {

    private final AuthenticationHandlerManager authenticationHandlerManager;

    public DefaultAuthenticationHandlerSelector(AuthenticationHandlerManager authenticationHandlerManager) {
        this.authenticationHandlerManager = authenticationHandlerManager;
    }

    @Override
    public AuthenticationHandler select(ExecutionContext executionContext) {
        // Prepare the authentication context
        final SimpleAuthenticationContext context = new SimpleAuthenticationContext(executionContext);

        for (AuthenticationHandler securityProvider : authenticationHandlerManager.getAuthenticationHandlers()) {
            if (securityProvider.canHandle(context)) {
                return securityProvider;
            }
        }

        return null;
    }
}
