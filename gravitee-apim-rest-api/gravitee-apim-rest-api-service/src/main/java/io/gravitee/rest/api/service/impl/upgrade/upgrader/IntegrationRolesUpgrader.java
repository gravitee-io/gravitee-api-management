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

import static io.gravitee.rest.api.model.permissions.RoleScope.INTEGRATION;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_INTEGRATION_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_INTEGRATION_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IntegrationRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public IntegrationRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            organizationRepository
                .findAll()
                .forEach(organization -> {
                    ExecutionContext executionContext = new ExecutionContext(organization);
                    initializeIntegrationRoles(executionContext);
                    roleService.createOrUpdateSystemRoles(executionContext, executionContext.getOrganizationId());
                });
        } catch (Exception e) {
            log.error("failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    private void initializeIntegrationRoles(ExecutionContext executionContext) {
        if (shouldCreateIntegrationOwnerRole(executionContext)) {
            log.info("     - <INTEGRATION> OWNER");
            roleService.create(executionContext, ROLE_INTEGRATION_OWNER);
        }
        if (shouldCreateIntegrationUserRole(executionContext)) {
            log.info("     - <INTEGRATION> USER");
            roleService.create(executionContext, ROLE_INTEGRATION_USER);
        }
    }

    private boolean shouldCreateIntegrationOwnerRole(final ExecutionContext executionContext) {
        return roleService
            .findByScopeAndName(INTEGRATION, ROLE_INTEGRATION_OWNER.getName(), executionContext.getOrganizationId())
            .isEmpty();
    }

    private boolean shouldCreateIntegrationUserRole(final ExecutionContext executionContext) {
        return roleService.findByScopeAndName(INTEGRATION, ROLE_INTEGRATION_USER.getName(), executionContext.getOrganizationId()).isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.INTEGRATION_ROLES_UPGRADER;
    }
}
