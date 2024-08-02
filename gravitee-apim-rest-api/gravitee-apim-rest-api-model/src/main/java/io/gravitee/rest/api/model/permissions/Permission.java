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
package io.gravitee.rest.api.model.permissions;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface Permission {
    default String getName() {
        return null;
    }

    default int getMask() {
        return -1;
    }

    static Permission[] findByScope(RoleScope scope) {
        switch (scope) {
            case API:
                return ApiPermission.values();
            case APPLICATION:
                return ApplicationPermission.values();
            case GROUP:
                return GroupPermission.values();
            case ENVIRONMENT:
                return EnvironmentPermission.values();
            case ORGANIZATION:
                return OrganizationPermission.values();
            case INTEGRATION:
                return IntegrationPermission.values();
            default:
                throw new IllegalArgumentException("[" + scope + "] are not a RolePermission");
        }
    }

    static Permission findByScopeAndName(RoleScope scope, String name) {
        for (Permission permission : findByScope(scope)) {
            if (permission.getName().equals(name)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("[" + scope + "] and [" + name + "] are not a RolePermission");
    }
}
