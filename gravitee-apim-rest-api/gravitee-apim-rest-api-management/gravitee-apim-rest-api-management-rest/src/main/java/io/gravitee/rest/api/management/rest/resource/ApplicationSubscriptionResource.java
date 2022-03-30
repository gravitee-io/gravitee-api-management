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
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
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
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
    private PlanService planService;

    @Inject
    private ApiService apiService;

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
        return convert(subscriptionService.findById(subscription));
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
            return Response.ok(convert(subscriptionService.close(GraviteeContext.getExecutionContext(), subscription))).build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @Path("apikeys")
    public ApplicationSubscriptionApiKeysResource getApiSubscriptionApiKeysResourceResource() {
        return resourceContext.getResource(ApplicationSubscriptionApiKeysResource.class);
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
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setSubscribedBy(
            new Subscription.User(
                subscriptionEntity.getSubscribedBy(),
                userService.findById(GraviteeContext.getExecutionContext(), subscriptionEntity.getSubscribedBy(), true).getDisplayName()
            )
        );

        PlanEntity plan = planService.findById(GraviteeContext.getExecutionContext(), subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        subscription.getPlan().setSecurity(plan.getSecurity());

        ApiEntity api = apiService.findById(GraviteeContext.getExecutionContext(), subscriptionEntity.getApi());
        subscription.setApi(
            new Subscription.Api(
                api.getId(),
                api.getName(),
                api.getVersion(),
                new Subscription.User(api.getPrimaryOwner().getId(), api.getPrimaryOwner().getDisplayName())
            )
        );

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());

        return subscription;
    }
}
