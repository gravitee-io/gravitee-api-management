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

import static io.gravitee.rest.api.model.SubscriptionStatus.*;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Subscriptions")
public class ApiSubscriptionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private PlanService planService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @Inject
    private ParameterService parameterService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @Parameter(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a subscription", description = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Get a subscription",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "404", description = "Subscription does not exist")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Subscription getApiSubscription(@PathParam("subscription") String subscription) {
        return convert(subscriptionService.findById(subscription));
    }

    @POST
    @Path("/_process")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a subscription", description = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Update a subscription",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad subscription format")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = UPDATE) })
    public Response processApiSubscription(
        @PathParam("subscription") String subscription,
        @Parameter(name = "subscription", required = true) @Valid @NotNull ProcessSubscriptionEntity processSubscriptionEntity
    ) {
        if (processSubscriptionEntity.getId() != null && !subscription.equals(processSubscriptionEntity.getId())) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to process")
                .build();
        }

        // Force subscription ID
        processSubscriptionEntity.setId(subscription);

        SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscriptionEntity, getAuthenticatedUser());
        return Response.ok(convert(subscriptionEntity)).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a subscription", description = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Update a subscription",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad subscription format")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = UPDATE) })
    public Response updateApiSubscription(
        @PathParam("subscription") String subscription,
        @Parameter(name = "subscription", required = true) @Valid @NotNull UpdateSubscriptionEntity updateSubscriptionEntity
    ) {
        if (updateSubscriptionEntity.getId() != null && !subscription.equals(updateSubscriptionEntity.getId())) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to update")
                .build();
        }

        // Force ID
        updateSubscriptionEntity.setId(subscription);

        SubscriptionEntity subscriptionEntity = subscriptionService.update(updateSubscriptionEntity);
        return Response.ok(convert(subscriptionEntity)).build();
    }

    @POST
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Change the status of a subscription",
        description = "User must have the MANAGE_PLANS permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Subscription status successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "400", description = "Status changes not authorized")
    @ApiResponse(responseCode = "404", description = "API subscription does not exist")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response changeApiSubscriptionStatus(
        @PathParam("subscription") String subscription,
        @Parameter(required = true, schema = @Schema(allowableValues = { "CLOSED", "PAUSED", "RESUMED" })) @QueryParam(
            "status"
        ) SubscriptionStatus subscriptionStatus
    ) {
        if (CLOSED.equals(subscriptionStatus)) {
            SubscriptionEntity updatedSubscriptionEntity = subscriptionService.close(subscription);
            return Response.ok(convert(updatedSubscriptionEntity)).build();
        } else if (PAUSED.equals(subscriptionStatus)) {
            SubscriptionEntity updatedSubscriptionEntity = subscriptionService.pause(subscription);
            return Response.ok(convert(updatedSubscriptionEntity)).build();
        } else if (RESUMED.equals(subscriptionStatus)) {
            SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resume(subscription);
            return Response.ok(convert(updatedSubscriptionEntity)).build();
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @POST
    @Path("/_transfer")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Transfer a subscription",
        description = "User must have the API_SUBSCRIPTION update permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Update a subscription",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "400", description = "Bad subscription format")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = UPDATE) })
    public Response transferApiSubscription(
        @PathParam("subscription") String subscription,
        @Parameter(name = "subscription", required = true) @Valid @NotNull TransferSubscriptionEntity transferSubscriptionEntity
    ) {
        if (transferSubscriptionEntity.getId() != null && !subscription.equals(transferSubscriptionEntity.getId())) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to process")
                .build();
        }

        // Force subscription ID
        transferSubscriptionEntity.setId(subscription);

        SubscriptionEntity subscriptionEntity = subscriptionService.transfer(transferSubscriptionEntity, getAuthenticatedUser());
        return Response.ok(convert(subscriptionEntity)).build();
    }

    private Subscription convert(SubscriptionEntity subscriptionEntity) {
        Subscription subscription = new Subscription();

        subscription.setId(subscriptionEntity.getId());
        subscription.setCreatedAt(subscriptionEntity.getCreatedAt());
        subscription.setUpdatedAt(subscriptionEntity.getUpdatedAt());
        subscription.setStartingAt(subscriptionEntity.getStartingAt());
        subscription.setEndingAt(subscriptionEntity.getEndingAt());
        subscription.setProcessedAt(subscriptionEntity.getProcessedAt());
        subscription.setProcessedBy(subscriptionEntity.getProcessedBy());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setSubscribedBy(
            new Subscription.User(
                subscriptionEntity.getSubscribedBy(),
                userService.findById(subscriptionEntity.getSubscribedBy(), true).getDisplayName()
            )
        );
        subscription.setClientId(subscriptionEntity.getClientId());

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        subscription.getPlan().setSecurity(plan.getSecurity());

        ApplicationEntity application = applicationService.findById(
            GraviteeContext.getCurrentEnvironment(),
            subscriptionEntity.getApplication()
        );
        subscription.setApplication(
            new Subscription.Application(
                application.getId(),
                application.getName(),
                application.getType(),
                application.getDescription(),
                application.getDomain(),
                new Subscription.User(application.getPrimaryOwner().getId(), application.getPrimaryOwner().getDisplayName()),
                application.getApiKeyMode()
            )
        );

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());

        return subscription;
    }

    @Path("apikeys")
    public ApiSubscriptionApiKeysResource getApiSubscriptionApiKeysResourceResource() {
        return resourceContext.getResource(ApiSubscriptionApiKeysResource.class);
    }
}
