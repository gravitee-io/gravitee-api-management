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
package io.gravitee.management.rest.resource.configuration.dictionary;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.configuration.dictionary.DictionaryEntity;
import io.gravitee.management.model.configuration.dictionary.DictionaryType;
import io.gravitee.management.model.configuration.dictionary.UpdateDictionaryEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.resource.AbstractResource;
import io.gravitee.management.rest.resource.param.LifecycleActionParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.configuration.dictionary.DictionaryService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Configuration", "Dictionaries"})
public class DictionaryResource extends AbstractResource {

    @Autowired
    private DictionaryService dictionaryService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a dictionary",
            notes = "User must have the DICTIONARY[READ] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "A dictionary"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions(@Permission(value = RolePermission.MANAGEMENT_DICTIONARY, acls = RolePermissionAction.READ))
    public DictionaryEntity getDictionary(
            @PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);
        // remove provider informations for readonlyUsers
        boolean notReadOnly = hasPermission(RolePermission.MANAGEMENT_DICTIONARY, RolePermissionAction.CREATE, RolePermissionAction.UPDATE, RolePermissionAction.DELETE);
        if (!notReadOnly) {
            dictionaryEntity.setProvider(null);
            dictionaryEntity.setTrigger(null);
        }
        return dictionaryEntity;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a dictionary",
            notes = "User must have the DICTIONARY[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Updated dictionary", response = DictionaryEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions(@Permission(value = RolePermission.MANAGEMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public DictionaryEntity updateDictionary(
            @PathParam("dictionary") String dictionary,
            @ApiParam(name = "dictionary", required = true) @Valid @NotNull final UpdateDictionaryEntity updatedDictionary) {
        return dictionaryService.update(dictionary, updatedDictionary);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_deploy")
    @ApiOperation(
            value = "Deploy dictionary to API gateway",
            notes = "User must have the DICTIONARY[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Dictionary successfully deployed", response = DictionaryEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions(@Permission(value = RolePermission.MANAGEMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public Response deployDictionary(@PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.MANUAL) {
            dictionaryEntity = dictionaryService.deploy(dictionary);
            return Response
                    .ok(dictionaryEntity)
                    .tag(Long.toString(dictionaryEntity.getUpdatedAt().getTime()))
                    .lastModified(dictionaryEntity.getUpdatedAt())
                    .build();
        }

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("An automatic dictionary can not be deployed manually")
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("_undeploy")
    @ApiOperation(
            value = "Undeploy dictionary to API gateway",
            notes = "User must have the DICTIONARY[UPDATE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Dictionary successfully undeployed", response = DictionaryEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions(@Permission(value = RolePermission.MANAGEMENT_DICTIONARY, acls = RolePermissionAction.UPDATE))
    public Response undeployDictionary(@PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.MANUAL) {
            dictionaryEntity = dictionaryService.undeploy(dictionary);
            return Response
                    .ok(dictionaryEntity)
                    .tag(Long.toString(dictionaryEntity.getUpdatedAt().getTime()))
                    .lastModified(dictionaryEntity.getUpdatedAt())
                    .build();
        }

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("An automatic dictionary can not be undeployed manually")
                .build();
    }

    @DELETE
    @ApiOperation(value = "Delete a dictionary",
            notes = "User must have the DICTIONARY[DELETE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "Dictionary successfully deleted"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.APPLICATION_DEFINITION, acls = RolePermissionAction.DELETE)
    })
    public Response deleteDictionary(@PathParam("dictionary") String dictionary) {
        dictionaryService.delete(dictionary);
        return Response.noContent().build();
    }

    @POST
    @ApiOperation(
            value = "Manage the dictionary's lifecycle",
            notes = "User must have the DICTIONARY[LIFECYCLE] permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Dictionary state updated"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.MANAGEMENT_DICTIONARY, acls = RolePermissionAction.UPDATE)
    })
    public Response doLifecycleAction(
            @Context HttpHeaders headers,
            @ApiParam(required = true, allowableValues = "START, STOP")
            @QueryParam("action") LifecycleActionParam action,
            @PathParam("dictionary") String dictionary) {
        DictionaryEntity dictionaryEntity = dictionaryService.findById(dictionary);

        if (dictionaryEntity.getType() == DictionaryType.DYNAMIC) {
            switch (action.getAction()) {
                case START:
                    checkLifecycle(dictionaryEntity, action.getAction());
                    dictionaryEntity = dictionaryService.start(dictionary);
                    break;
                case STOP:
                    checkLifecycle(dictionaryEntity, action.getAction());
                    dictionaryEntity = dictionaryService.stop(dictionary);
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

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("A manual dictionary can not be started/stopped manually")
                .build();
    }

    private void checkLifecycle(DictionaryEntity dictionary, LifecycleActionParam.LifecycleAction action) {
        switch (dictionary.getState()) {
            case STARTED:
                if (LifecycleActionParam.LifecycleAction.START.equals(action)) {
                    throw new BadRequestException("Dictionary is already started");
                }
                break;
            case STOPPED:
                if (LifecycleActionParam.LifecycleAction.STOP.equals(action)) {
                    throw new BadRequestException("Dictionary is already stopped");
                }
                break;
            default:
                break;
        }
    }
}
