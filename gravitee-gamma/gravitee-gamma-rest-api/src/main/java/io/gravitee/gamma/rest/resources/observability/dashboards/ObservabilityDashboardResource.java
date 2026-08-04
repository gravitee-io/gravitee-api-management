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
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.GetObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

/**
 * Single saved-dashboard endpoint, reached via {@link ObservabilityDashboardsResource}'s
 * {@code {dashboardId}} locator.
 *
 * <p>Environment isolation: a dashboard id from another environment resolves to 404, not 403 (see
 * {@code GetObservabilityDashboardUseCase} / {@code DashboardRepository#findByIdAndEnvironmentId}),
 * so cross-environment existence cannot be probed.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ObservabilityDashboardResource {

    @Inject
    private GetObservabilityDashboardUseCase getDashboardUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public DashboardDto get(@PathParam("dashboardId") String dashboardId) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getDashboardUseCase.execute(new GetObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId));
        return DashboardDto.from(output.dashboard());
    }
}
