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

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * One-shot upgrader that seeds the new {@code KAFKA_EXPLORER} environment permission on existing installations.
 * <ul>
 *   <li>Every non-system {@code ENVIRONMENT} role that carries {@code CLUSTER} ACLs gets the same ACLs copied to
 *       {@code KAFKA_EXPLORER} ({@code putIfAbsent} — pre-existing values are preserved), so users who could use
 *       the Kafka explorer through {@code ENVIRONMENT_CLUSTER} keep their access when endpoints switch to the
 *       dedicated permission.</li>
 *   <li>System roles are refreshed via {@code createOrUpdateSystemRoles} so ADMIN gains the new permission.</li>
 * </ul>
 * Idempotent — re-running after a successful upgrade is a no-op.
 * Order: {@link UpgraderOrder#KAFKA_EXPLORER_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class KafkaExplorerPermissionUpgrader implements Upgrader {

    private static final String CLUSTER_PERMISSION = EnvironmentPermission.CLUSTER.getName();
    private static final String KAFKA_EXPLORER_PERMISSION = EnvironmentPermission.KAFKA_EXPLORER.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public KafkaExplorerPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() throws UpgraderException {
        return wrapException(() -> {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    ExecutionContext executionContext = new ExecutionContext(organization);
                    String organizationId = executionContext.getOrganizationId();
                    log.info("Applying Kafka explorer permission for organization {}", organizationId);
                    roleService
                        .findByScope(RoleScope.ENVIRONMENT, organizationId)
                        .stream()
                        .filter(role -> !role.isSystem())
                        .forEach(role -> copyClusterAcls(executionContext, role));
                    roleService.createOrUpdateSystemRoles(executionContext, organizationId);
                });
            return true;
        });
    }

    private void copyClusterAcls(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        if (
            actualPermissions == null ||
            !actualPermissions.containsKey(CLUSTER_PERMISSION) ||
            actualPermissions.containsKey(KAFKA_EXPLORER_PERMISSION)
        ) {
            return;
        }

        Map<String, char[]> expectedPermissions = new HashMap<>(actualPermissions);
        expectedPermissions.put(KAFKA_EXPLORER_PERMISSION, actualPermissions.get(CLUSTER_PERMISSION).clone());

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("Copied CLUSTER ACLs to KAFKA_EXPLORER on role: {}", role.getName());
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.KAFKA_EXPLORER_PERMISSION_UPGRADER;
    }
}
