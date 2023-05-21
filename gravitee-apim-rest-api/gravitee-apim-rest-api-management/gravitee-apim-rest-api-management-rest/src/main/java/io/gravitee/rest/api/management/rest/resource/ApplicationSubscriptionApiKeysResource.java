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
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Keys")
public class ApplicationSubscriptionApiKeysResource extends AbstractApiKeyResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private SubscriptionService subscriptionService;

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
    @Operation(summary = "List all API Keys for a subscription", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "List of API Keys for a subscription",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApiKeyEntity.class), uniqueItems = true)
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
    @Operation(summary = "Renew an API key", description = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "A new API Key",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiKeyEntity.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewApiKeyForApplicationSubscription() {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        checkApplicationDoesntUseSharedApiKey(executionContext, application);
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(executionContext, subscriptionEntity);
        URI location = URI.create(uriInfo.getPath().replace("_renew", apiKeyEntity.getId()));
        return Response.created(location).entity(apiKeyEntity).build();
    }

    @Path("{apikey}")
    public ApplicationSubscriptionApiKeyResource getApplicationSubscriptionApiKeyResource() {
        return resourceContext.getResource(ApplicationSubscriptionApiKeyResource.class);
    }
}
