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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Grants the GUARDIAN environment permission to the default roles on an existing installation.
 *
 * <p>
 * {@link io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition} covers a fresh install only: the roles of an
 * installation that already exists are never rebuilt from it. Without this upgrader the guardian endpoints answer 403
 * to every caller but an organization admin, who bypasses the permission check.
 * </p>
 *
 * @author GraviteeSource Team
 */
@Component
@CustomLog
public class DefaultGuardianRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public DefaultGuardianRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return this.wrapException(() -> {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    ExecutionContext executionContext = new ExecutionContext(organization);
                    grantGuardianPermission(
                        executionContext,
                        ROLE_ENVIRONMENT_API_PUBLISHER.getName(),
                        new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() }
                    );
                    grantGuardianPermission(executionContext, DEFAULT_ROLE_ENVIRONMENT_USER.getName(), new char[] { READ.getId() });
                });
            return true;
        });
    }

    private void grantGuardianPermission(ExecutionContext executionContext, String roleName, char[] permissions) {
        Optional<RoleEntity> role = roleService.findByScopeAndName(ENVIRONMENT, roleName, executionContext.getOrganizationId());
        if (role.isEmpty()) {
            log.debug("Role {} not found for organization {}, nothing to grant", roleName, executionContext.getOrganizationId());
            return;
        }

        RoleEntity existingRole = role.get();
        Map<String, char[]> existingPermissions = existingRole.getPermissions() == null ? Map.of() : existingRole.getPermissions();
        if (existingPermissions.containsKey(EnvironmentPermission.GUARDIAN.getName())) {
            log.debug(
                "Role {} already holds the GUARDIAN permission for organization {}, nothing to grant",
                roleName,
                executionContext.getOrganizationId()
            );
            return;
        }

        // A copy, not the role's own map: UpdateRoleEntity.from() hands back the same instance, so a
        // put() would change the role in place -- and would fail outright on a null or read-only map.
        // Same shape as GammaAuthzPdpPermissionUpgrader.
        Map<String, char[]> permissionsToGrant = new HashMap<>(existingPermissions);
        permissionsToGrant.put(EnvironmentPermission.GUARDIAN.getName(), permissions);

        UpdateRoleEntity roleToUpdate = UpdateRoleEntity.from(existingRole);
        roleToUpdate.setPermissions(permissionsToGrant);

        log.info("Grant the GUARDIAN permission to role {} for organization {}", roleName, executionContext.getOrganizationId());
        roleService.update(executionContext, roleToUpdate);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.GUARDIAN_ROLES_UPGRADER;
    }
}
