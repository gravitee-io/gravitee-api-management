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
package io.gravitee.gateway.security.jwt;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.core.TokenExtractor;
import java.util.Arrays;
import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JWTAuthenticationHandler implements AuthenticationHandler {

    /**
     * The name of the authentication handler, which is also the name of the policy to invoke for coherency.
     */
    static final String AUTHENTICATION_HANDLER_NAME = "jwt";

    static final String JWT_CONTEXT_ATTRIBUTE = "jwt";

    private static final List<AuthenticationPolicy> POLICIES = Arrays.asList(
        // First, validate the incoming access_token thanks to an OAuth2 authorization server
        (PluginAuthenticationPolicy) () -> AUTHENTICATION_HANDLER_NAME
    );

    @Override
    public boolean canHandle(AuthenticationContext context) {
        String token = readToken(context.request());

        if (token == null || token.isEmpty()) {
            return false;
        }

        // Update the context with token
        if (context.get(JWT_CONTEXT_ATTRIBUTE) == null) {
            context.set(JWT_CONTEXT_ATTRIBUTE, new LazyJwtToken(token));
        }

        return true;
    }

    private String readToken(Request request) {
        return TokenExtractor.extract(request);
    }

    @Override
    public String name() {
        return AUTHENTICATION_HANDLER_NAME;
    }

    @Override
    public int order() {
        return 0;
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return POLICIES;
    }
}
