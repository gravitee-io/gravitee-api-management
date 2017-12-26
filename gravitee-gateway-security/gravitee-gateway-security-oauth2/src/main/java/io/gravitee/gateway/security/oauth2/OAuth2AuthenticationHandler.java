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
package io.gravitee.gateway.security.oauth2;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.HookAuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import io.gravitee.gateway.security.oauth2.policy.CheckSubscriptionPolicy;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

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

    @Override
    public boolean canHandle(Request request) {
        List<String> authorizationHeaders = request.headers().get(HttpHeaders.AUTHORIZATION);

        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return false;
        }

        Optional<String> authorizationBearerHeader = authorizationHeaders
                .stream()
                .filter(h -> StringUtils.startsWithIgnoreCase(h, BEARER_AUTHORIZATION_TYPE))
                .findFirst();

        if (! authorizationBearerHeader.isPresent()) {
            return false;
        }

        String accessToken = authorizationBearerHeader.get().substring(BEARER_AUTHORIZATION_TYPE.length()).trim();
        return ! accessToken.isEmpty();
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
        return Arrays.asList(
                // First, validate the incoming access_token thanks to an OAuth2 authorization server
                (PluginAuthenticationPolicy) () -> AUTHENTICATION_HANDLER_NAME,

                // Then, check that there is an existing valid subscription associated to the client_id
                (HookAuthenticationPolicy) () -> CheckSubscriptionPolicy.class);
    }
}
