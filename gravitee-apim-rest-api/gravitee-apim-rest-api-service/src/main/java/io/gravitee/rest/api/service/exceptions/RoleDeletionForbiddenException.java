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

import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleDeletionForbiddenException extends AbstractNotFoundException {

    private final String scope;
    private final String name;

    public RoleDeletionForbiddenException(RoleScope scope, String name) {
        this.scope = scope.name();
        this.name = name;
    }

    @Override
    public String getMessage() {
        return "Role [" + scope + "," + name + "] cannot be deleted because marked as System or Default role.";
    }

    @Override
    public String getTechnicalCode() {
        return "role.notDeletable";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scope", scope);
        parameters.put("name", name);
        return parameters;
    }
}
