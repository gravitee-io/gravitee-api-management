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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.*;

import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.service.CustomUserFieldService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Custom User Fields")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomUserFieldsResource extends AbstractResource {

    @Context
    private UriInfo uriInfo;

    @Inject
    private CustomUserFieldService fieldService;

    @GET
    @Operation(
        summary = "List All Custom User Fields",
        description = "User must have the CUSTOM_USER_FIELDS[READ] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Custom User Field deleted",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = CustomUserFieldEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response getCustomUserFields() {
        List<CustomUserFieldEntity> fields = fieldService.listAllFields(GraviteeContext.getExecutionContext());
        return Response.ok().entity(fields).build();
    }

    @POST
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = CREATE))
    @Operation(
        summary = "Create a Custom User Field",
        description = "User must have the CUSTOM_USER_FIELDS[CREATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "201",
        description = "Custom User Field Created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUserFieldEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response createCustomUserField(@Valid CustomUserFieldEntity newCustomUserFieldEntity) {
        CustomUserFieldEntity newField = fieldService.create(GraviteeContext.getExecutionContext(), newCustomUserFieldEntity);
        if (newField != null) {
            return Response.created(URI.create(uriInfo.getPath() + "/" + newField.getKey())).entity(newField).build();
        }

        return Response.serverError().build();
    }

    @PUT
    @Path("{key}")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = UPDATE))
    @Operation(
        summary = "Update a Custom User Field",
        description = "User must have the CUSTOM_USER_FIELDS[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Custom User Field updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomUserFieldEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response updateCustomUserField(@PathParam("key") String key, @Valid CustomUserFieldEntity toUpdateFieldEntity) {
        if (toUpdateFieldEntity == null || !key.toLowerCase().equals(toUpdateFieldEntity.getKey().toLowerCase())) {
            throw new CustomUserFieldException(key, "update");
        }

        CustomUserFieldEntity updatedField = fieldService.update(GraviteeContext.getExecutionContext(), toUpdateFieldEntity);
        if (updatedField != null) {
            return Response.ok().entity(updatedField).build();
        }

        return Response.serverError().build();
    }

    @DELETE
    @Path("{key}")
    @Permissions(@Permission(value = RolePermission.ORGANIZATION_CUSTOM_USER_FIELDS, acls = DELETE))
    @Operation(
        summary = "Delete a Custom User Field",
        description = "User must have the CUSTOM_USER_FIELDS[DELETE] permission to use this service"
    )
    @ApiResponse(responseCode = "204", description = "Custom User Field deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Response deleteCustomUserField(@PathParam("key") String key) {
        fieldService.delete(GraviteeContext.getExecutionContext(), key);
        return Response.noContent().build();
    }
}
