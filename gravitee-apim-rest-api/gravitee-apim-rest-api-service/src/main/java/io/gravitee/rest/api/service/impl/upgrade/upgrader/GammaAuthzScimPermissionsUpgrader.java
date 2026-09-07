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
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_GAMMA_AUTHZ_ADMIN;

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
import lombok.CustomLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Grants {@code AUTHZ_SCIM_CONNECTION} and {@code AUTHZ_SCIM_SYNC} to the {@code GAMMA_AUTHZ_ADMIN} role on existing
 * installations. Idempotent.
 */
@CustomLog
@Component
public class GammaAuthzScimPermissionsUpgrader implements Upgrader {

    private static final String SCIM_CONNECTION_PERMISSION = EnvironmentPermission.AUTHZ_SCIM_CONNECTION.getName();
    private static final String SCIM_SYNC_PERMISSION = EnvironmentPermission.AUTHZ_SCIM_SYNC.getName();
    private static final char[] FULL_ACLS = { CREATE.getId(), READ.getId(), UPDATE.getId(), DELETE.getId() };

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public GammaAuthzScimPermissionsUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                        .findByScopeAndName(ENVIRONMENT, ROLE_ENVIRONMENT_GAMMA_AUTHZ_ADMIN.getName(), executionContext.getOrganizationId())
                        .ifPresent(role -> addScimPermissionsIfMissing(executionContext, role));
                });
            return true;
        });
    }

    private void addScimPermissionsIfMissing(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        if (grantsFullAcls(actualPermissions, SCIM_CONNECTION_PERMISSION) && grantsFullAcls(actualPermissions, SCIM_SYNC_PERMISSION)) {
            return;
        }

        Map<String, char[]> expectedPermissions = actualPermissions == null ? new HashMap<>() : new HashMap<>(actualPermissions);
        expectedPermissions.put(SCIM_CONNECTION_PERMISSION, FULL_ACLS.clone());
        expectedPermissions.put(SCIM_SYNC_PERMISSION, FULL_ACLS.clone());

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("     - <ENVIRONMENT> {}: added SCIM connection and SCIM sync permissions", role.getName());
    }

    private static boolean grantsFullAcls(Map<String, char[]> permissions, String permission) {
        char[] actualAcls = permissions == null ? null : permissions.get(permission);
        for (char acl : FULL_ACLS) {
            if (!ArrayUtils.contains(actualAcls, acl)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.GAMMA_AUTHZ_SCIM_PERMISSIONS_UPGRADER;
    }
}
