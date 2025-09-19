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
package io.gravitee.rest.api.service.exceptions;

import static java.util.Collections.singletonMap;

import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultRoleNotFoundException extends AbstractNotFoundException {

    private final String scopes;

    public DefaultRoleNotFoundException(RoleScope... scopes) {
        this.scopes = Arrays.stream(scopes)
            .map(RoleScope::name)
            .reduce((s, s2) -> s + ", " + s2)
            .orElse("null");
    }

    @Override
    public String getMessage() {
        return "Default roles for scopes [" + scopes + "] cannot be found.";
    }

    @Override
    public String getTechnicalCode() {
        return "role.default.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return singletonMap("scopes", scopes);
    }
}
