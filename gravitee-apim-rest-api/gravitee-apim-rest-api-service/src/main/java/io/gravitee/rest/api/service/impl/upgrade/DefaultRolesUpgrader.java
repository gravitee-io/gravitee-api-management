/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.upgrade;

import static io.gravitee.rest.api.model.permissions.RoleScope.*;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.*;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.service.InstallationService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class DefaultRolesUpgrader extends OneShotUpgrader {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRolesUpgrader.class);

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public DefaultRolesUpgrader(RoleService roleService, OrganizationRepository organizationRepository) {
        super(InstallationService.DEFAULT_ROLES_UPGRADER_STATUS);
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    protected void processOneShotUpgrade() throws TechnicalException {
        organizationRepository
            .findAll()
            .forEach(
                organization -> {
                    ExecutionContext executionContext = new ExecutionContext(organization);
                    initializeDefaultRoles(executionContext);
                }
            );
    }

    private void initializeDefaultRoles(ExecutionContext executionContext) {
        if (roleService.findAllByOrganization(executionContext.getOrganizationId()).isEmpty()) {
            roleService.initialize(executionContext, executionContext.getOrganizationId());
        } else if (shouldCreateApiReviewerRole(executionContext)) {
            LOGGER.info("     - <API> REVIEWER");
            roleService.create(executionContext, ROLE_API_REVIEWER);
        }
    }

    private boolean shouldCreateApiReviewerRole(final ExecutionContext executionContext) {
        return roleService.findByScopeAndName(API, ROLE_API_REVIEWER.getName(), executionContext.getOrganizationId()).isEmpty();
    }

    @Override
    public int getOrder() {
        return 150;
    }
}
