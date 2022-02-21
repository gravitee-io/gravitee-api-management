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

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.swagger.annotations.*;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Api(tags = { "API Keys" })
public class ApiSubscriptionApiKeyResource extends AbstractResource {

    @Inject
    private ApiKeyService apiKeyService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @ApiParam(name = "subscription", hidden = true)
    private String subscription;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("apikey")
    @ApiParam(name = "apikey", hidden = true)
    private String apikey;

    @POST
    @Path("/_reactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Reactivate an API key", notes = "User must have the API_SUBSCRIPTION permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "API key successfully reactivated"),
            @ApiResponse(code = 400, message = "API Key does not correspond to the subscription"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response reactivateApiKeyForApiSubscription() {
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(apikey);

        if (!apiKeyEntity.hasSubscription(subscription)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("api key in path does not correspond to the subscription").build();
        }

        ApiKeyEntity reactivated = apiKeyService.reactivate(apiKeyEntity);
        return Response.ok().entity(reactivated).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Revoke API key", notes = "User must have the API_SUBSCRIPTION:DELETE permission to use this service")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKeyForApiSubscription() {
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(apikey);

        if (!apiKeyEntity.hasSubscription(subscription)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("api key in path does not correspond to the subscription").build();
        }

        apiKeyService.revoke(apiKeyEntity, true);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update API Key", notes = "User must have the API_SUBSCRIPTION:UPDATE permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API Key successfully updated", response = ApiKeyEntity.class),
            @ApiResponse(code = 400, message = "Bad API Key key format"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiKeyForApiSubscription(@Valid @NotNull ApiKeyEntity apiKey) {
        if (!apikey.equals(apiKey.getId())) {
            return Response
                .status(BAD_REQUEST)
                .entity("'apikey' parameter in path does not correspond to the api-key id to update")
                .build();
        }

        ApiKeyEntity updatedKeyEntity = apiKeyService.update(apiKey);
        return Response.ok(updatedKeyEntity).build();
    }
}
