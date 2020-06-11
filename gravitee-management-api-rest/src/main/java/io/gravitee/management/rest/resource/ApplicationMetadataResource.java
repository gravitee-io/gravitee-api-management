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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.ApplicationMetadataEntity;
import io.gravitee.management.model.NewApplicationMetadataEntity;
import io.gravitee.management.model.UpdateApplicationMetadataEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApplicationMetadataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application", "Metadata"})
public class ApplicationMetadataResource extends AbstractResource {

    @Inject
    private ApplicationMetadataService metadataService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List metadata for the given APPLICATION",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of metadata", response = ApplicationMetadataEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ)
    })
    public List<ApplicationMetadataEntity> listApplicationMetadatas(
            @PathParam("application") String application) {
        return metadataService.findAllByApplication(application);
    }

    @GET
    @Path("{metadata}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "A metadata for the given APPLICATION and metadata id",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A metadata", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 404, message = "Metadata not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.READ)
    })
    public ApplicationMetadataEntity getApplicationMetadata(@PathParam("application") String application, @PathParam("metadata") String metadata) {
        return metadataService.findByIdAndApplication(metadata, application);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an APPLICATION metadata",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new APPLICATION metadata", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.CREATE)
    })
    public Response create(@PathParam("application") String application, @Valid @NotNull final NewApplicationMetadataEntity metadata) {
        // prevent creation of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        final ApplicationMetadataEntity applicationMetadataEntity = metadataService.create(metadata);
        return Response
                .created(URI.create("/applications/" + application + "/metadata/" + applicationMetadataEntity.getKey()))
                .entity(applicationMetadataEntity)
                .build();
    }

    @PUT
    @Path("{metadata}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an APPLICATION metadata",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "APPLICATION metadata", response = ApplicationMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.UPDATE)
    })
    public Response update(@PathParam("application") String application,
                           @PathParam("metadata") String metadataPathParam,
                           @Valid @NotNull final UpdateApplicationMetadataEntity metadata) {
        // prevent update of a metadata on an another APPLICATION
        metadata.setApplicationId(application);

        return Response.ok(metadataService.update(metadata)).build();
    }

    @DELETE
    @Path("{metadata}")
    @ApiOperation(value = "Delete a metadata",
            notes = "User must have the MANAGE_APPLICATION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Metadata successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_METADATA, acls = RolePermissionAction.DELETE)
    })
    public Response delete(@PathParam("application") String application, @PathParam("metadata") String metadata) {
        metadataService.delete(metadata, application);
        return Response.noContent().build();
    }
}
