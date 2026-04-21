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

import io.gravitee.apim.core.dashboard.model.Dashboard;
import io.gravitee.apim.core.dashboard.use_case.ListDashboardsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalAnalyticsDashboardMapper;
import io.gravitee.rest.api.portal.rest.model.AnalyticsDashboard;
import io.gravitee.rest.api.portal.rest.resource.AbstractResource;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * Story 2 (APIM-12689): {@code GET /portal/environments/{envId}/analytics/dashboards} — paginated list using {@link ListDashboardsUseCase},
 * gated by {@link io.gravitee.rest.api.model.parameters.Key#PORTAL_NEXT_ANALYTICS_ENABLED}.
 *
 * @author GraviteeSource Team
 */
public class PortalAnalyticsDashboardsResource extends AbstractResource<AnalyticsDashboard, Dashboard> {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ListDashboardsUseCase listDashboardsUseCase;

    @Inject
    private ParameterService parameterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listDashboards(@BeanParam PaginationParam paginationParam) {
        PortalAnalyticsGate.requireAnalyticsEnabled(parameterService);

        var executionContext = GraviteeContext.getExecutionContext();
        var dashboards = listDashboardsUseCase.execute(new ListDashboardsUseCase.Input(executionContext.getEnvironmentId())).dashboards();

        if (!paginationParam.hasPagination()) {
            return createListResponse(executionContext, dashboards, PaginationParam.builder().page(1).size(dashboards.size()).build());
        }

        return createListResponse(executionContext, dashboards, paginationParam);
    }

    @Override
    protected List<AnalyticsDashboard> transformPageContent(ExecutionContext executionContext, List<Dashboard> pageContent) {
        if (pageContent.isEmpty()) {
            return Collections.emptyList();
        }

        return PortalAnalyticsDashboardMapper.INSTANCE.toDto(pageContent);
    }

    @Path("{dashboardId}")
    public PortalAnalyticsDashboardResource getDashboardResource() {
        return resourceContext.getResource(PortalAnalyticsDashboardResource.class);
    }
}
