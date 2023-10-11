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
package io.gravitee.rest.api.portal.rest.resource.bootstrap;

import io.gravitee.apim.core.access_point.query_service.AccessPointQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.bootstrap.PortalUIBootstrapEntity;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Bootstrap Portal UI")
@Path("/ui/bootstrap")
@Slf4j
public class PortalUIBootstrapResource {

    @Autowired
    protected AccessPointQueryService accessPointService;

    @Autowired
    protected EnvironmentService environmentService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the portal bootstrap", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Portal UI bootstrap information",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalUIBootstrapEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response bootstrap(@Context final HttpServletRequest httpServletRequest) {
        URI contextPath = URI.create(httpServletRequest.getContextPath());

        String portalApiUrl = accessPointService.getPortalApiUrl(GraviteeContext.getCurrentEnvironment());
        if (portalApiUrl != null) {
            URI fullPortalUrl = URI.create(portalApiUrl).resolve(contextPath);
            return Response
                .ok(
                    PortalUIBootstrapEntity
                        .builder()
                        .organizationId(GraviteeContext.getCurrentOrganization())
                        .environmentId(GraviteeContext.getCurrentEnvironment())
                        .baseURL(fullPortalUrl.toString())
                        .build()
                )
                .build();
        }

        ServerHttpRequest request = new ServletServerHttpRequest(httpServletRequest);
        UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request).replacePath(contextPath.toString()).build();
        return Response
            .ok(
                PortalUIBootstrapEntity
                    .builder()
                    .organizationId(GraviteeContext.getDefaultOrganization())
                    .environmentId(GraviteeContext.getDefaultEnvironment())
                    .baseURL(uriComponents.toUriString())
                    .build()
            )
            .build();
    }
}
