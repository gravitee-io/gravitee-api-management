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
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;

import io.gravitee.node.api.upgrader.Upgrader;
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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class DefaultEnvironmentFlowsRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public DefaultEnvironmentFlowsRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    ExecutionContext executionContext = new ExecutionContext(organization);
                    updateDefaultAPIPublisherRoles(
                        executionContext,
                        ROLE_ENVIRONMENT_API_PUBLISHER.getName(),
                        new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() }
                    );
                    updateDefaultAPIPublisherRoles(executionContext, DEFAULT_ROLE_ENVIRONMENT_USER.getName(), new char[] { READ.getId() });
                });
        } catch (Exception e) {
            log.error("failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    private void updateDefaultAPIPublisherRoles(ExecutionContext executionContext, String roleName, char[] permissions) {
        Optional<RoleEntity> role = roleService.findByScopeAndName(RoleScope.ENVIRONMENT, roleName, executionContext.getOrganizationId());
        if (role.isEmpty()) {
            return;
        }
        var roleToUpdate = convert(role.get());

        roleToUpdate.getPermissions().put(EnvironmentPermission.ENVIRONMENT_FLOWS.getName(), permissions);

        log.info("Update default role {} for environment {}", roleName, executionContext.getOrganizationId());
        roleService.update(executionContext, roleToUpdate);
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
