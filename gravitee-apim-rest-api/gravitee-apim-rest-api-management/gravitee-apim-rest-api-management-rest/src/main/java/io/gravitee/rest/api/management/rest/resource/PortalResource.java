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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.DELETE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.portal.PortalApisResource;
import io.gravitee.rest.api.management.rest.resource.portal.PortalPagesViewResource;
import io.gravitee.rest.api.management.rest.resource.portal.SocialIdentityProvidersResource;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.settings.PortalConfigEntity;
import io.gravitee.rest.api.model.settings.PortalSettingsEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * Defines the REST resources to manage Portal.
 *
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Portal")
public class PortalResource {

    @Inject
    private ConfigService configService;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the portal configuration", description = "Every users can use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Portal configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalConfigEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public PortalConfigEntity getPortalConfig() {
        return configService.getPortalConfig(GraviteeContext.getExecutionContext());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Save the portal configuration")
    @ApiResponse(
        responseCode = "200",
        description = "Updated portal configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PortalSettingsEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_SETTINGS, acls = { CREATE, UPDATE, DELETE }) })
    @Deprecated
    public Response savePortalConfig(@Parameter(name = "config", required = true) @NotNull PortalSettingsEntity portalSettingsEntity) {
        configService.save(GraviteeContext.getExecutionContext(), portalSettingsEntity);
        return Response.ok().entity(portalSettingsEntity).build();
    }

    @Path("pages")
    public PortalPagesResource getPortalPagesResource() {
        return resourceContext.getResource(PortalPagesResource.class);
    }

    @Path("apis")
    public PortalApisResource getPortalApisResource() {
        return resourceContext.getResource(PortalApisResource.class);
    }

    @Path("media")
    public PortalMediaResource getPortalMediaResource() {
        return resourceContext.getResource(PortalMediaResource.class);
    }

    @Path("pages-view")
    public PortalPagesViewResource getPortalPagesViewResource() {
        return resourceContext.getResource(PortalPagesViewResource.class);
    }

    @Deprecated
    @Path("identities")
    public SocialIdentityProvidersResource getSocialIdentityProvidersResource() {
        return resourceContext.getResource(SocialIdentityProvidersResource.class);
    }

    @Path("redirect")
    public PortalRedirectResource getPortalRedirectResource() {
        return resourceContext.getResource(PortalRedirectResource.class);
    }
}
