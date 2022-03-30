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
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Metadata")
public class ApplicationMetadataResource extends AbstractResource {

    @Inject
    private ApplicationMetadataService metadataService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List metadata for an application",
        description = "User must have the APPLICATION_METADATA[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of metadata for an application",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApplicationMetadataEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public List<ApplicationMetadataEntity> getApplicationMetadatas() {
        return metadataService.findAllByApplication(application);
    }

    @GET
    @Path("{metadata}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "A metadata for an application and metadata id",
        description = "User must have the APPLICATION_METADATA[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "A metadata",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationMetadataEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Metadata not found")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public ApplicationMetadataEntity getApplicationMetadata(@PathParam("metadata") String metadata) {
        return metadataService.findByIdAndApplication(metadata, application);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Create an application metadata",
        description = "User must have the APPLICATION_METADATA[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Application metadata successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationMetadataEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.CREATE) })
    public Response createApplicationMetadata(@Valid @NotNull final NewApplicationMetadataEntity metadata) {
        // prevent creation of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        final ApplicationMetadataEntity applicationMetadataEntity = metadataService.create(GraviteeContext.getExecutionContext(), metadata);
        return Response.created(this.getLocationHeader(applicationMetadataEntity.getKey())).entity(applicationMetadataEntity).build();
    }

    @PUT
    @Path("{metadata}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update an application metadata",
        description = "User must have the APPLICATION_METADATA[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Updated application metadata",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApplicationMetadataEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationMetadata(
        @PathParam("metadata") String metadataPathParam,
        @Valid @NotNull final UpdateApplicationMetadataEntity metadata
    ) {
        // prevent update of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        return Response.ok(metadataService.update(GraviteeContext.getExecutionContext(), metadata)).build();
    }

    @DELETE
    @Path("{metadata}")
    @Operation(
        summary = "Delete a metadata",
        description = "User must have the APPLICATION_METADATA[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Metadata successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMetadata(@PathParam("metadata") String metadata) {
        metadataService.delete(GraviteeContext.getExecutionContext(), metadata, application);
        return Response.noContent().build();
    }
}
