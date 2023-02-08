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
package io.gravitee.rest.api.portal.rest.resource.v4.entrypoint;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ConnectorListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.connector.ConnectorExpandPluginEntity;
import io.gravitee.rest.api.portal.rest.resource.v4.connector.AbstractConnectorsResource;
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.v4.EntrypointConnectorPluginService;
import io.swagger.annotations.ApiParam;
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
 * Defines the REST resources to manage entrypoints.
 *
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "🧪 V4 - Entrypoints")
public class EntrypointsResource extends AbstractConnectorsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EntrypointConnectorPluginService entrypointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "🧪 List entrypoint plugins",
        description = "⚠️ This resource is in alpha version. This implies that it is likely to be modified or even removed in future versions. ⚠️. <br><br>User must have the ENVIRONMENT_API[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of entrypoints",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ConnectorListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ConnectorExpandPluginEntity> getEntrypoints(
        @QueryParam("expand") @ApiParam(
            name = "expand",
            allowableValues = "schema, icon, subscriptionSchema",
            allowMultiple = true
        ) List<String> expands
    ) {
        final Collection<ConnectorExpandPluginEntity> connectors = super.expand(entrypointService.findAll(), expands);
        if (expands != null && expands.contains("subscriptionSchema")) {
            connectors.forEach(connector -> connector.setSubscriptionSchema(entrypointService.getSubscriptionSchema(connector.getId())));
        }
        return connectors;
    }

    @Override
    protected String getSchema(final String connectorId) {
        return entrypointService.getSchema(connectorId);
    }

    @Override
    protected String getIcon(final String connectorId) {
        return entrypointService.getIcon(connectorId);
    }
}
