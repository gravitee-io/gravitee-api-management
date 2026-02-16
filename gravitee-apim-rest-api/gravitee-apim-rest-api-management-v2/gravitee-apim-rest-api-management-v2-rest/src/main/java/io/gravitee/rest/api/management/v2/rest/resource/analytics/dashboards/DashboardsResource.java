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
import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.use_case.CreateDashboardUseCase;
import io.gravitee.apim.core.dashboard.use_case.ListDashboardsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.DashboardMapper;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.CreateDashboard;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.DashboardsResponse;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class DashboardsResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CreateDashboardUseCase createDashboardUseCase;

    @Inject
    private ListDashboardsUseCase listDashboardsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public Response listDashboards(@BeanParam @Valid PaginationParam paginationParam) {
        var executionContext = GraviteeContext.getExecutionContext();
        var output = listDashboardsUseCase.execute(new ListDashboardsUseCase.Input(executionContext.getOrganizationId()));
        List<Dashboard> allData = output.dashboards();
        List<Dashboard> paginationData = computePaginationData(allData, paginationParam);

        var response = new DashboardsResponse()
            .data(DashboardMapper.INSTANCE.mapList(paginationData))
            .pagination(PaginationInfo.computePaginationInfo(allData.size(), paginationData.size(), paginationParam))
            .links(computePaginationLinks(allData.size(), paginationParam));

        return Response.ok(response).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ORGANIZATION_DASHBOARD, acls = { RolePermissionAction.CREATE }) })
    public Response createDashboard(@Valid @NotNull CreateDashboard createDashboard) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        var auditInfo = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .actor(
                AuditActor.builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        var dashboard = DashboardMapper.INSTANCE.map(createDashboard);
        var output = createDashboardUseCase.execute(new CreateDashboardUseCase.Input(dashboard, auditInfo));

        return Response.created(getLocationHeader(output.dashboard().getId()))
            .entity(DashboardMapper.INSTANCE.map(output.dashboard()))
            .build();
    }

    @Path("{dashboardId}")
    public DashboardResource getDashboardResource() {
        return resourceContext.getResource(DashboardResource.class);
    }
}
