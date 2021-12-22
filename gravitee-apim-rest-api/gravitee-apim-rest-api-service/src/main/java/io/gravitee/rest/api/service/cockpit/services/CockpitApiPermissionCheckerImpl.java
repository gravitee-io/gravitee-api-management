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
package io.gravitee.rest.api.service.cockpit.services;

import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Julien GIOVARESCO (julien.giovaresco at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class CockpitApiPermissionCheckerImpl implements CockpitApiPermissionChecker {

    private final PermissionService permissionService;

    public CockpitApiPermissionCheckerImpl(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public Optional<String> checkCreatePermission(String userId, String environmentId, DeploymentMode mode) {
        if (isNotAllowedToCreateApi(userId, environmentId)) {
            return Optional.of("You are not allowed to create APIs on this environment.");
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> checkUpdatePermission(String userId, String environmentId, String apiId, DeploymentMode mode) {
        if (isNotAllowedToUpdateApi(userId, environmentId)) {
            return Optional.of("You are not allowed to update APIs on this environment.");
        }

        if (isNotAllowedToUpdateDocumentation(userId, apiId)) {
            return Optional.of("You are not allowed to update the documentation of this API.");
        }

        if (mode != DeploymentMode.API_DOCUMENTED && isNotAllowedToUpdateApiDefinition(userId, apiId)) {
            return Optional.of("You are not allowed to mock and deploy this API.");
        }
        return Optional.empty();
    }

    private boolean isNotAllowedToCreateApi(String userId, String environmentId) {
        return (
            !isAdmin(userId) &&
            !permissionService.hasPermission(userId, RolePermission.ENVIRONMENT_API, environmentId, RolePermissionAction.CREATE)
        );
    }

    private boolean isNotAllowedToUpdateApi(String userId, String environmentId) {
        return (
            !isAdmin(userId) &&
            !permissionService.hasPermission(userId, RolePermission.ENVIRONMENT_API, environmentId, RolePermissionAction.UPDATE)
        );
    }

    private boolean isNotAllowedToUpdateDocumentation(String userId, String apiId) {
        return (
            !isAdmin(userId) &&
            !permissionService.hasPermission(userId, RolePermission.API_DOCUMENTATION, apiId, RolePermissionAction.UPDATE)
        );
    }

    private boolean isNotAllowedToUpdateApiDefinition(String userId, String apiId) {
        return (
            !isAdmin(userId) && !permissionService.hasPermission(userId, RolePermission.API_DEFINITION, apiId, RolePermissionAction.UPDATE)
        );
    }

    protected boolean isAdmin(String userId) {
        return permissionService.hasManagementRights(userId);
    }
}
