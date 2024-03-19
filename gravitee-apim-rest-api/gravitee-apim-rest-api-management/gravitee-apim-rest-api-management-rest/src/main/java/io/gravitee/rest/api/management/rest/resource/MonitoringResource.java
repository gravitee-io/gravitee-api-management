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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.monitoring.MonitoringData;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.MonitoringService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Gateway")
public class MonitoringResource extends AbstractResource {

    @Inject
    private MonitoringService monitoringService;

    @Inject
    private InstanceService instanceService;

    @Inject
    private OrganizationService organizationService;

    @PathParam("instance")
    @Parameter(name = "instance", hidden = true)
    private String instance;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get monitoring metrics for a gateway instance")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_PLATFORM, acls = RolePermissionAction.READ) })
    public MonitoringData getInstanceMonitoring(@PathParam("gatewayId") String gatewayId) {
        InstanceEntity instanceEntity = instanceService.findByEvent(GraviteeContext.getExecutionContext(), this.instance);
        if (
            !isInstanceAccessibleByEnv(instanceEntity.getEnvironments(), GraviteeContext.getCurrentEnvironment()) ||
            !isInstanceAccessibleByOrga(instanceEntity.getOrganizationsHrids(), GraviteeContext.getCurrentOrganization())
        ) {
            throw new InstanceNotFoundException(instance);
        }
        return monitoringService.findMonitoring(gatewayId);
    }

    private boolean isInstanceAccessibleByOrga(List<String> organizationsHrids, String currentOrganization) {
        if (organizationsHrids == null || organizationsHrids.isEmpty()) {
            return true;
        }
        return organizationService
            .findByHrids(new HashSet<>(organizationsHrids))
            .stream()
            .map(OrganizationEntity::getId)
            .anyMatch(id -> id.equalsIgnoreCase(currentOrganization));
    }

    private boolean isInstanceAccessibleByEnv(Set<String> environments, String currentEnvironment) {
        return environments == null || environments.isEmpty() || environments.contains(currentEnvironment);
    }
}
