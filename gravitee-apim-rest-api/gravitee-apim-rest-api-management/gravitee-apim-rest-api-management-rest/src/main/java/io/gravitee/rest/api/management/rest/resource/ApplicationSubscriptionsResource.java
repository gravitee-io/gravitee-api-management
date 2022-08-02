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
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.model.wrapper.SubscriptionEntityPageResult;
import io.gravitee.rest.api.management.rest.resource.param.ListStringParam;
import io.gravitee.rest.api.management.rest.resource.param.ListSubscriptionStatusParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Subscriptions")
public class ApplicationSubscriptionsResource extends AbstractResource {

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

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subscribe to a plan", description = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Subscription successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response createSubscriptionWithApplication(
        @Parameter(name = "plan", required = true) @NotNull @QueryParam("plan") String plan,
        NewSubscriptionEntity newSubscriptionEntity
    ) {
        // If no request message has been passed, the entity is not created
        if (newSubscriptionEntity == null) {
            newSubscriptionEntity = new NewSubscriptionEntity();
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        PlanEntity planEntity = planService.findById(executionContext, plan);

        if (
            planEntity.isCommentRequired() && (newSubscriptionEntity.getRequest() == null || newSubscriptionEntity.getRequest().isEmpty())
        ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Plan requires a consumer comment when subscribing").build();
        }

        newSubscriptionEntity.setApplication(application);
        newSubscriptionEntity.setPlan(plan);
        Subscription subscription = convert(executionContext, subscriptionService.create(executionContext, newSubscriptionEntity));
        return Response
            .created(this.getRequestUriBuilder().path(subscription.getId()).replaceQueryParam("plan", null).build())
            .entity(subscription)
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List subscriptions for the application",
        description = "User must have the READ_SUBSCRIPTION permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paged result of application's subscriptions",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = SubscriptionEntityPageResult.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public SubscriptionEntityPageResult getApplicationSubscriptions(
        @BeanParam SubscriptionParam subscriptionParam,
        @Valid @BeanParam Pageable pageable,
        @Parameter(
            description = "Expansion of data to return in subscriptions",
            array = @ArraySchema(schema = @Schema(allowableValues = { "keys", "security" }))
        ) @QueryParam("expand") List<String> expand
    ) {
        // Transform query parameters to a subscription query
        SubscriptionQuery subscriptionQuery = subscriptionParam.toQuery();
        subscriptionQuery.setApplication(application);

        boolean expandApiKeys = expand != null && expand.contains("keys");
        boolean expandPlanSecurity = expand != null && expand.contains("security");
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        Page<SubscriptionEntity> subscriptions = subscriptionService.search(
            executionContext,
            subscriptionQuery,
            pageable.toPageable(),
            expandApiKeys,
            expandPlanSecurity
        );

        SubscriptionEntityPageResult result = new SubscriptionEntityPageResult(subscriptions, pageable.getSize());
        SubscriptionMetadataQuery subscriptionMetadataQuery = new SubscriptionMetadataQuery(
            executionContext.getOrganizationId(),
            executionContext.getEnvironmentId(),
            subscriptions.getContent()
        )
            .withApis(true)
            .withApplications(true)
            .withPlans(true);
        result.setMetadata(subscriptionService.getMetadata(executionContext, subscriptionMetadataQuery).toMap());
        return result;
    }

    @Path("{subscription}")
    public ApplicationSubscriptionResource getApplicationSubscriptionResource() {
        return resourceContext.getResource(ApplicationSubscriptionResource.class);
    }

    private Subscription convert(final ExecutionContext executionContext, SubscriptionEntity subscriptionEntity) {
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
                userService.findById(executionContext, subscriptionEntity.getSubscribedBy(), true).getDisplayName()
            )
        );

        PlanEntity plan = planService.findById(executionContext, subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        subscription.getPlan().setSecurity(plan.getSecurity());

        ApiEntity api = apiService.findById(executionContext, subscriptionEntity.getApi());
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
        @Parameter(description = "plan", explode = Explode.FALSE, schema = @Schema(type = "array"))
        private ListStringParam plans;

        @QueryParam("api")
        @Parameter(description = "api", explode = Explode.FALSE, schema = @Schema(type = "array"))
        private ListStringParam apis;

        @QueryParam("status")
        @DefaultValue("ACCEPTED")
        @Parameter(
            description = "Comma separated list of Subscription status, default is ACCEPTED",
            explode = Explode.FALSE,
            schema = @Schema(type = "array")
        )
        private ListSubscriptionStatusParam status;

        @QueryParam("api_key")
        private String apiKey;

        @QueryParam("security_types")
        private ListStringParam securityTypes;

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

        public ListStringParam getSecurityTypes() {
            return securityTypes;
        }

        public void setSecurityTypes(ListStringParam securityTypes) {
            this.securityTypes = securityTypes;
        }

        private SubscriptionQuery toQuery() {
            SubscriptionQuery query = new SubscriptionQuery();
            query.setApis(apis);
            query.setPlans(plans);
            query.setStatuses(status);
            query.setApiKey(apiKey);
            query.setPlanSecurityTypes(securityTypes);
            return query;
        }
    }
}
