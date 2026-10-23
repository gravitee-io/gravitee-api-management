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

import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;

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
 * Gives every custom environment role holding {@code AUTHORIZATION} the same actions on {@code AUTHZ_PDP}, which now
 * guards the PDP gateways and their AuthZEN endpoints. System roles are refreshed by {@link GammaAuthzRolesUpgrader}.
 * Idempotent.
 */
@CustomLog
@Component
public class GammaAuthzPdpPermissionUpgrader implements Upgrader {

    private static final String AUTHORIZATION_PERMISSION = EnvironmentPermission.AUTHORIZATION.getName();
    private static final String AUTHZ_PDP_PERMISSION = EnvironmentPermission.AUTHZ_PDP.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public GammaAuthzPdpPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                        .findByScope(ENVIRONMENT, executionContext.getOrganizationId())
                        .stream()
                        .filter(role -> !role.isSystem())
                        .forEach(role -> copyAuthorizationActionsToAuthzPdp(executionContext, role));
                });
            return true;
        });
    }

    private void copyAuthorizationActionsToAuthzPdp(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        char[] authorizationActions = actualPermissions == null ? null : actualPermissions.get(AUTHORIZATION_PERMISSION);
        if (ArrayUtils.isEmpty(authorizationActions)) {
            return;
        }

        char[] authzPdpActions = actualPermissions.getOrDefault(AUTHZ_PDP_PERMISSION, new char[0]);
        char[] mergedActions = authzPdpActions.clone();
        for (char action : authorizationActions) {
            if (!ArrayUtils.contains(mergedActions, action)) {
                mergedActions = ArrayUtils.add(mergedActions, action);
            }
        }
        if (mergedActions.length == authzPdpActions.length) {
            return;
        }

        Map<String, char[]> expectedPermissions = new HashMap<>(actualPermissions);
        expectedPermissions.put(AUTHZ_PDP_PERMISSION, mergedActions);

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("     - <ENVIRONMENT> {}: copied AUTHORIZATION actions to AUTHZ_PDP", role.getName());
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.GAMMA_AUTHZ_PDP_PERMISSION_UPGRADER;
    }
}
