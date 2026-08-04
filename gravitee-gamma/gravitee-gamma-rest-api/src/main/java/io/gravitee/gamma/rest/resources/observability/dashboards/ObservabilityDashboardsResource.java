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
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.ListObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.List;

/**
 * Saved-dashboard endpoints, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/dashboards} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /?page=&perPage=} — dashboards saved in the context environment, paginated
 *       (1-based, {@code perPage} capped server-side at 100). See {@link ListObservabilityDashboardUseCase}
 *       for why pagination is applied by slicing rather than a native repository query.</li>
 *   <li>{@code GET /{dashboardId}} — see {@link ObservabilityDashboardResource}.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Declarative {@code ENVIRONMENT_DASHBOARD:READ} — a CRUD resource, unlike the deliberately lax
 * {@code ENVIRONMENT_DASHBOARD:READ || ENVIRONMENT_API:READ} check used by {@code LogsResource} /
 * {@code ObservabilityFiltersResource} for metadata discovery.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ObservabilityDashboardsResource {

    @Inject
    private ListObservabilityDashboardUseCase listDashboardUseCase;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<DashboardDto> list(@QueryParam("page") Integer page, @QueryParam("perPage") Integer perPage) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = listDashboardUseCase.execute(new ListObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), page, perPage));
        List<DashboardDto> data = output.dashboards().stream().map(DashboardDto::from).toList();
        return PaginatedResponseDto.of(data, output.totalCount(), output.page(), output.perPage());
    }

    @Path("/{dashboardId}")
    public ObservabilityDashboardResource getDashboardResource() {
        return resourceContext.getResource(ObservabilityDashboardResource.class);
    }
}
