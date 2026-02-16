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

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import io.gravitee.apim.core.api_key.use_case.RevokeApiSubscriptionApiKeyUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Keys")
public class ApiSubscriptionApiKeyResource extends AbstractApiKeyResource {

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private RevokeApiSubscriptionApiKeyUseCase revokeApiSubscriptionApiKeyUsecase;

    @Inject
    private SubscriptionService subscriptionService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @Parameter(name = "subscription", hidden = true)
    private String subscription;

    @PathParam("apikey")
    @Parameter(name = "apikey")
    private String apikey;

    @POST
    @Path("/_reactivate")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reactivate an API Key", description = "User must have the API_SUBSCRIPTION permission to use this service")
    @ApiResponse(responseCode = "204", description = "API Key successfully reactivated")
    @ApiResponse(responseCode = "400", description = "API Key does not correspond to the subscription")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response reactivateApiKeyForApiSubscription() {
        // Check subscription exists and belongs to API
        checkSubscription(subscription);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apikey);
        if (!apiKeyEntity.hasSubscription(subscription)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("API Key in path does not correspond to the subscription").build();
        }

        checkApplicationDoesntUseSharedApiKey(apiKeyEntity.getApplication());

        ApiKeyEntity reactivated = apiKeyService.reactivate(executionContext, apiKeyEntity);
        return Response.ok().entity(reactivated).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke API Key", description = "User must have the API_SUBSCRIPTION:DELETE permission to use this service")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKeyForApiSubscription() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        revokeApiSubscriptionApiKeyUsecase.execute(
            new RevokeApiSubscriptionApiKeyUseCase.Input(
                apikey,
                api,
                SubscriptionReferenceType.API.name(),
                subscription,
                AuditInfo.builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(
                        AuditActor.builder()
                            .userId(user.getUsername())
                            .userSource(user.getSource())
                            .userSourceId(user.getSourceId())
                            .build()
                    )
                    .build()
            )
        );

        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update API Key", description = "User must have the API_SUBSCRIPTION:UPDATE permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "API Key successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiKeyEntity.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad API Key key format")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response updateApiKeyForApiSubscription(@Valid @NotNull ApiKeyEntity apiKey) {
        // Check subscription exists and belongs to API
        SubscriptionEntity subscriptionEntity = checkSubscription(subscription);

        if (!apikey.equals(apiKey.getId())) {
            return Response.status(BAD_REQUEST)
                .entity("'apikey' parameter in path does not correspond to the api-key id to update")
                .build();
        }

        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        ApiKeyEntity apiKeyEntity = apiKeyService.findById(executionContext, apikey);
        if (!apiKeyEntity.hasSubscription(subscription)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("API Key in path does not correspond to the subscription").build();
        }

        checkApplicationDoesntUseSharedApiKey(executionContext, subscriptionEntity.getApplication());
        ApiKeyEntity updatedKeyEntity = apiKeyService.update(executionContext, apiKey);
        return Response.ok(updatedKeyEntity).build();
    }

    private SubscriptionEntity checkSubscription(String subscription) {
        SubscriptionEntity searchedSubscription = subscriptionService.findById(subscription);
        if (api.equalsIgnoreCase(searchedSubscription.getApi())) {
            return searchedSubscription;
        }
        throw new SubscriptionNotFoundException(subscription);
    }
}
