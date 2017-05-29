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
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiKeyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API"})
public class ApiKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @DELETE
    @Path("{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE)
    })
    public Response revokeApiKey(
            @PathParam("api") String api,
            @PathParam("key") String apiKey) {
        apiKeyService.revoke(apiKey);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
    }

    @PUT
    @Path("{key}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an API Key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API Key successfully updated", response = ApiKeyEntity.class),
            @ApiResponse(code = 400, message = "Bad plan format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
    })
    public Response updateApiKey(
            @PathParam("api") String api,
            @PathParam("key") String apiKey,
            @Valid @NotNull ApiKeyEntity apiKeyEntity) {
        if (apiKeyEntity.getKey() != null && ! apiKey.equals(apiKeyEntity.getKey())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'apiKey' parameter does not correspond to the api-key to update")
                    .build();
        }

        // Force API Key
        apiKeyEntity.setKey(apiKey);

        ApiKeyEntity keyEntity = apiKeyService.update(apiKeyEntity);
        return Response.ok(keyEntity).build();
    }
}
