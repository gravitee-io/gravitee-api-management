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
package io.gravitee.gateway.handlers.api.policy.security.apikey;

import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import java.util.List;
import java.util.Optional;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeyPlanBasedAuthenticationHandler implements AuthenticationHandler {

    private static final String APIKEY_CONTEXT_ATTRIBUTE = "apikey";

    private final AuthenticationHandler handler;
    private final Plan plan;

    public ApiKeyPlanBasedAuthenticationHandler(final AuthenticationHandler handler, final Plan plan) {
        this.handler = handler;
        this.plan = plan;
    }

    @Override
    public boolean canHandle(AuthenticationContext context) {
        boolean handle = handler.canHandle(context);

        if (!handle) {
            return false;
        }

        // Check that the plan associated to the api-key matches the current plan
        Optional<ApiKey> optApikey = (Optional<ApiKey>) context.get(APIKEY_CONTEXT_ATTRIBUTE);
        if (optApikey == null) {
            return false;
        }

        return optApikey.map(apikey -> apikey.getPlan().equals(plan.getId())).orElse(true);
    }

    @Override
    public String name() {
        return handler.name();
    }

    @Override
    public int order() {
        return handler.order();
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return handler.handle(executionContext);
    }
}
