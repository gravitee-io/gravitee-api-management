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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Dashboards"})
public class DashboardsResource extends AbstractResource  {

    @Autowired
    private DashboardService dashboardService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<DashboardEntity> list(final @QueryParam("reference_type") DashboardReferenceType referenceType)  {
        if (referenceType == null) {
            return dashboardService.findAll();
        } else {
            return dashboardService.findByReferenceType(referenceType);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.CREATE)
    })
    public DashboardEntity create(@Valid @NotNull final NewDashboardEntity dashboard) {
        return dashboardService.create(dashboard);
    }

    @GET
    @Path("{dashboardId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.READ)
    })
    public DashboardEntity get(final @PathParam("dashboardId") String dashboardId)  {
        return dashboardService.findById(dashboardId);
    }

    @Path("{dashboardId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.UPDATE)
    })
    public DashboardEntity update(@PathParam("dashboardId") String dashboardId, @Valid @NotNull final UpdateDashboardEntity dashboard) {
        dashboard.setId(dashboardId);
        return dashboardService.update(dashboard);
    }

    @Path("{dashboardId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.DELETE)
    })
    public void delete(@PathParam("dashboardId") String dashboardId) {
        dashboardService.delete(dashboardId);
    }
}
