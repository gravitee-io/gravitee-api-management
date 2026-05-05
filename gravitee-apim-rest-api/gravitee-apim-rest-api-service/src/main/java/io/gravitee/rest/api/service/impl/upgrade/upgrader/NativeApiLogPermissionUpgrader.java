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

import static io.gravitee.rest.api.model.permissions.ApiPermission.REVIEWS;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RoleScope.API;
import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_OWNER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.Permission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-shot upgrader that backfills the new native API log permissions on built-in roles across every organization.
 * <ul>
 *   <li>Built-in API {@code OWNER} role: adds {@code NATIVE_LOG[READ]} and {@code NATIVE_ANALYTICS[READ]} if missing
 *       ({@code putIfAbsent} — pre-existing values are preserved).</li>
 *   <li>API {@code PRIMARY_OWNER} system role: regenerated with the full {@link ApiPermission} set minus {@code REVIEWS},
 *       matching the convention in {@code RoleServiceImpl.createOrUpdateSystemRoles}.</li>
 *   <li>Other roles ({@code USER}, {@code REVIEWER}, custom roles) are untouched.</li>
 * </ul>
 * Idempotent — re-running after a successful upgrade is a no-op for {@code OWNER} and a re-sync for {@code PRIMARY_OWNER}.
 * Order: {@link UpgraderOrder#NATIVE_API_LOG_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class NativeApiLogPermissionUpgrader implements Upgrader {

    private static final String NATIVE_LOG_PERMISSION = ApiPermission.NATIVE_LOG.getName();
    private static final String NATIVE_ANALYTICS_PERMISSION = ApiPermission.NATIVE_ANALYTICS.getName();
    private static final char[] READ_ONLY = { READ.getId() };
    private static final Permission[] PRIMARY_OWNER_PERMISSIONS = Arrays.stream(ApiPermission.values())
        .filter(permission -> permission != REVIEWS)
        .toArray(Permission[]::new);

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public NativeApiLogPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return wrapException(() -> {
            var organizations = organizationRepository.findAll();
            int total = organizations.size();
            AtomicInteger processed = new AtomicInteger();
            organizations.forEach(organization -> {
                var executionContext = new ExecutionContext(organization);
                String organizationId = executionContext.getOrganizationId();
                int n = processed.incrementAndGet();
                log.info("Applying Native API connection log permissions for organization {} ({}/{})", organizationId, n, total);
                try {
                    permitOwner(executionContext);
                    syncPrimaryOwner(executionContext);
                } catch (RuntimeException e) {
                    log.error(
                        "Failed to apply Native API connection log permissions for organization {} after {} of {} processed",
                        organizationId,
                        n - 1,
                        total,
                        e
                    );
                    throw e;
                }
            });
            return true;
        });
    }

    private void permitOwner(ExecutionContext executionContext) {
        roleService
            .findByScopeAndName(API, ROLE_API_OWNER.getName(), executionContext.getOrganizationId())
            .ifPresent(role -> permit(executionContext, role));
    }

    private void permit(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermission = role.getPermissions();
        boolean hasExpected =
            actualPermission != null &&
            actualPermission.containsKey(NATIVE_LOG_PERMISSION) &&
            actualPermission.containsKey(NATIVE_ANALYTICS_PERMISSION);
        if (hasExpected) {
            return;
        }

        Map<String, char[]> expectedPermission = actualPermission == null ? new HashMap<>() : new HashMap<>(actualPermission);
        expectedPermission.putIfAbsent(NATIVE_LOG_PERMISSION, READ_ONLY);
        expectedPermission.putIfAbsent(NATIVE_ANALYTICS_PERMISSION, READ_ONLY);

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermission);

        roleService.update(executionContext, expectedRole);
        log.info("Added native API permission: {}", role.getName());
    }

    private void syncPrimaryOwner(ExecutionContext executionContext) {
        roleService.createOrUpdateSystemRole(
            executionContext,
            PRIMARY_OWNER,
            API,
            PRIMARY_OWNER_PERMISSIONS,
            executionContext.getOrganizationId()
        );
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.NATIVE_API_LOG_PERMISSION_UPGRADER;
    }
}
