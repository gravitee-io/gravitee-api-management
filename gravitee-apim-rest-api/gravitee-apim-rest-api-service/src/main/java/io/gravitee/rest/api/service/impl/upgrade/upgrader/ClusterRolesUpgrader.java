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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.READ;
import static io.gravitee.rest.api.model.permissions.RoleScope.CLUSTER;
import static io.gravitee.rest.api.model.permissions.RoleScope.ENVIRONMENT;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.CLUSTER_ROLE_OWNER;
import static io.gravitee.rest.api.service.common.DefaultRoleEntityDefinition.CLUSTER_ROLE_USER;

import io.gravitee.node.api.upgrader.Upgrader;
import io.gravitee.node.api.upgrader.UpgraderException;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UpdateRoleEntity;
import io.gravitee.rest.api.model.permissions.EnvironmentPermission;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClusterRolesUpgrader implements Upgrader {

    private final RoleService roleService;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public ClusterRolesUpgrader(RoleService roleService, @Lazy OrganizationRepository organizationRepository) {
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
                    updateEnvironmentApiPublisher(executionContext, organization.getId());
                    initializeClusterRoles(executionContext);
                    roleService.createOrUpdateSystemRoles(executionContext, executionContext.getOrganizationId());
                });
            return true;
        });
    }

    private void updateEnvironmentApiPublisher(ExecutionContext executionContext, String organizationId) {
        Optional<RoleEntity> environmentApiPublisher = roleService.findByScopeAndName(ENVIRONMENT, "API_PUBLISHER", organizationId);
        if (environmentApiPublisher.isPresent()) {
            UpdateRoleEntity updateRole = UpdateRoleEntity.from(environmentApiPublisher.get());
            updateRole.getPermissions().put(EnvironmentPermission.CLUSTER.getName(), new char[] { READ.getId() });
            roleService.update(executionContext, updateRole);
        }
    }

    private void initializeClusterRoles(ExecutionContext executionContext) {
        if (shouldCreateClusterUserRole(executionContext)) {
            log.info("     - <CLUSTER> USER");
            roleService.create(executionContext, CLUSTER_ROLE_USER);
        }
        if (shouldCreateClusterOwnerRole(executionContext)) {
            log.info("     - <CLUSTER> OWNER");
            roleService.create(executionContext, CLUSTER_ROLE_OWNER);
        }
    }

    private boolean shouldCreateClusterUserRole(final ExecutionContext executionContext) {
        return roleService.findByScopeAndName(CLUSTER, CLUSTER_ROLE_USER.getName(), executionContext.getOrganizationId()).isEmpty();
    }

    private boolean shouldCreateClusterOwnerRole(final ExecutionContext executionContext) {
        return roleService.findByScopeAndName(CLUSTER, CLUSTER_ROLE_OWNER.getName(), executionContext.getOrganizationId()).isEmpty();
    }

    @Override
    public int getOrder() {
        return UpgraderOrder.CLUSTER_ROLES_UPGRADER;
    }
}
