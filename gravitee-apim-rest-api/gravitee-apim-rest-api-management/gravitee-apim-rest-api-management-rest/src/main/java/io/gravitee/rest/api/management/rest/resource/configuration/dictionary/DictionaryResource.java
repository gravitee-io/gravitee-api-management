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
package io.gravitee.rest.api.management.rest.resource.configuration.dictionary;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.rest.resource.param.LifecycleAction;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.rest.api.model.configuration.dictionary.DictionaryType;
import io.gravitee.rest.api.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Configuration")
@Tag(name = "Dictionaries")
public class DictionaryResource extends AbstractResource {

    @Autowired
    private DictionaryService dictionaryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a dictionary", description = "User must have the DICTIONARY[READ] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "A dictionary",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DictionaryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.READ))
    public DictionaryEntity getDictionary(@PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);
        // remove provider informations for readonlyUsers
        boolean notReadOnly = hasPermission(
            RolePermission.ENVIRONMENT_DICTIONARY,
            RolePermissionAction.CREATE,
            RolePermissionAction.UPDATE,
            RolePermissionAction.DELETE
        );
        if (!notReadOnly) {
            dictionaryEntity.setProvider(null);
            dictionaryEntity.setTrigger(null);
        }
        return dictionaryEntity;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a dictionary", description = "User must have the DICTIONARY[UPDATE] permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Updated dictionary",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DictionaryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public DictionaryEntity updateDictionary(
        @PathParam("dictionary") String dictionary,
        @Parameter(name = "dictionary", required = true) @Valid @NotNull final UpdateDictionaryEntity updatedDictionary
    ) {
        return dictionaryService.update(GraviteeContext.getExecutionContext(), dictionary, updatedDictionary);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_deploy")
    @Operation(
        summary = "Deploy dictionary to API gateway",
        description = "User must have the DICTIONARY[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Dictionary successfully deployed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DictionaryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public Response deployDictionary(@PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.MANUAL) {
            dictionaryEntity = dictionaryService.deploy(GraviteeContext.getExecutionContext(), dictionary);
            return Response
                .ok(dictionaryEntity)
                .tag(Long.toString(dictionaryEntity.getUpdatedAt().getTime()))
                .lastModified(dictionaryEntity.getUpdatedAt())
                .build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("An automatic dictionary can not be deployed manually").build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_undeploy")
    @Operation(
        summary = "Undeploy dictionary to API gateway",
        description = "User must have the DICTIONARY[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Dictionary successfully undeployed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DictionaryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions(@Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public Response undeployDictionary(@PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.MANUAL) {
            dictionaryEntity = dictionaryService.undeploy(GraviteeContext.getExecutionContext(), dictionary);
            return Response
                .ok(dictionaryEntity)
                .tag(Long.toString(dictionaryEntity.getUpdatedAt().getTime()))
                .lastModified(dictionaryEntity.getUpdatedAt())
                .build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("An automatic dictionary can not be undeployed manually").build();
    }

    @DELETE
    @Operation(summary = "Delete a dictionary", description = "User must have the DICTIONARY[DELETE] permission to use this service")
    @ApiResponse(responseCode = "204", description = "Dictionary successfully deleted")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE) })
    public Response deleteDictionary(@PathParam("dictionary") String dictionary) {
        dictionaryService.delete(GraviteeContext.getExecutionContext(), dictionary);
        return Response.noContent().build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Manage the dictionary's lifecycle",
        description = "User must have the DICTIONARY[LIFECYCLE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Dictionary state updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = DictionaryEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DICTIONARY, acls = RolePermissionAction.UPDATE) })
    public Response doLifecycleAction(
        @Context HttpHeaders headers,
        @Parameter(required = true) @QueryParam("action") LifecycleAction action,
        @PathParam("dictionary") String dictionary
    ) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.DYNAMIC) {
            switch (action) {
                case START:
                    checkLifecycle(dictionaryEntity, action);
                    dictionaryEntity = dictionaryService.start(GraviteeContext.getExecutionContext(), dictionary);
                    break;
                case STOP:
                    checkLifecycle(dictionaryEntity, action);
                    dictionaryEntity = dictionaryService.stop(GraviteeContext.getExecutionContext(), dictionary);
                    break;
                default:
                    dictionaryEntity = null;
                    break;
            }

            return Response
                .ok(dictionaryEntity)
                .tag(Long.toString(dictionaryEntity.getUpdatedAt().getTime()))
                .lastModified(dictionaryEntity.getUpdatedAt())
                .build();
        }

        return Response.status(Response.Status.BAD_REQUEST).entity("A manual dictionary can not be started/stopped manually").build();
    }

    private void checkLifecycle(DictionaryEntity dictionary, LifecycleAction action) {
        switch (dictionary.getState()) {
            case STARTED:
                if (LifecycleAction.START.equals(action)) {
                    throw new BadRequestException("Dictionary is already started");
                }
                break;
            case STOPPED:
                if (LifecycleAction.STOP.equals(action)) {
                    throw new BadRequestException("Dictionary is already stopped");
                }
                break;
            default:
                break;
        }
    }
}
