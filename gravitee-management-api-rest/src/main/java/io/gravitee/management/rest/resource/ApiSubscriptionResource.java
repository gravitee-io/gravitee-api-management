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
import io.gravitee.management.model.*;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.rest.model.Subscription;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.*;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import static io.gravitee.management.model.SubscriptionStatus.*;
import static io.gravitee.management.model.permissions.RolePermissionAction.UPDATE;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Subscription"})
public class ApiSubscriptionResource extends AbstractResource {

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Get a subscription", response = Subscription.class),
            @ApiResponse(code = 404, message = "Subscription does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public Subscription getApiSubscription(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription) {
        return convert(subscriptionService.findById(subscription));
    }

    @POST
    @Path("/_process")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Update a subscription", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad subscription format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = UPDATE)
    })
    public Response processApiSubscription(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription,
            @ApiParam(name = "subscription", required = true) @Valid @NotNull ProcessSubscriptionEntity processSubscriptionEntity) {

        if (processSubscriptionEntity.getId() != null && ! subscription.equals(processSubscriptionEntity.getId())) {
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
    @ApiOperation(value = "Update a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Update a subscription", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad subscription format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = UPDATE)
    })
    public Response updateApiSubscription(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription,
            @ApiParam(name = "subscription", required = true) @Valid @NotNull UpdateSubscriptionEntity updateSubscriptionEntity) {

        if (updateSubscriptionEntity.getId() != null && ! subscription.equals(updateSubscriptionEntity.getId())) {
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
    @ApiOperation(value = "Change the status of a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Subscription status successfully updated", response = Subscription.class),
            @ApiResponse(code = 400, message = "Status changes not authorized"),
            @ApiResponse(code = 404, message = "API subscription does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
    })
    public Response changeSubscriptionStatus(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription,
            @ApiParam(required = true, allowableValues = "CLOSED, PAUSED, RESUMED")
            @QueryParam("status") SubscriptionStatus subscriptionStatus) {
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

    @GET
    @Path("/keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all API Keys for a subscription",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API Keys for a subscription", response = ApiKeyEntity.class,
                    responseContainer = "List"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public List<ApiKeyEntity> listApiKeysForSubscription(
            @PathParam("subscription") String subscription) {
        return apiKeyService.findBySubscription(subscription)
                .stream().sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt())).collect(Collectors.toList());
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Renew an API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new API Key", response = ApiKeyEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
    })
    public Response renewApiKey(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription) {
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(subscription);
        return Response
                .created(URI.create("/apis/" + api + "/subscriptions/" + subscription +
                        "/keys" + apiKeyEntity.getKey()))
                .entity(apiKeyEntity)
                .build();
    }

    @DELETE
    @Path("/keys/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Revoke an API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API key successfully revoked"),
            @ApiResponse(code = 400, message = "API Key does not correspond to the subscription"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.DELETE)
    })
    public Response revokeApiKey(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription,
            @PathParam("key") String apiKey) {
        ApiKeyEntity apiKeyEntity = apiKeyService.findByKey(apiKey);
        if (apiKeyEntity.getSubscription() != null && ! subscription.equals(apiKeyEntity.getSubscription())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'key' parameter does not correspond to the subscription")
                    .build();
        }

        apiKeyService.revoke(apiKey, true);

        return Response
                .status(Response.Status.NO_CONTENT)
                .build();
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
                        userService.findById(subscriptionEntity.getSubscribedBy()).getDisplayName()));
        subscription.setClientId(subscriptionEntity.getClientId());

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        subscription.getPlan().setSecurity(plan.getSecurity());

        ApplicationEntity application = applicationService.findById(subscriptionEntity.getApplication());
        subscription.setApplication(
                new Subscription.Application(
                        application.getId(),
                        application.getName(),
                        application.getType(),
                        new Subscription.User(
                                application.getPrimaryOwner().getId(),
                                application.getPrimaryOwner().getDisplayName()
                        )
                ));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());

        return subscription;
    }
}
