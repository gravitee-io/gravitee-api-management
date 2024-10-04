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

import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.ROLE_ENVIRONMENT_FEDERATION_AGENT;

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
public class EnvironmentFederationAgentRoleUpgrader implements Upgrader {

    private final RoleService roleService;

    private final OrganizationRepository organizationRepository;

    @Autowired
    public EnvironmentFederationAgentRoleUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
        this.roleService = roleService;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public boolean upgrade() {
        try {
            organizationRepository
                .findAll()
                .stream()
                .filter(organization -> shouldCreateEnvironmentFederationAgentRole(organization.getId()))
                .forEach(organization -> {
                    roleService.create(new ExecutionContext(organization.getId()), ROLE_ENVIRONMENT_FEDERATION_AGENT);
                });
        } catch (Exception e) {
            log.error("failed to apply {}", getClass().getSimpleName(), e);
            return false;
        }

        return true;
    }

    private boolean shouldCreateEnvironmentFederationAgentRole(String organizationId) {
        return roleService.findByScopeAndName(ENVIRONMENT, ROLE_ENVIRONMENT_FEDERATION_AGENT.getName(), organizationId).isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.DEFAULT_ROLES_UPGRADER + 1;
    }
}
