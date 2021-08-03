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
package io.gravitee.rest.api.service.exceptions;

import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RoleNotFoundException extends AbstractNotFoundException {

    private final String id;
    private final RoleScope roleScope;
    private final String roleName;

    public RoleNotFoundException(String id) {
        this.id = id;
        this.roleScope = null;
        this.roleName = null;
    }

    public RoleNotFoundException(RoleScope roleScope, String roleName) {
        this.id = null;
        this.roleScope = roleScope;
        this.roleName = roleName;
    }

    @Override
    public String getMessage() {
        if (id != null) {
            return "Role [" + id + "] can not be found.";
        } else {
            return "Role [" + roleScope.name() + "_" + roleName + "] can not be found.";
        }
    }

    @Override
    public String getTechnicalCode() {
        return "role.notFound";
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<String, String>() {
            {
                put("id", id);
            }
        };
    }
}
