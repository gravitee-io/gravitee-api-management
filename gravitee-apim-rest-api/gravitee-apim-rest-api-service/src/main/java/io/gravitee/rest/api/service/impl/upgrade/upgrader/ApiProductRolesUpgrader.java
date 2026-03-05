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

import static io.gravitee.rest.api.model.permissions.RoleScope.API_PRODUCT;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_PRODUCT_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.NewRoleEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class ApiProductRolesUpgrader implements Upgrader {

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public ApiProductRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    initializeApiProductRoles(executionContext);
                    ensureApiProductRolesHaveNotificationPermission(executionContext);
                    ensureApiProductPrimaryOwner(executionContext, organization.getId());
                });
            return true;
        });
    }

    private void initializeApiProductRoles(ExecutionContext executionContext) {
        createRoleIfMissing(executionContext, ROLE_API_PRODUCT_USER);
        createRoleIfMissing(executionContext, ROLE_API_PRODUCT_OWNER);
    }

    private void createRoleIfMissing(ExecutionContext executionContext, NewRoleEntity role) {
        if (roleService.findByScopeAndName(API_PRODUCT, role.getName(), executionContext.getOrganizationId()).isEmpty()) {
            log.info("     - <API_PRODUCT> {}", role.getName());
            roleService.create(executionContext, role);
        }
    }

    private void ensureApiProductRolesHaveNotificationPermission(ExecutionContext executionContext) {
        String orgId = executionContext.getOrganizationId();
        roleService
            .findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_OWNER.getName(), orgId)
            .ifPresent(role ->
                addNotificationPermissionIfMissing(
                    executionContext,
                    role,
                    ROLE_API_PRODUCT_OWNER.getPermissions().get(ApiProductPermission.NOTIFICATION.getName())
                )
            );
        roleService
            .findByScopeAndName(API_PRODUCT, ROLE_API_PRODUCT_USER.getName(), orgId)
            .ifPresent(role ->
                addNotificationPermissionIfMissing(
                    executionContext,
                    role,
                    ROLE_API_PRODUCT_USER.getPermissions().get(ApiProductPermission.NOTIFICATION.getName())
                )
            );
    }

    private void addNotificationPermissionIfMissing(ExecutionContext executionContext, RoleEntity role, char[] expectedNotificationPerms) {
        if (expectedNotificationPerms == null) {
            return;
        }
        Map<String, char[]> perms = role.getPermissions();
        String notifKey = ApiProductPermission.NOTIFICATION.getName();
        if (perms != null && perms.containsKey(notifKey)) {
            return;
        }
        Map<String, char[]> newPerms = perms == null ? new HashMap<>() : new HashMap<>(perms);
        newPerms.put(notifKey, expectedNotificationPerms);
        UpdateRoleEntity update = UpdateRoleEntity.from(role);
        update.setPermissions(newPerms);
        roleService.update(executionContext, update);
        log.info("     - <API_PRODUCT> {}: added NOTIFICATION permission", role.getName());
    }

    private void ensureApiProductPrimaryOwner(ExecutionContext executionContext, String organizationId) {
        roleService.createOrUpdateSystemRole(executionContext, PRIMARY_OWNER, API_PRODUCT, ApiProductPermission.values(), organizationId);
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.API_PRODUCT_ROLES_UPGRADER;
    }
}
