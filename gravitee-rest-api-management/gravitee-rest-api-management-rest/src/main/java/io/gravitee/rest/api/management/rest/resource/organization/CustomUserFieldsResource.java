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
package io.gravitee.rest.api.management.rest.resource.organization;

import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Custom User Fields"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomUserFieldsResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;

    @Inject
    private CustomUserFieldService fieldService;

    @GET
    @ApiOperation(
            value = "List All Custom User Fields",
            notes = "User must have the CUSTOM_USER_FIELDS[READ] permission to use this service"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Custom User Field deleted", responseContainer = "List" ,response = CustomUserFieldEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response listAll() {

        List<CustomUserFieldEntity> fields = fieldService.listAllFields();
        return Response.ok().entity(fields).build();
    }

    @POST
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = CREATE))
    @ApiOperation(
            value = "Create a Custom User Field",
            notes = "User must have the CUSTOM_USER_FIELDS[CREATE] permission to use this service"
    )
    @ApiResponses({
            @ApiResponse(code = 201, message = "Custom User Field Created", response = CustomUserFieldEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response createField(@Valid CustomUserFieldEntity newCustomUserFieldEntity) {
        CustomUserFieldEntity newField = fieldService.create(newCustomUserFieldEntity);
        if (newField != null) {
            return Response
                    .created(URI.create(uriInfo.getPath() + "/" + newField.getKey()))
                    .entity(newField)
                    .build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("{key}")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = UPDATE))
    @ApiOperation(
            value = "Update a Custom User Field",
            notes = "User must have the CUSTOM_USER_FIELDS[UPDATE] permission to use this service"
    )
    @ApiResponses({
            @ApiResponse(code = 200, message = "Custom User Field updated", response = CustomUserFieldEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response updateField(@PathParam ("key")String key,
                               @Valid CustomUserFieldEntity toUpdateFieldEntity) {

        if (toUpdateFieldEntity == null || !key.toLowerCase().equals(toUpdateFieldEntity.getKey().toLowerCase())) {
            throw new CustomUserFieldException(key, "update");
        }

        CustomUserFieldEntity updatedField = fieldService.update(toUpdateFieldEntity);
        if (updatedField != null) {
            return Response
                    .ok()
                    .entity(updatedField)
                    .build();
        }

        return Response.serverError().build();
    }

    @DELETE
    @Path("{key}")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = DELETE))
    @ApiOperation(
            value = "Delete a Custom User Field",
            notes = "User must have the CUSTOM_USER_FIELDS[DELETE] permission to use this service"
    )
    @ApiResponses({
            @ApiResponse(code = 204, message = "Custom User Field deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response deleteField(@PathParam ("key")String key) {

        fieldService.delete(key);
        return Response.noContent().build();
    }
}
