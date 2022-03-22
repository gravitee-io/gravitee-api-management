/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.platform.plugin.PlatformPluginEntity;
import io.gravitee.rest.api.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class ConnectorResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ConnectorService connectorService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a connector", description = "User must have the ENVIRONMENT_API[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Connector plugin",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PlatformPluginEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Connector not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public PlatformPluginEntity getConnector(@PathParam("connector") String connector) {
        return connectorService.findById(connector);
    }

    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get a connector's schema",
        description = "User must have the ENVIRONMENT_API[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connector schema",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = String.class))
    )
    @ApiResponse(responseCode = "404", description = "Connector not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public String getConnectorSchema(@PathParam("connector") String connector) {
        // Check that the connector exists
        connectorService.findById(connector);

        return connectorService.getSchema(connector);
    }

    @GET
    @Path("documentation")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
        summary = "Get a connector's documentation",
        description = "User must have the ENVIRONMENT_API[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Connector documentation",
        content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class))
    )
    @ApiResponse(responseCode = "404", description = "Connector not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public String getConnectorDoc(@PathParam("connector") String connector) {
        // Check that the connector exists
        connectorService.findById(connector);

        return connectorService.getDocumentation(connector);
    }
}
