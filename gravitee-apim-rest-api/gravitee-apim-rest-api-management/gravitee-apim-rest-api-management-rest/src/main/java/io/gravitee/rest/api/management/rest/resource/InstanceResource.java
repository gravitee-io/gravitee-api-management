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
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.model.InstanceEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.OrganizationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InstanceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Gateway")
public class InstanceResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private InstanceService instanceService;

    @PathParam("instance")
    private String instance;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a gateway instance")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INSTANCE, acls = RolePermissionAction.READ) })
    public InstanceEntity getInstance() {
        InstanceEntity instanceEntity = instanceService.findByEvent(GraviteeContext.getExecutionContext(), this.instance);
        if (isInstanceAccessibleByEnv(instanceEntity.getEnvironments(), GraviteeContext.getCurrentEnvironment())) {
            return instanceEntity;
        }
        throw new InstanceNotFoundException(instance);
    }

    @Path("monitoring/{gatewayId}")
    public MonitoringResource getMonitoringResource() {
        return resourceContext.getResource(MonitoringResource.class);
    }

    private boolean isInstanceAccessibleByEnv(Set<String> environments, String currentEnvironment) {
        return environments == null || environments.isEmpty() || environments.contains(currentEnvironment);
    }
}
