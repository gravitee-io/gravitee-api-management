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

import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "Application Subscriptions")
public class ApplicationSubscriptionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private UserService userService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @PathParam("subscription")
    @Parameter(name = "subscription")
    private String subscription;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get subscription information", description = "User must have the READ permission to use this service")
    @ApiResponse(
        responseCode = "200",
        description = "Subscription information",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Subscription getApplicationSubscription() {
        return convert(GraviteeContext.getExecutionContext(), subscriptionService.findById(subscription));
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Close the subscription",
        description = "User must have the APPLICATION_SUBSCRIPTION[DELETE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Subscription has been closed successfully",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response closeApplicationSubscription() {
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscription);
        if (subscriptionEntity.getApplication().equals(application)) {
            final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
            return Response.ok(convert(executionContext, subscriptionService.close(executionContext, subscription))).build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("apikeys")
    public ApplicationSubscriptionApiKeysResource getApiSubscriptionApiKeysResourceResource() {
        return resourceContext.getResource(ApplicationSubscriptionApiKeysResource.class);
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Update a subscription configuration",
        description = "User must have the APPLICATION_SUBSCRIPTION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Update a subscription configuration",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionEntity.class))
    )
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @ApiResponse(responseCode = "400", description = "Subscription not updatable, or bad subscription configuration format")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = UPDATE) })
    public Response updateApplicationSubscription(
        @Valid @NotNull UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity
    ) {
        updateSubscriptionConfigurationEntity.setSubscriptionId(subscription);
        SubscriptionEntity updatedSubscription = subscriptionService.update(
            GraviteeContext.getExecutionContext(),
            updateSubscriptionConfigurationEntity
        );
        return Response.ok(updatedSubscription).build();
    }

    @POST
    @Path("/_changeConsumerStatus")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Change the status of a subscription",
        description = "User must have the APPLICATION_SUBSCRIPTION[UPDATE] permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Subscription status successfully updated",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "400", description = "Status changes not authorized")
    @ApiResponse(responseCode = "404", description = "API subscription does not exist")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response changeSubscriptionConsumerStatus(
        @Parameter(required = true, schema = @Schema(allowableValues = { "STARTED", "STOPPED" })) @QueryParam(
            "status"
        ) SubscriptionConsumerStatus subscriptionConsumerStatus
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (subscriptionConsumerStatus == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        switch (subscriptionConsumerStatus) {
            case STARTED:
                {
                    SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resumeConsumer(executionContext, subscription);
                    return Response.ok(convert(executionContext, updatedSubscriptionEntity)).build();
                }
            case STOPPED:
                {
                    SubscriptionEntity updatedSubscriptionEntity = subscriptionService.pauseConsumer(executionContext, subscription);
                    return Response.ok(convert(executionContext, updatedSubscriptionEntity)).build();
                }
            default:
                return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    private Subscription convert(ExecutionContext executionContext, SubscriptionEntity subscriptionEntity) {
        Subscription subscription = new Subscription();

        subscription.setId(subscriptionEntity.getId());
        subscription.setCreatedAt(subscriptionEntity.getCreatedAt());
        subscription.setUpdatedAt(subscriptionEntity.getUpdatedAt());
        subscription.setStartingAt(subscriptionEntity.getStartingAt());
        subscription.setEndingAt(subscriptionEntity.getEndingAt());
        subscription.setProcessedAt(subscriptionEntity.getProcessedAt());
        subscription.setProcessedBy(subscriptionEntity.getProcessedBy());
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setConsumerStatus(subscriptionEntity.getConsumerStatus());
        subscription.setSubscribedBy(
            new Subscription.User(
                subscriptionEntity.getSubscribedBy(),
                userService.findById(executionContext, subscriptionEntity.getSubscribedBy(), true).getDisplayName()
            )
        );

        GenericPlanEntity plan = planSearchService.findById(executionContext, subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        if (plan.getPlanSecurity() != null) {
            subscription.getPlan().setSecurity(PlanSecurityType.valueOfLabel(plan.getPlanSecurity().getType()).name());
        }

        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, subscriptionEntity.getApi());
        subscription.setApi(
            new Subscription.Api(
                genericApiEntity.getId(),
                genericApiEntity.getName(),
                genericApiEntity.getApiVersion(),
                new Subscription.User(genericApiEntity.getPrimaryOwner().getId(), genericApiEntity.getPrimaryOwner().getDisplayName())
            )
        );

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());
        subscription.setConsumerPausedAt(subscriptionEntity.getConsumerPausedAt());

        return subscription;
    }
}
