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
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Grants read on {@code HITL_DECISIONS} to the environment {@code USER} role on existing installations, as new ones
 * get it from {@code DEFAULT_ROLE_ENVIRONMENT_USER}. Deciding needs update, which an administrator grants through the
 * roles of the approvers. The environment {@code ADMIN} role needs no upgrade: it is refreshed with every environment
 * permission at each startup. Only adds an action; other roles, custom roles included, stay untouched. Idempotent.
 */
@CustomLog
@Component
public class GammaHitlPermissionsUpgrader implements Upgrader {

    private static final String HITL_DECISIONS_PERMISSION = EnvironmentPermission.HITL_DECISIONS.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public GammaHitlPermissionsUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                        .ifPresent(role -> addDecisionsReadIfMissing(executionContext, role));
                });
            return true;
        });
    }

    private void addDecisionsReadIfMissing(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        char[] actualAcls = actualPermissions == null ? null : actualPermissions.get(HITL_DECISIONS_PERMISSION);
        if (ArrayUtils.contains(actualAcls, READ.getId())) {
            return;
        }

        Map<String, char[]> expectedPermissions = actualPermissions == null ? new HashMap<>() : new HashMap<>(actualPermissions);
        expectedPermissions.put(
            HITL_DECISIONS_PERMISSION,
            ArrayUtils.add(actualAcls == null ? new char[0] : actualAcls.clone(), READ.getId())
        );

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("     - <ENVIRONMENT> {}: added read on HITL decisions", role.getName());
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.GAMMA_HITL_PERMISSIONS_UPGRADER;
    }
}
