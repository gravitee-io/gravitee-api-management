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
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.MetadataEntity;
import io.gravitee.rest.api.model.NewMetadataEntity;
import io.gravitee.rest.api.model.UpdateMetadataEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Metadata")
public class MetadataResource extends AbstractResource {

    @Inject
    private MetadataService metadataService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve the list of platform metadata",
        description = "User must have the PORTAL_METADATA[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of environment metadata",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MetadataEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = RolePermissionAction.READ) })
    public List<MetadataEntity> getMetadatas() {
        return metadataService.findByReferenceTypeAndReferenceId(
            MetadataReferenceType.ENVIRONMENT,
            GraviteeContext.getExecutionContext().getEnvironmentId()
        );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create a platform metadata",
        description = "User must have the PORTAL_METADATA[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Metadata successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MetadataEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = RolePermissionAction.CREATE) })
    public MetadataEntity createMetadata(@Valid @NotNull final NewMetadataEntity metadata) {
        return metadataService.create(GraviteeContext.getExecutionContext(), metadata);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a platform metadata",
        description = "User must have the PORTAL_METADATA[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated metadata",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = MetadataEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = RolePermissionAction.UPDATE) })
    public MetadataEntity updateMetadata(@Valid @NotNull final UpdateMetadataEntity metadata) {
        return metadataService.update(GraviteeContext.getExecutionContext(), metadata);
    }

    @Path("{metadata}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Delete a platform metadata",
        description = "User must have the PORTAL_METADATA[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Metadata successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_METADATA, acls = RolePermissionAction.DELETE) })
    public void deleteMetadata(@PathParam("metadata") String metadata) {
        metadataService.delete(GraviteeContext.getExecutionContext(), metadata);
    }
}
