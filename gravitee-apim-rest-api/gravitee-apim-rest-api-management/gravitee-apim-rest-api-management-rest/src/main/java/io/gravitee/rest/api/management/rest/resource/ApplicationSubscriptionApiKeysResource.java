/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Keys")
public class ApplicationSubscriptionApiKeysResource extends AbstractResource {

    @Inject
    private ApiKeyService apiKeyService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @Parameter(name = "subscription", hidden = true)
    private String subscription;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all API Keys for a Subscription", description = "User must have the APPLICATION_SUBSCRIPTION permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of API Keys for a Subscription",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApiKeyEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public List<ApiKeyEntity> getApiKeysForApplicationSubscription() {
        return apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), subscription);
    }

    @POST
    @Path("/_renew")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Renew an API key", description = "User must have the APPLICATION_SUBSCRIPTION permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API Key",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiKeyEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewApiKeyForApplicationSubscription(@QueryParam("customApiKey") String customApiKey) {
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(GraviteeContext.getExecutionContext(), subscription, customApiKey);
        return Response.ok(apiKeyEntity).build();
    }

    @DELETE
    @Path("/{apikey}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke an API Key", description = "User must have the APPLICATION_SUBSCRIPTION permission to use this service")
    @ApiResponse(
        responseCode = "204",
        description = "API key successfully revoked"
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKeyForApplicationSubscription(@PathParam("apikey") String apiKeyId) {
        apiKeyService.revoke(GraviteeContext.getExecutionContext(), apiKeyId, true);
        return Response.noContent().build();
    }
}
