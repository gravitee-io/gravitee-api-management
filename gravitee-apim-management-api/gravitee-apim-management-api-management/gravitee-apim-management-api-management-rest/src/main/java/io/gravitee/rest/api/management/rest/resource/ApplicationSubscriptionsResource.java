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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.resource.param.ListStringParam;
import io.gravitee.rest.api.management.rest.resource.param.ListSubscriptionStatusParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.swagger.annotations.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Application Subscriptions" })
public class ApplicationSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanService planService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ApiService apiService;

    @Inject
    private UserService userService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @ApiParam(name = "application", hidden = true)
    private String application;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Subscribe to a plan", notes = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Subscription successfully created", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response createSubscriptionWithApplication(
        @ApiParam(name = "plan", required = true) @NotNull @QueryParam("plan") String plan,
        NewSubscriptionEntity newSubscriptionEntity
    ) {
        // If no request message has been passed, the entity is not created
        if (newSubscriptionEntity == null) {
            newSubscriptionEntity = new NewSubscriptionEntity();
        }

        PlanEntity planEntity = planService.findById(plan);

        if (
            planEntity.isCommentRequired() && (newSubscriptionEntity.getRequest() == null || newSubscriptionEntity.getRequest().isEmpty())
        ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Plan requires a consumer comment when subscribing").build();
        }

        newSubscriptionEntity.setApplication(application);
        newSubscriptionEntity.setPlan(plan);
        Subscription subscription = convert(subscriptionService.create(newSubscriptionEntity));
        return Response
            .created(this.getRequestUriBuilder().path(subscription.getId()).replaceQueryParam("plan", null).build())
            .entity(subscription)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "List subscriptions for the application",
        notes = "User must have the READ_SUBSCRIPTION permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Paged result of application's subscriptions", response = PagedResult.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public PagedResult<SubscriptionEntity> getApplicationSubscriptions(
        @BeanParam SubscriptionParam subscriptionParam,
        @Valid @BeanParam Pageable pageable,
        @ApiParam(allowableValues = "keys", value = "Expansion of data to return in subscriptions") @QueryParam(
            "expand"
        ) List<String> expand
    ) {
        // Transform query parameters to a subscription query
        SubscriptionQuery subscriptionQuery = subscriptionParam.toQuery();
        subscriptionQuery.setApplication(application);
        Page<SubscriptionEntity> subscriptions = subscriptionService.search(subscriptionQuery, pageable.toPageable());

        if (expand != null && !expand.isEmpty()) {
            for (String e : expand) {
                switch (e) {
                    case "keys":
                        subscriptions
                            .getContent()
                            .forEach(
                                subscriptionEntity -> {
                                    final List<String> keys = apiKeyService
                                        .findBySubscription(subscriptionEntity.getId())
                                        .stream()
                                        .filter(apiKeyEntity -> !apiKeyEntity.isExpired() && !apiKeyEntity.isRevoked())
                                        .map(ApiKeyEntity::getKey)
                                        .collect(Collectors.toList());
                                    subscriptionEntity.setKeys(keys);
                                }
                            );
                        break;
                    default:
                        break;
                }
            }
        }

        PagedResult<SubscriptionEntity> result = new PagedResult<>(subscriptions, pageable.getSize());
        result.setMetadata(subscriptionService.getMetadata(subscriptions.getContent()).getMetadata());
        return result;
    }

    @GET
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get subscription information", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Subscription information", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Subscription getApplicationSubscription(@PathParam("subscription") String subscription) {
        return convert(subscriptionService.findById(subscription));
    }

    @DELETE
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Close the subscription",
        notes = "User must have the APPLICATION_SUBSCRIPTION[DELETE] permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Subscription has been closed successfully", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response closeApplicationSubscription(@PathParam("subscription") String subscriptionId) {
        SubscriptionEntity subscription = subscriptionService.findById(subscriptionId);
        if (subscription.getApplication().equals(application)) {
            return Response.ok(convert(subscriptionService.close(subscriptionId))).build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @GET
    @Path("{subscription}/keys")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all API Keys for a subscription", notes = "User must have the READ permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(
                code = 200,
                message = "List of API Keys for a subscription",
                response = ApiKeyEntity.class,
                responseContainer = "Set"
            ),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public List<ApiKeyEntity> getApiKeysForApplicationSubscription(@PathParam("subscription") String subscription) {
        return apiKeyService.findBySubscription(subscription);
    }

    @POST
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Renew an API key", notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "A new API Key", response = ApiKeyEntity.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewApiKeyForApplicationSubscription(@PathParam("subscription") String subscription) {
        ApiKeyEntity apiKeyEntity = apiKeyService.renew(subscription);
        return Response.created(this.getLocationHeader("keys", apiKeyEntity.getKey())).entity(apiKeyEntity).build();
    }

    @DELETE
    @Path("{subscription}/keys/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Revoke an API key", notes = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 204, message = "API key successfully revoked"),
            @ApiResponse(code = 400, message = "API Key does not correspond to the subscription"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKeyForApplicationSubscription(
        @PathParam("subscription") String subscription,
        @PathParam("key") String apiKey
    ) {
        ApiKeyEntity apiKeyEntity = apiKeyService.findByKey(apiKey);
        if (apiKeyEntity.getSubscription() != null && !subscription.equals(apiKeyEntity.getSubscription())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("'key' parameter does not correspond to the subscription").build();
        }

        apiKeyService.revoke(apiKey, true);

        return Response.status(Response.Status.NO_CONTENT).build();
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
                userService.findById(subscriptionEntity.getSubscribedBy(), true).getDisplayName()
            )
        );

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        subscription.getPlan().setSecurity(plan.getSecurity());

        ApiEntity api = apiService.findById(subscriptionEntity.getApi());
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

    private static class SubscriptionParam {

        @QueryParam("plan")
        @ApiParam(value = "plan")
        private ListStringParam plans;

        @QueryParam("api")
        @ApiParam(value = "api")
        private ListStringParam apis;

        @QueryParam("status")
        @DefaultValue("accepted,pending,paused")
        @ApiModelProperty(dataType = "string", allowableValues = "accepted, pending, rejected, closed", value = "Subscription status")
        private ListSubscriptionStatusParam status;

        @QueryParam("api_key")
        private String apiKey;

        public ListStringParam getPlans() {
            return plans;
        }

        public void setPlans(ListStringParam plans) {
            this.plans = plans;
        }

        public ListStringParam getApis() {
            return apis;
        }

        public void setApis(ListStringParam apis) {
            this.apis = apis;
        }

        public ListSubscriptionStatusParam getStatus() {
            return status;
        }

        public void setStatus(ListSubscriptionStatusParam status) {
            this.status = status;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        private SubscriptionQuery toQuery() {
            SubscriptionQuery query = new SubscriptionQuery();

            if (apis != null && apis.getValue() != null) {
                query.setApis(apis.getValue());
            }

            if (plans != null && plans.getValue() != null) {
                query.setPlans(plans.getValue());
            }

            if (status != null) {
                query.setStatuses(status.getStatus());
            }

            if (apiKey != null) {
                query.setApiKey(apiKey);
            }
            return query;
        }
    }
}
