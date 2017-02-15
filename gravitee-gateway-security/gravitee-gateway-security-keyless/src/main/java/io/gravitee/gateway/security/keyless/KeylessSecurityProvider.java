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
package io.gravitee.gateway.security.keyless;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.security.core.SecurityPolicy;
import io.gravitee.gateway.security.core.SecurityProvider;

/**
 * A key-less {@link SecurityProvider} meaning that no authentication is required to access
 * the public service.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class KeylessSecurityProvider implements SecurityProvider {

    static final String KEYLESS_POLICY = "key-less";

    static final SecurityPolicy POLICY = new SecurityPolicy() {
        @Override
        public String policy() {
            return KEYLESS_POLICY;
        }

        @Override
        public String configuration() {
            return null;
        }
    };

    @Override
    public boolean canHandle(Request request) {
        return true;
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
    public SecurityPolicy create(ExecutionContext executionContext) {
        return POLICY;
    }
}
