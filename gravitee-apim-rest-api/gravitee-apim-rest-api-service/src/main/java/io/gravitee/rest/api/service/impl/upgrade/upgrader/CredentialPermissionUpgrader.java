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
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_API_PUBLISHER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Gives the {@code CREDENTIAL} permission to the environment roles of an existing installation: ADMIN through its
 * system role, and API_PUBLISHER with every action, unless an operator already set the permission on it.
 */
@Component
@CustomLog
public class CredentialPermissionUpgrader implements Upgrader {

    private static final String CREDENTIAL = EnvironmentPermission.CREDENTIAL.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public CredentialPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return wrapException(() -> {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    var executionContext = new ExecutionContext(organization);
                    roleService.createOrUpdateSystemRoles(executionContext, organization.getId());
                    grantToApiPublisher(executionContext);
                });
            return true;
        });
    }

    private void grantToApiPublisher(ExecutionContext executionContext) {
        var publisher = roleService.findByScopeAndName(
            ENVIRONMENT,
            ROLE_ENVIRONMENT_API_PUBLISHER.getName(),
            executionContext.getOrganizationId()
        );
        if (publisher.isEmpty()) {
            return;
        }
        Map<String, char[]> existing = publisher.get().getPermissions() == null ? Map.of() : publisher.get().getPermissions();
        if (existing.containsKey(CREDENTIAL)) {
            return;
        }
        // A copy: UpdateRoleEntity.from() hands back the role's own map, which may be read-only.
        var permissions = new HashMap<>(existing);
        permissions.put(CREDENTIAL, new char[] { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() });
        var roleToUpdate = UpdateRoleEntity.from(publisher.get());
        roleToUpdate.setPermissions(permissions);
        log.info("Grant the CREDENTIAL permission to role API_PUBLISHER for organization {}", executionContext.getOrganizationId());
        roleService.update(executionContext, roleToUpdate);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.CREDENTIAL_PERMISSION_UPGRADER;
    }
}
