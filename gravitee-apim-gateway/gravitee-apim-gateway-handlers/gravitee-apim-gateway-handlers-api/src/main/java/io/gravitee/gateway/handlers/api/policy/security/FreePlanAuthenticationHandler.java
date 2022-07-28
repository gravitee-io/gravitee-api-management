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

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FreePlanAuthenticationHandler implements AuthenticationHandler {

    protected final AuthenticationHandler handler;
    protected final Api api;

    public FreePlanAuthenticationHandler(final AuthenticationHandler handler, final Api api) {
        this.handler = handler;
        this.api = api;
    }

    @Override
    public boolean canHandle(AuthenticationContext authenticationContext) {
        return handler.canHandle(authenticationContext);
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
        return handler
            .handle(executionContext)
            .stream()
            /*                .map(new Function<AuthenticationPolicy, AuthenticationPolicy>() {
                    @Override
                    public AuthenticationPolicy apply(AuthenticationPolicy securityPolicy) {
                        // Override the configuration of the policy with the one provided by the plan
                        if (securityPolicy instanceof PluginAuthenticationPolicy) {
                            return new PluginAuthenticationPolicy() {
                                @Override
                                public String name() {
                                    return ((PluginAuthenticationPolicy) securityPolicy).name();
                                }

                                @Override
                                public String configuration() {
                                    return api.getAuthenticationDefinition();
                                }
                            };
                        }

                        return null; // for free plan api, we doesn't want to check subscription
                        // TODO check other thing ?
                    }
                })*/
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
