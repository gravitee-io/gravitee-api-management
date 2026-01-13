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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class DefaultInstanceRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public DefaultInstanceRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    updateDefaultEnvironmentRoles(executionContext, DEFAULT_ROLE_ENVIRONMENT_USER.getName(), new char[] { READ.getId() });
                });
            return true;
        });
    }

    private void updateDefaultEnvironmentRoles(ExecutionContext executionContext, String roleName, char[] permissions) {
        Optional<RoleEntity> role = roleService.findByScopeAndName(RoleScope.ENVIRONMENT, roleName, executionContext.getOrganizationId());
        if (role.isEmpty()) {
            return;
        }

        char[] rolePermissions = role.get().getPermissions().get(EnvironmentPermission.INSTANCE.getName());
        if (rolePermissions != null && rolePermissions.length > 0) {
            return;
        }

        var roleToUpdate = convert(role.get());

        roleToUpdate.getPermissions().put(EnvironmentPermission.INSTANCE.getName(), permissions);

        log.info("Update default role {} for environment {} with INSTANCE permission", roleName, executionContext.getOrganizationId());
        try {
            roleService.update(executionContext, roleToUpdate);
        } catch (Exception e) {
            log.error("Failed to update default role {} for environment {}", roleName, executionContext.getOrganizationId(), e);
        }
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.DEFAULT_ROLES_UPGRADER;
    }

    private UpdateRoleEntity convert(final RoleEntity roleEntity) {
        if (roleEntity == null) {
            return null;
        }
        final UpdateRoleEntity role = new UpdateRoleEntity();
        role.setId(roleEntity.getId());
        role.setName(roleEntity.getName());
        role.setDescription(roleEntity.getDescription());
        role.setScope(roleEntity.getScope());
        role.setDefaultRole(roleEntity.isDefaultRole());
        role.setPermissions(roleEntity.getPermissions());
        return role;
    }
}
