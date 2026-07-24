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

import io.gravitee.apim.core.api_product.use_case.GetPortalApiProductUseCase;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.ApiProductMapper;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.UUID;

public class ApiProductResource extends AbstractResource {

    private static final ApiProductMapper apiProductMapper = ApiProductMapper.INSTANCE;

    @Inject
    private GetPortalApiProductUseCase getPortalApiProductUseCase;

    @GET
    @Path("{apiProductId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApiProductByApiProductId(@PathParam("apiProductId") UUID apiProductId) {
        var executionContext = getExecutionContext();
        var output = getPortalApiProductUseCase.execute(
            new GetPortalApiProductUseCase.Input(
                executionContext.getEnvironmentId(),
                apiProductId.toString(),
                PortalNavigationItemViewerContext.forPortal(getAuthenticatedUserOrNull())
            )
        );

        return Response.ok(apiProductMapper.map(output.apiProduct())).build();
    }
}
