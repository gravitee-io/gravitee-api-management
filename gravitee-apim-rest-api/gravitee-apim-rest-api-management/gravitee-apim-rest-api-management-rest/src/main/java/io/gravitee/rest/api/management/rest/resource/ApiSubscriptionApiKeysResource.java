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
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.validator.CustomApiKey;
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
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Keys")
public class ApiSubscriptionApiKeysResource extends AbstractApiKeyResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private SubscriptionService subscriptionService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @Parameter(name = "subscription")
    private String subscription;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List all API Keys for a subscription",
        description = "User must have the MANAGE_API_KEYS permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of API Keys for a subscription",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = ApiKeyEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public List<ApiKeyEntity> getApiKeysForApiSubscription() {
        // Check subscription exists and belongs to API
        checkSubscription(subscription);

        return apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), subscription);
    }

    @POST
    @Path("/_renew")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Renew an API Key", description = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "A new API Key",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiKeyEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad custom API Key format or custom API Key definition disabled")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewSubscriptionApiKeysForApiSubscription(
        @Parameter(name = "customApiKey") @CustomApiKey @QueryParam("customApiKey") String customApiKey
    ) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        if (
            StringUtils.isNotEmpty(customApiKey) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("You are not allowed to provide a custom API Key").build();
        }

        // Check subscription exists and belongs to API
        SubscriptionEntity subscriptionEntity = checkSubscription(subscription);

        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());

        ApiKeyEntity apiKeyEntity = apiKeyService.renew(executionContext, subscriptionEntity, customApiKey);

        URI location = URI.create(uriInfo.getPath().replace("_renew", apiKeyEntity.getId()));
        return Response.created(location).entity(apiKeyEntity).build();
    }

    @Path("{apikey}")
    public ApiSubscriptionApiKeyResource getApiSubscriptionApiKeyResource() {
        return resourceContext.getResource(ApiSubscriptionApiKeyResource.class);
    }

    private SubscriptionEntity checkSubscription(String subscription) {
        SubscriptionEntity searchedSubscription = subscriptionService.findById(subscription);
        if (
            api.equalsIgnoreCase(searchedSubscription.getReferenceId()) &&
            searchedSubscription.getReferenceType().equalsIgnoreCase(GenericPlanEntity.ReferenceType.API.name())
        ) {
            return searchedSubscription;
        }
        throw new SubscriptionNotFoundException(subscription);
    }
}
