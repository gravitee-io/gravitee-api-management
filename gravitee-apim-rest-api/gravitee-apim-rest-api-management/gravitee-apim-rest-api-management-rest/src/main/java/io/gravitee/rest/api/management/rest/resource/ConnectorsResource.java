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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ConnectorListItem;
import io.gravitee.rest.api.model.ConnectorPluginEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ConnectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Defines the REST resources to manage connectors.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Plugins")
public class ConnectorsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ConnectorService connectorService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List connector plugins", description = "User must have the ENVIRONMENT_API[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of connectors",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ConnectorListItem.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_API, acls = RolePermissionAction.READ) })
    public Collection<ConnectorListItem> getConnectors(@QueryParam("expand") List<String> expand) {
        expand = expand == null ? Collections.emptyList() : expand;
        boolean includeSchema = expand.contains("schema");
        boolean includeIcon = expand.contains("icon");
        Stream<ConnectorListItem> stream = connectorService
            .findAll()
            .stream()
            .map(connector -> convert(connector, includeSchema, includeIcon));

        return stream.sorted(Comparator.comparing(ConnectorListItem::getName)).collect(Collectors.toList());
    }

    @Path("{connector}")
    public ConnectorResource getConnectorResource() {
        return resourceContext.getResource(ConnectorResource.class);
    }

    private ConnectorListItem convert(ConnectorPluginEntity connector, boolean includeSchema, boolean includeIcon) {
        ConnectorListItem item = new ConnectorListItem();

        item.setId(connector.getId());
        item.setName(connector.getName());
        item.setDescription(connector.getDescription());
        item.setVersion(connector.getVersion());
        item.setSupportedTypes(connector.getSupportedTypes());

        if (includeSchema) {
            item.setSchema(connectorService.getSchema(connector.getId()));
        }

        if (includeIcon) {
            item.setIcon(connectorService.getIcon(connector.getId()));
        }

        return item;
    }
}
