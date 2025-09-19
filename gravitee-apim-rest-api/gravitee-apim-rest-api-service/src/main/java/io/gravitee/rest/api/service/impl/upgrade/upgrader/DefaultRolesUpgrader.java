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

import static io.gravitee.rest.api.model.permissions.RoleScope.API;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_API_REVIEWER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class DefaultRolesUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public DefaultRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    initializeDefaultRoles(executionContext);
                    roleService.createOrUpdateSystemRoles(executionContext, executionContext.getOrganizationId());
                });
            return true;
        });
    }

    private void initializeDefaultRoles(ExecutionContext executionContext) {
        if (roleService.findAllByOrganization(executionContext.getOrganizationId()).isEmpty()) {
            roleService.initialize(executionContext, executionContext.getOrganizationId());
        } else if (shouldCreateApiReviewerRole(executionContext)) {
            log.info("     - <API> REVIEWER");
            roleService.create(executionContext, ROLE_API_REVIEWER);
        }
    }

    private boolean shouldCreateApiReviewerRole(final ExecutionContext executionContext) {
        return roleService.findByScopeAndName(API, ROLE_API_REVIEWER.getName(), executionContext.getOrganizationId()).isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.DEFAULT_ROLES_UPGRADER;
    }
}
