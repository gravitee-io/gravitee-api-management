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
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.DashboardEntity;
import io.gravitee.rest.api.model.NewDashboardEntity;
import io.gravitee.rest.api.model.UpdateDashboardEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.DashboardService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Dashboards")
public class DashboardsResource extends AbstractResource {

    @Inject
    private DashboardService dashboardService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Retrieve the list of platform dashboards")
    @ApiResponse(
        responseCode = "200",
        description = "List of platform dashboards",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = DashboardEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public List<DashboardEntity> getDashboards(final @QueryParam("reference_type") DashboardReferenceType referenceType) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            !hasPermission(executionContext, RolePermission.ENVIRONMENT_DASHBOARD, RolePermissionAction.READ) &&
            !hasPermission(executionContext, RolePermission.ENVIRONMENT_API, RolePermissionAction.READ) &&
            !canReadAPIConfiguration() &&
            !DashboardReferenceType.HOME.equals(referenceType)
        ) {
            throw new ForbiddenAccessException();
        }
        if (referenceType == null) {
            return dashboardService.findAll();
        } else {
            return dashboardService.findByReferenceType(referenceType);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a platform dashboard",
        description = "User must have the MANAGEMENT_DASHBOARD[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Dashboard successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DashboardEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.CREATE) })
    public DashboardEntity createDashboard(@Valid @NotNull final NewDashboardEntity dashboard) {
        return dashboardService.create(GraviteeContext.getExecutionContext(), dashboard);
    }

    @GET
    @Path("{dashboardId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve a platform dashboard",
        description = "User must have the MANAGEMENT_DASHBOARD[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Platform dashboard",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DashboardEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.READ) })
    public DashboardEntity getDashboard(final @PathParam("dashboardId") String dashboardId) {
        return dashboardService.findById(dashboardId);
    }

    @Path("{dashboardId}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a platform dashboard",
        description = "User must have the MANAGEMENT_DASHBOARD[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated dashboard",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DashboardEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.UPDATE) })
    public DashboardEntity updateDashboard(
        @PathParam("dashboardId") String dashboardId,
        @Valid @NotNull final UpdateDashboardEntity dashboard
    ) {
        dashboard.setId(dashboardId);
        return dashboardService.update(GraviteeContext.getExecutionContext(), dashboard);
    }

    @Path("{dashboardId}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete a platform dashboard",
        description = "User must have the MANAGEMENT_DASHBOARD[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Dashboard successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = RolePermissionAction.DELETE) })
    public void deleteDashboard(@PathParam("dashboardId") String dashboardId) {
        dashboardService.delete(GraviteeContext.getExecutionContext(), dashboardId);
    }
}
