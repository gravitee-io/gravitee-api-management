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
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.SubscriptionService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Subscription"})
public class ApiPlanSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for an API",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of subscriptions", response = SubscriptionEntity.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Set<SubscriptionEntity> listApiSubscriptions(
            @PathParam("api") String api,
            @PathParam("plan") String plan) {

        if (Visibility.PUBLIC.equals(apiService.findById(api).getVisibility())
                || hasPermission(RolePermission.API_SUBSCRIPTION, api, RolePermissionAction.READ)) {

            Set<SubscriptionEntity> subscriptions = subscriptionService.findByPlan(plan);
            if (isAdmin()) {
                return subscriptions;
            } else if (isAuthenticated()) {
                Set<String> userApps = applicationService.findByUser(getAuthenticatedUsername()).
                        stream().
                        map(ApplicationEntity::getId).
                        collect(Collectors.toSet());
                return subscriptions.
                        stream().
                        filter(subscription -> userApps.contains(subscription.getApplication())).
                        collect(Collectors.toSet());
            }
            return Collections.emptySet();
        }
        throw new ForbiddenAccessException();
    }

    @GET
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Get a subscription", response = SubscriptionEntity.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public SubscriptionEntity getApiSubscription(
            @PathParam("api") String api,
            @PathParam("subscription") String subscription) {
        return subscriptionService.findById(subscription);
    }

    @PUT
    @Path("{subscription}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Update a subscription", response = SubscriptionEntity.class),
            @ApiResponse(code = 400, message = "Bad subscription format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
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
        return Response.ok(subscriptionEntity).build();
    }

    @POST
    @Path("{subscription}/process")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a subscription",
            notes = "User must have the MANAGE_PLANS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Update a subscription", response = SubscriptionEntity.class),
            @ApiResponse(code = 400, message = "Bad subscription format"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.UPDATE)
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

        SubscriptionEntity subscriptionEntity = subscriptionService.process(processSubscriptionEntity, getAuthenticatedUsername());
        return Response.ok(subscriptionEntity).build();
    }

}
