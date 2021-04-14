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
import io.swagger.annotations.*;
import java.net.URI;
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
@Api(tags = { "Application Metadata" })
public class ApplicationMetadataResource extends AbstractResource {

    @Inject
    private ApplicationMetadataService metadataService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "List metadata for an application",
        notes = "User must have the APPLICATION_METADATA[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List of metadata for an application",
                response = ApplicationMetadataEntity.class,
                responseContainer = "List"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public List<ApplicationMetadataEntity> getApplicationMetadatas() {
        return metadataService.findAllByApplication(application);
    }

    @GET
    @Path("{metadata}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "A metadata for an application and metadata id",
        notes = "User must have the APPLICATION_METADATA[READ] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "A metadata", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 404, message = "Metadata not found"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ) })
    public ApplicationMetadataEntity getApplicationMetadata(@PathParam("metadata") String metadata) {
        return metadataService.findByIdAndApplication(metadata, application);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Create an application metadata",
        notes = "User must have the APPLICATION_METADATA[CREATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Application metadata successfully created", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.CREATE) })
    public Response createApplicationMetadata(@Valid @NotNull final NewApplicationMetadataEntity metadata) {
        // prevent creation of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        final ApplicationMetadataEntity applicationMetadataEntity = metadataService.create(metadata);
        return Response.created(this.getLocationHeader(applicationMetadataEntity.getKey())).entity(applicationMetadataEntity).build();
    }

    @PUT
    @Path("{metadata}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Update an application metadata",
        notes = "User must have the APPLICATION_METADATA[UPDATE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Updated application metadata", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationMetadata(
        @PathParam("metadata") String metadataPathParam,
        @Valid @NotNull final UpdateApplicationMetadataEntity metadata
    ) {
        // prevent update of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        return Response.ok(metadataService.update(metadata)).build();
    }

    @DELETE
    @Path("{metadata}")
    @ApiOperation(value = "Delete a metadata", notes = "User must have the APPLICATION_METADATA[DELETE] permission to use this service")
    @ApiResponses(
        { @ApiResponse(code = 204, message = "Metadata successfully deleted"), @ApiResponse(code = 500, message = "Internal server error") }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMetadata(@PathParam("metadata") String metadata) {
        metadataService.delete(metadata, application);
        return Response.noContent().build();
    }
}
