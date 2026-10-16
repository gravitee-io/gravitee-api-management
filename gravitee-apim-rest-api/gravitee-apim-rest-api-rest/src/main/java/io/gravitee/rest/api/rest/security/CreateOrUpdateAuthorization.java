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
package io.gravitee.rest.api.rest.security;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Authorizes a create-or-update import by whether its target already exists: the create permission on the environment
 * when it is absent, the update permission on the object when it is present.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CreateOrUpdateAuthorization {

    /**
     * @param existingId the id of the object the import targets, or {@code null} when it does not exist yet
     * @throws ForbiddenAccessException when the caller lacks the permission the case requires
     */
    public static void require(
        PermissionService permissionService,
        ExecutionContext executionContext,
        RolePermission createPermission,
        RolePermission updatePermission,
        String existingId
    ) {
        boolean allowed = existingId == null
            ? permissionService.hasPermission(
                executionContext,
                createPermission,
                environmentReference(executionContext, createPermission),
                RolePermissionAction.CREATE
            )
            : permissionService.hasPermission(
                executionContext,
                updatePermission,
                updatePermission.getScope() == RoleScope.ENVIRONMENT ? executionContext.getEnvironmentId() : existingId,
                RolePermissionAction.UPDATE
            );
        if (!allowed) {
            throw new ForbiddenAccessException();
        }
    }

    private static String environmentReference(ExecutionContext executionContext, RolePermission createPermission) {
        // Imports are addressed per environment: granting creation on any other scope would fail open.
        if (createPermission.getScope() != RoleScope.ENVIRONMENT) {
            throw new IllegalStateException("Create permission " + createPermission + " is not environment-scoped");
        }
        return executionContext.getEnvironmentId();
    }
}
