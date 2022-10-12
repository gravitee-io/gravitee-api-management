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

import static io.gravitee.gateway.security.core.AuthenticationContext.ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE;

import io.gravitee.gateway.api.ExecutionContext;
import java.util.List;

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
        final SimpleAuthenticationContext authenticationContext = new SimpleAuthenticationContext(executionContext);
        final List<AuthenticationHandler> authenticationHandlers = authenticationHandlerManager.getAuthenticationHandlers();

        for (int i = 0; i < authenticationHandlers.size(); i++) {
            final AuthenticationHandler authenticationHandler = authenticationHandlers.get(i);
            authenticationContext.setInternalAttribute(ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE, true);

            if (i < authenticationHandlers.size() - 1) {
                // Check whether the next authentication handler works on the same type of token or not.
                authenticationContext.setInternalAttribute(
                    ATTR_INTERNAL_LAST_SECURITY_HANDLER_SUPPORTING_SAME_TOKEN_TYPE,
                    !authenticationHandler.tokenType().equals(authenticationHandlers.get(i + 1).tokenType())
                );
            }

            if (authenticationHandler.canHandle(authenticationContext)) {
                return authenticationHandler;
            }
        }

        return null;
    }
}
