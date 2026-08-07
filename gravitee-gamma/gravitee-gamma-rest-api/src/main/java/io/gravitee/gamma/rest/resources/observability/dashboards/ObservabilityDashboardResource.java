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
package io.gravitee.gamma.rest.resources.observability.dashboards;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.DeleteObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.GetObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.UpdateObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.SaveDashboardRequestDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * Single saved-dashboard endpoints (GET / PUT / DELETE), reached via
 * {@link ObservabilityDashboardsResource}'s {@code {dashboardId}} locator.
 *
 * <p>Environment isolation: a dashboard id from another environment resolves to 404, not 403, on
 * every verb (see {@code DashboardRepository#findByIdAndEnvironmentId}), so cross-environment
 * existence cannot be probed.
 *
 * <p>On PUT the id comes from the path — a different id in the request body is ignored, like every
 * other server-owned field (see {@link SaveDashboardRequestDto}).
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ObservabilityDashboardResource {

    @Inject
    private GetObservabilityDashboardUseCase getDashboardUseCase;

    @Inject
    private UpdateObservabilityDashboardUseCase updateDashboardUseCase;

    @Inject
    private DeleteObservabilityDashboardUseCase deleteDashboardUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public DashboardDto get(@PathParam("dashboardId") String dashboardId) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getDashboardUseCase.execute(new GetObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId));
        return DashboardDto.from(output.dashboard());
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.UPDATE }) })
    public DashboardDto update(@PathParam("dashboardId") String dashboardId, SaveDashboardRequestDto request) {
        if (request == null) {
            throw new InvalidDashboardException("Request body is required");
        }
        var ctx = GraviteeContext.getExecutionContext();
        var output = updateDashboardUseCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId, request.toContent())
        );
        return DashboardDto.from(output.dashboard());
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("dashboardId") String dashboardId) {
        var ctx = GraviteeContext.getExecutionContext();
        deleteDashboardUseCase.execute(new DeleteObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId));
        return Response.noContent().build();
    }
}
