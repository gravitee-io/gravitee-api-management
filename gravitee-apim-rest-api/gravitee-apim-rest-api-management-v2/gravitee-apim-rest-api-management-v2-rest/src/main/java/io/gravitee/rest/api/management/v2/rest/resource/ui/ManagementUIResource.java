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
package io.gravitee.rest.api.management.v2.rest.resource.ui;

import io.gravitee.apim.core.console.use_case.GetConsoleCustomizationUseCase;
import io.gravitee.apim.core.installation.query_service.InstallationAccessQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ConsoleCustomizationMapper;
import io.gravitee.rest.api.management.v2.rest.model.ConsoleCustomization;
import io.gravitee.rest.api.model.bootstrap.ManagementUIBootstrapEntity;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Management UI Resource")
@Path("/ui")
@Slf4j
public class ManagementUIResource {

    private static final String PROPERTY_HTTP_API_MANAGEMENT_PROXY_PATH = "installation.api.proxyPath.management";
    private static final String PROPERTY_HTTP_API_MANAGEMENT_ENTRYPOINT = "http.api.management.entrypoint";

    @Autowired
    protected InstallationAccessQueryService installationAccessQueryService;

    @Autowired
    private Environment environment;

    @Autowired
    private GetConsoleCustomizationUseCase getConsoleCustomizationUseCase;

    @Path("/bootstrap")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the console bootstrap", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Management UI bootstrap information",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ManagementUIBootstrapEntity.class))
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

        String consoleApiUrl = installationAccessQueryService.getConsoleAPIUrl(organizationId);
        if (consoleApiUrl != null) {
            return Response.ok(ManagementUIBootstrapEntity.builder().organizationId(organizationId).baseURL(consoleApiUrl).build()).build();
        }

        ServerHttpRequest request = new ServletServerHttpRequest(httpServletRequest);
        UriComponents uriComponents = UriComponentsBuilder.fromHttpRequest(request)
            .replacePath(installationAccessQueryService.getConsoleApiPath())
            .replaceQuery(null)
            .build();

        return Response.ok(
            ManagementUIBootstrapEntity.builder().organizationId(organizationId).baseURL(uriComponents.toUriString()).build()
        ).build();
    }

    @Path("/customization")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ConsoleCustomization getConsoleCustomization() {
        return ConsoleCustomizationMapper.INSTANCE.map(getConsoleCustomizationUseCase.execute().consoleCustomization());
    }
}
