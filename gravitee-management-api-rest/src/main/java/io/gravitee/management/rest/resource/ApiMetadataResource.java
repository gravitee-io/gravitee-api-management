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
import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiMetadataService;
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
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API Metadata"})
public class ApiMetadataResource extends AbstractResource {

    @Inject
    private ApiMetadataService metadataService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List metadata for the given API",
            notes = "User must have the API_METADATA[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of metadata", response = ApiMetadataEntity.class, responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_METADATA, acls = RolePermissionAction.READ)
    })
    public List<ApiMetadataEntity> listApiMetadatas(
            @PathParam("api") String api) {
        return metadataService.findAllByApi(api);
    }

    @GET
    @Path("{metadata}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "A metadata for the given API and metadata id",
            notes = "User must have the API_METADATA[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A metadata", response = ApiMetadataEntity.class),
            @ApiResponse(code = 404, message = "Metadata not found"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_METADATA, acls = RolePermissionAction.READ)
    })
    public ApiMetadataEntity getApiMetadata(@PathParam("api") String api, @PathParam("metadata") String metadata) {
        return metadataService.findByIdAndApi(metadata, api);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create an API metadata",
            notes = "User must have the API_METADATA[CREATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new API metadata", response = ApiMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_METADATA, acls = RolePermissionAction.CREATE)
    })
    public Response create(@PathParam("api") String api, @Valid @NotNull final NewApiMetadataEntity metadata) {
        // prevent creation of a metadata on an another API
        metadata.setApiId(api);

        final ApiMetadataEntity apiMetadataEntity = metadataService.create(metadata);
        return Response
                .created(URI.create("/apis/" + api + "/metadata/" + apiMetadataEntity.getKey()))
                .entity(apiMetadataEntity)
                .build();
    }

    @PUT
    @Path("{metadata}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an API metadata",
            notes = "User must have the API_METADATA[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "API metadata", response = ApiMetadataEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_METADATA, acls = RolePermissionAction.UPDATE)
    })
    public Response update(@PathParam("api") String api,
                           @PathParam("metadata") String metadataPathParam,
                           @Valid @NotNull final UpdateApiMetadataEntity metadata) {
        // prevent update of a metadata on an another API
        metadata.setApiId(api);

        return Response.ok(metadataService.update(metadata)).build();
    }

    @DELETE
    @Path("{metadata}")
    @ApiOperation(value = "Delete a metadata",
            notes = "User must have the API_METADATA[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Metadata successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_METADATA, acls = RolePermissionAction.DELETE)
    })
    public Response delete(@PathParam("api") String api, @PathParam("metadata") String metadata) {
        metadataService.delete(metadata, api);
        return Response.noContent().build();
    }
}
