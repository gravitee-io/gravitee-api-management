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
 * One-shot upgrader that backfills the API {@code LIVE_SESSION} permission on built-in roles across every organization.
 * <ul>
 *   <li>Built-in API {@code OWNER} role: if {@code LIVE_SESSION} is missing, it gets the same actions the role holds on
 *       {@code LOG}, so an organization that restricted {@code LOG} to keep payloads away does not get body access back;
 *       without {@code LOG} actions the role is left as is, and an existing {@code LIVE_SESSION} value is kept.</li>
 *   <li>API {@code PRIMARY_OWNER} system role: regenerated with the full {@link ApiPermission} set minus {@code REVIEWS},
 *       matching the convention in {@code RoleServiceImpl.createOrUpdateSystemRoles}.</li>
 *   <li>Other roles ({@code USER}, {@code REVIEWER}, custom roles) are untouched.</li>
 * </ul>
 * Idempotent: re-running after a successful upgrade is a no-op for {@code OWNER} and a re-sync for {@code PRIMARY_OWNER}.
 * Order: {@link UpgraderOrder#LIVE_SESSION_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class LiveSessionPermissionUpgrader implements Upgrader {

    private static final String LIVE_SESSION_PERMISSION = ApiPermission.LIVE_SESSION.getName();
    private static final String LOG_PERMISSION = ApiPermission.LOG.getName();
    private static final Permission[] PRIMARY_OWNER_PERMISSIONS = Arrays.stream(ApiPermission.values())
        .filter(permission -> permission != REVIEWS)
        .toArray(Permission[]::new);

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public LiveSessionPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                log.info("Applying live session permission for organization {} ({}/{})", organizationId, n, total);
                try {
                    permitOwner(executionContext);
                    syncPrimaryOwner(executionContext);
                } catch (RuntimeException e) {
                    log.error(
                        "Failed to apply live session permission for organization {} after {} of {} processed",
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
        Map<String, char[]> actualPermissions = role.getPermissions();
        if (actualPermissions == null || actualPermissions.containsKey(LIVE_SESSION_PERMISSION)) {
            return;
        }
        char[] logActions = actualPermissions.get(LOG_PERMISSION);
        if (logActions == null || logActions.length == 0) {
            return;
        }

        Map<String, char[]> expectedPermissions = new HashMap<>(actualPermissions);
        expectedPermissions.put(LIVE_SESSION_PERMISSION, logActions.clone());

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("Added live session permission to API role {}", role.getName());
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
        return UpgraderOrder.LIVE_SESSION_PERMISSION_UPGRADER;
    }
}
