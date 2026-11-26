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
import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.DEFAULT_ROLE_ENVIRONMENT_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Removes the {@code ENVIRONMENT_DOCUMENTATION} permission Gravitee used to ship in the environment {@code USER} role, so
 * existing installations match the stock role new installations get.
 * <ul>
 *   <li>Only the environment-scope role named {@code USER} is targeted; every other role is left untouched.</li>
 *   <li>The permission is removed only while it still holds the stock grant, {@code READ} alone. A grant an administrator
 *   changed is kept as it is and logged.</li>
 *   <li>Every other permission of the role is preserved.</li>
 * </ul>
 * Idempotent, re-running after a successful upgrade is a no-op.
 * Order: {@link UpgraderOrder#ENVIRONMENT_USER_DOCUMENTATION_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class EnvironmentUserDocumentationPermissionUpgrader implements Upgrader {

    private static final String DOCUMENTATION_PERMISSION = EnvironmentPermission.DOCUMENTATION.getName();
    private static final char[] STOCK_DOCUMENTATION_ACLS = { READ.getId() };

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public EnvironmentUserDocumentationPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    roleService
                        .findByScopeAndName(ENVIRONMENT, DEFAULT_ROLE_ENVIRONMENT_USER.getName(), executionContext.getOrganizationId())
                        .ifPresent(role -> removeStockDocumentationGrant(executionContext, role));
                });
            return true;
        });
    }

    private void removeStockDocumentationGrant(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        if (actualPermissions == null || !actualPermissions.containsKey(DOCUMENTATION_PERMISSION)) {
            return;
        }
        if (!Arrays.equals(actualPermissions.get(DOCUMENTATION_PERMISSION), STOCK_DOCUMENTATION_ACLS)) {
            log.warn(
                "     - <ENVIRONMENT> {} of organization {}: DOCUMENTATION permission was customized, kept as it is",
                role.getName(),
                executionContext.getOrganizationId()
            );
            return;
        }

        Map<String, char[]> expectedPermissions = new HashMap<>(actualPermissions);
        expectedPermissions.remove(DOCUMENTATION_PERMISSION);

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info(
            "     - <ENVIRONMENT> {} of organization {}: removed DOCUMENTATION permission",
            role.getName(),
            executionContext.getOrganizationId()
        );
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.ENVIRONMENT_USER_DOCUMENTATION_PERMISSION_UPGRADER;
    }
}
