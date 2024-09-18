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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.param.InstanceSearchParam;
import io.gravitee.rest.api.model.InstanceListItem;
import io.gravitee.rest.api.model.InstanceQuery;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.InstanceService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CloudEnabledException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Gateway")
public class InstancesResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private InstanceService instanceService;

    @Inject
    private ParameterService parameterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List gateway instances")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_INSTANCE, acls = RolePermissionAction.READ) })
    public Page<InstanceListItem> getInstances(@BeanParam InstanceSearchParam param) {
        if (cloudEnabled()) {
            throw new CloudEnabledException();
        }

        InstanceQuery query = new InstanceQuery();
        query.setIncludeStopped(param.isIncludeStopped());
        query.setFrom(param.getFrom());
        query.setTo(param.getTo());
        query.setPage(param.getPage());
        query.setSize(param.getSize());

        return instanceService.search(GraviteeContext.getExecutionContext(), query);
    }

    @Path("{instance}")
    public InstanceResource getInstanceResource() {
        return resourceContext.getResource(InstanceResource.class);
    }

    private Boolean cloudEnabled() {
        return parameterService.findAsBoolean(
            GraviteeContext.getExecutionContext(),
            Key.CLOUD_HOSTED_ENABLED,
            GraviteeContext.getCurrentOrganization(),
            ParameterReferenceType.ORGANIZATION
        );
    }
}
