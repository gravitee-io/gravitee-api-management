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
package io.gravitee.rest.api.management.rest.resource.portal;

import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * API resource to expose portal pages along with their view details, backed by PortalPageQueryService.
 */
@Tag(name = "Portal Pages View")
public class PortalPagesViewResource extends AbstractResource {

    @Inject
    private PortalPageQueryService portalPageQueryService;

    public static class QueryParams {

        @Parameter(description = "Portal view context to filter pages", required = true)
        @QueryParam("context")
        public String context;

        @Parameter(description = "Whether to expand pages (include content). Defaults to false for lightweight fetch.")
        @QueryParam("expand")
        public Boolean expand;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List portal pages with view details",
        description = "Returns portal pages for the current environment filtered by the provided view context. " +
        "Use expand=true to include full page content; default is false (lightweight)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of portal pages with view details",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = PortalPageWithViewDetails.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DOCUMENTATION, acls = RolePermissionAction.READ) })
    public Response getPortalPagesWithViewDetails(@BeanParam QueryParams params) {
        PortalViewContext viewContext = PortalViewContext.valueOf(params.context);
        boolean expand = params.expand != null && params.expand;
        List<PortalPageWithViewDetails> result = portalPageQueryService.findByEnvironmentIdAndContext(
            GraviteeContext.getCurrentEnvironment(),
            viewContext,
            expand
        );
        return Response.ok(result).build();
    }
}
