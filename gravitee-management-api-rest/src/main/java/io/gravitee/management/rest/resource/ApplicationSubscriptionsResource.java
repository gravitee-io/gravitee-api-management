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
import io.gravitee.management.model.ApiKeyEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.SubscriptionEntity;
import io.gravitee.management.model.permissions.ApplicationPermission;
import io.gravitee.management.rest.model.Subscription;
import io.gravitee.management.rest.security.ApplicationPermissionsRequired;
import io.gravitee.management.service.ApiKeyService;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.SubscriptionService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"Application", "Subscription"})
public class ApplicationSubscriptionsResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanService planService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApiService apiService;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_PLANS)
    @ApiOperation(value = "Subscribe to a plan",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Subscription successfully created", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response createSubscription(
            @PathParam("application") String application,
            @ApiParam(name = "plan", required = true)
            @NotNull @QueryParam("plan") String plan) {
        Subscription subscription = convert(subscriptionService.create(plan, application));
        return Response
                .created(URI.create("/applications/" + application + "/subscriptions/" + subscription.getId()))
                .entity(subscription)
                .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    @ApiOperation(value = "List subscriptions for the application",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of subscriptions", response = Subscription.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Set<Subscription> listApplicationSubscriptions(
            @PathParam("application") String application) {
        return subscriptionService.findByApplicationAndPlan(application, null)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    @ApiOperation(value = "Get subscription information",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Subscription information", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Subscription getSubscription(
            @PathParam("subscription") String subscription) {
        return convert(subscriptionService.findById(subscription));
    }

    @GET
    @Path("{subscription}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.READ)
    @ApiOperation(value = "List all API Keys for a subscription",
            notes = "User must have the READ permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of API Keys for a subscription", response = ApiKeyEntity.class,
                    responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Set<ApiKeyEntity> listApiKeysForSubscription(
            @PathParam("subscription") String subscription) {
        return apiKeyService.findBySubscription(subscription);
    }

    @POST
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API_KEYS)
    @ApiOperation(value = "Renew an API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "A new API Key", response = ApiKeyEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response renewApiKey(
            @PathParam("application") String application,
            @PathParam("subscription") String subscription) {
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(subscription);
        return Response
                .created(URI.create("/applications/" + application + "/subscriptions/" + subscription +
                        "/keys" + apiKeyEntity.getKey()))
                .entity(apiKeyEntity)
                .build();
    }

    @DELETE
    @Path("{subscription}/keys/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApplicationPermissionsRequired(ApplicationPermission.MANAGE_API_KEYS)
    @ApiOperation(value = "Revoke an API key",
            notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 204, message = "API key successfully revoked"),
            @ApiResponse(code = 400, message = "API Key does not correspond to the subscription"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Response revokeApiKey(
            @PathParam("application") String application,
            @PathParam("subscription") String subscription,
            @PathParam("key") String apiKey) {
        ApiKeyEntity apiKeyEntity = apiKeyService.findByKey(apiKey);
        if (apiKeyEntity.getSubscription() != null && ! subscription.equals(apiKeyEntity.getSubscription())) {
            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("'key' parameter does not correspond to the subscription")
                    .build();
        }

        apiKeyService.revoke(apiKey);

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

        subscription.getPlan().setApis(plan.getApis().stream().map(api -> {
            io.gravitee.management.model.ApiEntity apiEntity = apiService.findById(api);
            return new Subscription.Api(apiEntity.getId(), apiEntity.getName());
        }).collect(Collectors.toList()));

        return subscription;
    }
}
