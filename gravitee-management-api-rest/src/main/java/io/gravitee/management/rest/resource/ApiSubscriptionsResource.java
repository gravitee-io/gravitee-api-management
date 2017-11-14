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
import io.gravitee.management.rest.resource.param.LifecycleActionParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.SubscriptionService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.naming.OperationNotSupportedException;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

import static io.gravitee.management.model.SubscriptionStatus.CLOSED;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Subscription"})
public class ApiSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanService planService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApplicationService applicationService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for the API",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of subscriptions", response = Subscription.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public Set<Subscription> listApiSubscriptions(
            @PathParam("api") String api) {
        return subscriptionService.findByApi(api)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @POST
    @Path("{subscription}/status")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Change the status of a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API subscription successfully closed"),
            @ApiResponse(code = 400, message = "Status changes not authorized"),
            @ApiResponse(code = 404, message = "API subscription does not exist"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
    })
    public Response changeStatus(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription,
            @ApiParam(required = true, allowableValues = "CLOSED")
            @QueryParam("status") SubscriptionStatus subscriptionStatus) {
        subscriptionService.findById(subscription);
        if (CLOSED.equals(subscriptionStatus)) {
            subscriptionService.close(subscription);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("{subscription}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all API Keys for a subscription",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API Keys for a subscription", response = ApiKeyEntity.class,
                    responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public Set<ApiKeyEntity> listApiKeysForSubscription(
            @PathParam("subscription") String subscription) {
        return apiKeyService.findBySubscription(subscription);
    }

    @POST
    @Path("{subscription}")
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
    @Path("{subscription}/keys/{key}")
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
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setSubscribedBy(subscriptionEntity.getSubscribedBy());

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        ApplicationEntity application = applicationService.findById(subscriptionEntity.getApplication());
        subscription.setApplication(
                new Subscription.Application(
                        application.getId(),
                        application.getName(),
                        application.getType(),
                        new Subscription.Owner(
                                application.getPrimaryOwner().getUsername(),
                                application.getPrimaryOwner().getFirstname(),
                                application.getPrimaryOwner().getLastname()
                        )
                ));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());

        return subscription;
    }
}
