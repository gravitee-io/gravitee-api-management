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
import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_EDGE_MANAGER;

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
 * Grants {@code ENVIRONMENT_API:CREATE} to the {@code EDGE_MANAGER} environment role on existing installations, so an
 * Edge manager can create the default gateway API backing an unmapped interception route.
 * <ul>
 *   <li>Only the role named {@code EDGE_MANAGER} is targeted, customer roles are left untouched.</li>
 *   <li>Only the {@code C} ACL is added to the {@code API} permission, already granted ACLs are preserved.</li>
 * </ul>
 * Idempotent, re-running after a successful upgrade is a no-op.
 * Order: {@link UpgraderOrder#EDGE_MANAGER_API_CREATE_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class EdgeManagerApiCreatePermissionUpgrader implements Upgrader {

    private static final String API_PERMISSION = EnvironmentPermission.API.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public EdgeManagerApiCreatePermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                        .findByScopeAndName(ENVIRONMENT, ROLE_ENVIRONMENT_EDGE_MANAGER.getName(), executionContext.getOrganizationId())
                        .ifPresent(role -> addApiCreateAclIfMissing(executionContext, role));
                });
            return true;
        });
    }

    private void addApiCreateAclIfMissing(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        char[] actualApiAcls = actualPermissions == null ? null : actualPermissions.get(API_PERMISSION);
        if (ArrayUtils.contains(actualApiAcls, CREATE.getId())) {
            return;
        }

        Map<String, char[]> expectedPermissions = actualPermissions == null ? new HashMap<>() : new HashMap<>(actualPermissions);
        expectedPermissions.put(API_PERMISSION, ArrayUtils.add(actualApiAcls, CREATE.getId()));

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("     - <ENVIRONMENT> {}: added API CREATE permission", role.getName());
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.EDGE_MANAGER_API_CREATE_PERMISSION_UPGRADER;
    }
}
