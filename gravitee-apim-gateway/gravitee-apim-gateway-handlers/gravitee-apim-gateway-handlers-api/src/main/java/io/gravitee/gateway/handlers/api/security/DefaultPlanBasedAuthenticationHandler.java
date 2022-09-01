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

/**
 * @author GraviteeSource Team
 */
public class DefaultPlanBasedAuthenticationHandler extends PlanBasedAuthenticationHandler {

    public DefaultPlanBasedAuthenticationHandler(AuthenticationHandler handler, Plan plan) {
        super(handler, plan);
    }

    /**
     * Always return true, can handle the request, without checking subscription.
     * Used for :
     * - Keyless plan : no need to subscribe
     * - Oauth2 plan : subscription is checked during policy chain (fetching subscription needs client_id from Oauth2 token introspection)
     *
     * {@inheritDoc}
     */
    @Override
    protected boolean canHandleSubscription(AuthenticationContext authenticationContext) {
        return true;
    }
}
