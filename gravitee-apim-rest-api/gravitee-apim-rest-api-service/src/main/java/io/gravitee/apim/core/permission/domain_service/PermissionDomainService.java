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
package io.gravitee.apim.core.permission.domain_service;

import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.Arrays;
import java.util.Map;

public interface PermissionDomainService {
    boolean hasExactPermissions(
        String organizationId,
        String userId,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    );

    boolean hasPermission(
        String organizationId,
        String userId,
        RolePermission permission,
        String referenceId,
        RolePermissionAction... acls
    );

    default boolean checkForExactPermissions(Map<String, char[]> userPermissions, Permission permission, RolePermissionAction... acls) {
        if (userPermissions == null) {
            return false;
        }

        char[] permissionAcls = userPermissions.get(permission.getName());
        if (permissionAcls == null) {
            return false;
        }

        var sortedPermissionAcls = new String(permissionAcls).chars().sorted().mapToObj(i -> (char) i).toArray();
        var aclsToCheck = Arrays.stream(acls).map(RolePermissionAction::getId).sorted().toArray();

        return Arrays.equals(sortedPermissionAcls, aclsToCheck);
    }
}
