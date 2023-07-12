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
package io.gravitee.gateway.security.keyless;

import static io.gravitee.gateway.security.core.AuthenticationContext.ATTR_INTERNAL_TOKEN_IDENTIFIED;
import static io.gravitee.gateway.security.core.AuthenticationContext.TOKEN_TYPE_NONE;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.security.core.AuthenticationContext;
import io.gravitee.gateway.security.core.AuthenticationHandler;
import io.gravitee.gateway.security.core.AuthenticationPolicy;
import io.gravitee.gateway.security.core.PluginAuthenticationPolicy;
import java.util.Collections;
import java.util.List;

/**
 * A key-less {@link AuthenticationHandler} meaning that no authentication is required to access
 * the public service.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeylessAuthenticationHandler implements AuthenticationHandler {

    static final String KEYLESS_POLICY = "key-less";

    private static final List<AuthenticationPolicy> POLICIES = Collections.singletonList((PluginAuthenticationPolicy) () -> KEYLESS_POLICY);

    @Override
    public boolean canHandle(AuthenticationContext context) {
        return !Boolean.TRUE.equals(context.getInternalAttribute(ATTR_INTERNAL_TOKEN_IDENTIFIED));
    }

    @Override
    public String name() {
        return "key_less";
    }

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public List<AuthenticationPolicy> handle(ExecutionContext executionContext) {
        return POLICIES;
    }

    @Override
    public String tokenType() {
        return TOKEN_TYPE_NONE;
    }
}
