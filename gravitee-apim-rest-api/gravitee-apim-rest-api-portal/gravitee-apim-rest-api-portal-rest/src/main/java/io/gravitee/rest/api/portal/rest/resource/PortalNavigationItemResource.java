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

import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.use_case.GetPortalNavigationItemUseCase;
import io.gravitee.apim.core.portal_page.use_case.GetPortalPageContentByNavigationIdUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.portal.rest.mapper.PortalNavigationItemMapper;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class PortalNavigationItemResource extends AbstractResource {

    @Inject
    private GetPortalNavigationItemUseCase getPortalNavigationItemUseCase;

    @Inject
    private GetPortalPageContentByNavigationIdUseCase getPortalPageContentByNavigationIdUseCase;

    private static final PortalNavigationItemMapper portalNavigationItemMapper = PortalNavigationItemMapper.INSTANCE;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getPortalNavigationItemById(@PathParam("portalNavigationItemId") String portalNavigationItemId) {
        var executionContext = getExecutionContext();
        var result = getPortalNavigationItemUseCase.execute(
            new GetPortalNavigationItemUseCase.Input(
                PortalNavigationItemId.of(portalNavigationItemId),
                executionContext.getEnvironmentId(),
                PortalNavigationItemViewerContext.forPortal(isAuthenticated())
            )
        );

        return Response.ok(portalNavigationItemMapper.getBasePortalNavigationItem(result.portalNavigationItem())).build();
    }

    @GET
    @Path("/content")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getPortalNavigationItemContentById(@PathParam("portalNavigationItemId") String portalNavigationItemId) {
        var executionContext = getExecutionContext();
        var result = getPortalPageContentByNavigationIdUseCase.execute(
            new GetPortalPageContentByNavigationIdUseCase.Input(
                portalNavigationItemId,
                executionContext.getEnvironmentId(),
                PortalNavigationItemViewerContext.forPortal(isAuthenticated())
            )
        );

        return Response.ok(portalNavigationItemMapper.map(result.portalPageContent())).build();
    }
}
