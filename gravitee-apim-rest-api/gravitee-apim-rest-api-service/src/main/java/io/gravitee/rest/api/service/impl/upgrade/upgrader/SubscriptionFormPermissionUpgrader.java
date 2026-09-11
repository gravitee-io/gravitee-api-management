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
 * One-shot upgrader that seeds the new {@code SUBSCRIPTION_FORM} environment permission on existing
 * installations. Subscription forms used to be governed by {@code METADATA}: every non-system
 * {@code ENVIRONMENT} role carrying {@code METADATA} ACLs gets the same ACLs copied to
 * {@code SUBSCRIPTION_FORM} ({@code putIfAbsent}, pre-existing values are preserved), so whoever could
 * manage the form keeps that ability once the endpoints switch to the dedicated permission. System
 * roles are refreshed via {@code createOrUpdateSystemRoles} so ADMIN gains the new permission. A role without
 * {@code METADATA} never had access to the form and gets nothing: granting it is an explicit decision of the
 * administrator, not a migration concern.
 * Idempotent — re-running after a successful upgrade is a no-op.
 * Order: {@link UpgraderOrder#SUBSCRIPTION_FORM_PERMISSION_UPGRADER}.
 */
@CustomLog
@Component
public class SubscriptionFormPermissionUpgrader implements Upgrader {

    private static final String METADATA_PERMISSION = EnvironmentPermission.METADATA.getName();
    private static final String SUBSCRIPTION_FORM_PERMISSION = EnvironmentPermission.SUBSCRIPTION_FORM.getName();

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public SubscriptionFormPermissionUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    log.info("Applying subscription form permission for organization {}", organizationId);
                    roleService
                        .findByScope(RoleScope.ENVIRONMENT, organizationId)
                        .stream()
                        .filter(role -> !role.isSystem())
                        .forEach(role -> copyMetadataAcls(executionContext, role));
                    roleService.createOrUpdateSystemRoles(executionContext, organizationId);
                });
            return true;
        });
    }

    private void copyMetadataAcls(ExecutionContext executionContext, RoleEntity role) {
        Map<String, char[]> actualPermissions = role.getPermissions();
        if (
            actualPermissions == null ||
            !actualPermissions.containsKey(METADATA_PERMISSION) ||
            actualPermissions.containsKey(SUBSCRIPTION_FORM_PERMISSION)
        ) {
            return;
        }

        Map<String, char[]> expectedPermissions = new HashMap<>(actualPermissions);
        expectedPermissions.put(SUBSCRIPTION_FORM_PERMISSION, actualPermissions.get(METADATA_PERMISSION).clone());

        UpdateRoleEntity expectedRole = UpdateRoleEntity.from(role);
        expectedRole.setPermissions(expectedPermissions);

        roleService.update(executionContext, expectedRole);
        log.info("Copied METADATA ACLs to SUBSCRIPTION_FORM on role: {}", role.getName());
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.SUBSCRIPTION_FORM_PERMISSION_UPGRADER;
    }
}
