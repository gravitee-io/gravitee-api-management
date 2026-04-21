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
package io.gravitee.rest.api.portal.rest.resource.analytics;

import io.gravitee.apim.core.dashboard.exception.DashboardNotFoundException;
import io.gravitee.apim.core.dashboard.use_case.GetDashboardUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalAnalyticsDashboardMapper;
import io.gravitee.rest.api.portal.rest.resource.AbstractResource;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Objects;

/**
 * {@code GET /portal/environments/{envId}/analytics/dashboards/{dashboardId}} using
 * {@link GetDashboardUseCase}. The analytics-enabled gate is applied upstream by {@link PortalAnalyticsResource}.
 *
 * <p>Adds an explicit environment boundary check on top of the shared use case: the core use case looks up by id
 * only, so without this guard a dashboard belonging to another environment could leak if its id were known. The
 * mismatch is reported as {@code 404} (not {@code 403}) to avoid disclosing that the id exists elsewhere.</p>
 *
 * @author GraviteeSource Team
 */
public class PortalAnalyticsDashboardResource extends AbstractResource {

    @Inject
    private GetDashboardUseCase getDashboardUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getDashboard(@PathParam("dashboardId") String dashboardId) {
        var executionContext = GraviteeContext.getExecutionContext();
        var dashboard = getDashboardUseCase.execute(new GetDashboardUseCase.Input(dashboardId)).dashboard();

        if (!Objects.equals(dashboard.getEnvironmentId(), executionContext.getEnvironmentId())) {
            throw new DashboardNotFoundException(dashboardId);
        }

        return Response.ok(PortalAnalyticsDashboardMapper.INSTANCE.toDto(dashboard)).build();
    }
}
