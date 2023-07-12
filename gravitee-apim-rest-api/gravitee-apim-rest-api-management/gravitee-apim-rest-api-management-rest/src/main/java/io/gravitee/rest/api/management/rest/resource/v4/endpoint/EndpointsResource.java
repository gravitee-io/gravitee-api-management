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
package io.gravitee.rest.api.management.rest.resource.v4.endpoint;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.v4.connector.AbstractConnectorsResource;
import io.gravitee.rest.api.management.rest.resource.v4.entrypoint.EntrypointResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ConnectorListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;

/**
 * Defines the REST resources to manage endpoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "🧪 V4 - Endpoints")
public class EndpointsResource extends AbstractConnectorsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EndpointConnectorPluginService endpointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List endpoint plugins", description = "User must have the ENVIRONMENT_API[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of endpoints",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ConnectorListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ConnectorExpandPluginEntity> getEndpoints(@QueryParam("expand") List<String> expands) {
        return super.expand(endpointService.findAll(), expands);
    }

    @Path("{entrypoint}")
    public EntrypointResource getEntrypointResource() {
        return resourceContext.getResource(EntrypointResource.class);
    }

    @Override
    protected String getSchema(final String connectorId) {
        return endpointService.getSchema(connectorId);
    }

    @Override
    protected String getIcon(final String connectorId) {
        return endpointService.getIcon(connectorId);
    }
}
