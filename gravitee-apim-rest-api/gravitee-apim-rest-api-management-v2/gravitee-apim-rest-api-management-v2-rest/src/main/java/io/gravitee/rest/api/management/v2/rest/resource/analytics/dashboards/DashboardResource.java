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
package io.gravitee.rest.api.management.v2.rest.resource.analytics.dashboards;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.dashboard.use_case.DeleteDashboardUseCase;
import io.gravitee.apim.core.dashboard.use_case.GetDashboardUseCase;
import io.gravitee.apim.core.dashboard.use_case.UpdateDashboardUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.DashboardMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.UpdateDashboard;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class DashboardResource extends AbstractResource {

    @PathParam("dashboardId")
    String dashboardId;

    @Inject
    private GetDashboardUseCase getDashboardUseCase;

    @Inject
    private UpdateDashboardUseCase updateDashboardUseCase;

    @Inject
    private DeleteDashboardUseCase deleteDashboardUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public Response getDashboard() {
        var output = getDashboardUseCase.execute(new GetDashboardUseCase.Input(dashboardId));

        return Response.ok(DashboardMapper.INSTANCE.map(output.dashboard())).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_DASHBOARD, acls = { RolePermissionAction.UPDATE }) })
    public Response updateDashboard(@Valid @NotNull UpdateDashboard updateDashboard) {
        var auditInfo = getAuditInfo();

        var dashboard = DashboardMapper.INSTANCE.map(updateDashboard);

        var output = updateDashboardUseCase.execute(new UpdateDashboardUseCase.Input(dashboardId, dashboard, auditInfo));

        return Response.ok(DashboardMapper.INSTANCE.map(output.dashboard())).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_DASHBOARD, acls = { RolePermissionAction.DELETE }) })
    public Response deleteDashboard() {
        var auditInfo = getAuditInfo();

        deleteDashboardUseCase.execute(new DeleteDashboardUseCase.Input(dashboardId, auditInfo));

        return Response.noContent().build();
    }
}
