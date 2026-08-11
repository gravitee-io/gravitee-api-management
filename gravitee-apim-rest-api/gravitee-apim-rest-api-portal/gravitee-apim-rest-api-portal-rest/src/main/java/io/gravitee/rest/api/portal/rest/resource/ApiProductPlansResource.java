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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.plan.use_case.GetPortalApiProductPlansUseCase;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.ApiProductPlanMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

public class ApiProductPlansResource extends AbstractResource {

    @Inject
    private GetPortalApiProductPlansUseCase getPortalApiProductPlansUseCase;

    private static final ApiProductPlanMapper apiProductPlanMapper = ApiProductPlanMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiProductPlans(@PathParam("apiProductId") UUID apiProductId, @BeanParam PaginationParam paginationParam) {
        var executionContext = getExecutionContext();
        var output = getPortalApiProductPlansUseCase.execute(
            new GetPortalApiProductPlansUseCase.Input(executionContext.getEnvironmentId(), apiProductId.toString(), viewerContext())
        );
        var plans = output.plans().stream().map(apiProductPlanMapper::map).toList();

        return createListResponse(executionContext, plans, paginationParam);
    }

    private PortalNavigationItemViewerContext viewerContext() {
        return PortalNavigationItemViewerContext.forPortal(getAuthenticatedUserOrNull());
    }
}
