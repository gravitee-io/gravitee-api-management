/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.security.oauth2;

import static io.gravitee.gateway.security.core.AuthenticationContext.ATTR_INTERNAL_TOKEN_IDENTIFIED;
import static io.gravitee.gateway.security.core.AuthenticationContext.TOKEN_TYPE_AUTHORIZATION_BEARER;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.security.core.*;
import io.gravitee.gateway.security.oauth2.policy.CheckSubscriptionPolicy;
import java.util.Arrays;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAuth2AuthenticationHandler implements AuthenticationHandler {

    /**
     * The name of the authentication handler, which is also the name of the policy to invoke for coherency.
     */
    static final String AUTHENTICATION_HANDLER_NAME = "oauth2";

    static final String BEARER_AUTHORIZATION_TYPE = "Bearer";

    static final String JWT_CONTEXT_ATTRIBUTE = "jwt";

    private static final List<AuthenticationPolicy> POLICIES = Arrays.asList(
        // First, validate the incoming access_token thanks to an OAuth2 authorization server
        (PluginAuthenticationPolicy) () -> AUTHENTICATION_HANDLER_NAME,
        // Then, check that there is an existing valid subscription associated to the client_id
        (HookAuthenticationPolicy) () -> CheckSubscriptionPolicy.class
    );

    @Override
    public boolean canHandle(AuthenticationContext context) {
        String token = TokenExtractor.extract(context.request());

        if (token == null) {
            return false;
        }

        context.setInternalAttribute(ATTR_INTERNAL_TOKEN_IDENTIFIED, true);

        // Update the context with token
        if (context.get(JWT_CONTEXT_ATTRIBUTE) == null) {
            context.set(JWT_CONTEXT_ATTRIBUTE, new LazyJwtToken(token));
        }

        return true;
    }

    @Override
    public String name() {
        return AUTHENTICATION_HANDLER_NAME;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return POLICIES;
    }

    @Override
    public String tokenType() {
        return TOKEN_TYPE_AUTHORIZATION_BEARER;
    }
}
