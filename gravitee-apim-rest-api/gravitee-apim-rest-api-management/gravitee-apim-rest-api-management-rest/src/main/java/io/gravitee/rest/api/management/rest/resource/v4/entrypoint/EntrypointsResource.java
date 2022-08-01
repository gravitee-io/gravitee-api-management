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
package io.gravitee.rest.api.management.rest.resource.v4.entrypoint;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ConnectorListItem;
import io.gravitee.rest.api.model.ConnectorPluginEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.entrypoint.EntrypointExpandEntity;
import io.gravitee.rest.api.model.v4.entrypoint.EntrypointPluginEntity;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.v4.EntrypointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
@Tag(name = "Plugins")
public class EntrypointsResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private EntrypointService entrypointService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List entrypoint plugins", description = "User must have the ENVIRONMENT_API[READ] permission to use this service")
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
    public Collection<EntrypointExpandEntity> getEntrypoint(@QueryParam("expand") List<String> expand) {
        Stream<EntrypointExpandEntity> stream = entrypointService.findAll().stream().map(this::convert);

        if (expand != null && !expand.isEmpty()) {
            for (String s : expand) {
                switch (s) {
                    case "schema":
                        stream =
                            stream.peek(
                                connectorListItem -> connectorListItem.setSchema(entrypointService.getSchema(connectorListItem.getId()))
                            );
                    case "icon":
                        stream =
                            stream.peek(
                                connectorListItem -> connectorListItem.setIcon(entrypointService.getIcon(connectorListItem.getId()))
                            );
                }
            }
        }

        return stream.sorted(Comparator.comparing(EntrypointExpandEntity::getName)).collect(Collectors.toList());
    }

    @Path("{connector}")
    public EntrypointResource getConnectorResource() {
        return resourceContext.getResource(EntrypointResource.class);
    }

    private EntrypointExpandEntity convert(EntrypointPluginEntity entrypointPluginEntity) {
        EntrypointExpandEntity entrypointExpandEntity = new EntrypointExpandEntity();

        entrypointExpandEntity.setId(entrypointPluginEntity.getId());
        entrypointExpandEntity.setName(entrypointPluginEntity.getName());
        entrypointExpandEntity.setDescription(entrypointPluginEntity.getDescription());
        entrypointExpandEntity.setVersion(entrypointPluginEntity.getVersion());
        entrypointExpandEntity.setSupportedApiType(entrypointPluginEntity.getSupportedApiType());
        entrypointExpandEntity.setSupportedModes(entrypointPluginEntity.getSupportedModes());

        return entrypointExpandEntity;
    }
}
