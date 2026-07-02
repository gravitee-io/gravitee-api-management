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

import static io.gravitee.rest.api.model.permissions.RoleScope.AI_CATALOG;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_AI_CATALOG_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_AI_CATALOG_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class AiCatalogRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public AiCatalogRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    initializeCatalogRoles(executionContext);
                    roleService.createOrUpdateSystemRoles(executionContext, executionContext.getOrganizationId());
                });
            return true;
        });
    }

    private void initializeCatalogRoles(ExecutionContext executionContext) {
        if (shouldCreateRole(executionContext, ROLE_AI_CATALOG_OWNER.getName())) {
            log.info("     - <AI_CATALOG> OWNER");
            roleService.create(executionContext, ROLE_AI_CATALOG_OWNER);
        }
        if (shouldCreateRole(executionContext, ROLE_AI_CATALOG_USER.getName())) {
            log.info("     - <AI_CATALOG> USER");
            roleService.create(executionContext, ROLE_AI_CATALOG_USER);
        }
    }

    private boolean shouldCreateRole(final ExecutionContext executionContext, String roleName) {
        return roleService.findByScopeAndName(AI_CATALOG, roleName, executionContext.getOrganizationId()).isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.AI_CATALOG_ROLES_UPGRADER;
    }
}
