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
package io.gravitee.gamma.rest.resources;

import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.bootstrap.ManagementUIBootstrapEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponents;

@Path("/ui")
public class GammaUIResource {

    @Inject
    protected InstallationAccessQueryService installationAccessQueryService;

    @Path("/bootstrap")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the Gamma ControlPlane bootstrap", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Gamma ControlPlane bootstrap information",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = GammaBootstrap.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response bootstrap(
        @Context final HttpServletRequest httpServletRequest,
        @QueryParam("organizationId") final String enforceOrganizationId
    ) {
        String organizationId;
        if (enforceOrganizationId != null) {
            organizationId = enforceOrganizationId;
        } else {
            organizationId = GraviteeContext.getCurrentOrganization() != null
                ? GraviteeContext.getCurrentOrganization()
                : GraviteeContext.getDefaultOrganization();
        }
        ServerHttpRequest request = new ServletServerHttpRequest(httpServletRequest);

        String gammaApiUrl = installationAccessQueryService.getGammaAPIUrl(organizationId);
        if (gammaApiUrl == null) {
            gammaApiUrl = ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders())
                .replacePath(installationAccessQueryService.getGammaApiPath())
                .replaceQuery(null)
                .build()
                .toUriString();
        }

        String managementApiUrl = installationAccessQueryService.getConsoleAPIUrl(organizationId);
        if (managementApiUrl == null) {
            managementApiUrl = ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders())
                .replacePath(installationAccessQueryService.getConsoleApiPath())
                .replaceQuery(null)
                .build()
                .toUriString();
        }

        return Response.ok(new GammaBootstrap(gammaApiUrl, managementApiUrl, organizationId)).build();
    }

    public record GammaBootstrap(String gammaBaseURL, String managementBaseURL, String organizationId) {}
}
