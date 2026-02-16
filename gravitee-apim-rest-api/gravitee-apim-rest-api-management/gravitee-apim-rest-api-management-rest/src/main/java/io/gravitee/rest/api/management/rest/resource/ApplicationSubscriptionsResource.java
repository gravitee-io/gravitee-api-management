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

import io.gravitee.apim.core.api_product.use_case.GetApiProductsUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.model.wrapper.SubscriptionEntityPageResult;
import io.gravitee.rest.api.management.rest.resource.param.ListStringParam;
import io.gravitee.rest.api.management.rest.resource.param.ListSubscriptionStatusParam;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.ReferenceDisplayInfo;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Objects;

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
    private PlanSearchService planSearchService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @Inject
    private GetApiProductsUseCase getApiProductsUseCase;

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
        GenericPlanEntity planEntity = planSearchService.findById(executionContext, plan);

        if (
            planEntity.isCommentRequired() && (newSubscriptionEntity.getRequest() == null || newSubscriptionEntity.getRequest().isEmpty())
        ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Plan requires a consumer comment when subscribing").build();
        }

        newSubscriptionEntity.setApplication(application);
        newSubscriptionEntity.setPlan(plan);
        Subscription subscription = convert(executionContext, subscriptionService.create(executionContext, newSubscriptionEntity));
        return Response.created(this.getRequestUriBuilder().path(subscription.getId()).replaceQueryParam("plan", null).build())
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
            .withApiProducts(true)
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

        GenericPlanEntity plan = planSearchService.findById(executionContext, subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));
        if (plan.getPlanMode() == PlanMode.STANDARD) {
            subscription.getPlan().setSecurity(plan.getPlanSecurity().getType());
        }

        subscription.setReferenceId(subscriptionEntity.getReferenceId());
        subscription.setReferenceType(subscriptionEntity.getReferenceType());
        if (
            subscriptionEntity.getReferenceId() != null &&
            SubscriptionReferenceType.API_PRODUCT.name().equals(subscriptionEntity.getReferenceType())
        ) {
            fetchApiProductAndSetSubscriptionApiProduct(executionContext, subscription, subscriptionEntity.getReferenceId());
        } else if (subscriptionEntity.getApi() != null) {
            subscription.setApi(fetchApi(executionContext, subscriptionEntity.getApi()));
        }

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());

        return subscription;
    }

    private Subscription.Api fetchApi(ExecutionContext executionContext, String apiId) {
        GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId, false, false, false);
        return new Subscription.Api(
            genericApiEntity.getId(),
            genericApiEntity.getName(),
            genericApiEntity.getApiVersion(),
            genericApiEntity.getDefinitionVersion(),
            new Subscription.User(genericApiEntity.getPrimaryOwner().getId(), genericApiEntity.getPrimaryOwner().getDisplayName())
        );
    }

    private void fetchApiProductAndSetSubscriptionApiProduct(
        ExecutionContext executionContext,
        Subscription subscription,
        String referenceId
    ) {
        subscriptionService
            .getReferenceDisplayInfo(executionContext, SubscriptionReferenceType.API_PRODUCT.name(), referenceId)
            .ifPresent(ref -> {
                var input = GetApiProductsUseCase.Input.of(
                    executionContext.getEnvironmentId(),
                    referenceId,
                    executionContext.getOrganizationId()
                );
                Subscription.User owner = getApiProductsUseCase
                    .execute(input)
                    .apiProduct()
                    .filter(ap -> ap.getPrimaryOwner() != null)
                    .map(ap -> {
                        var po = ap.getPrimaryOwner();
                        return new Subscription.User(
                            Objects.requireNonNullElse(po.id(), ""),
                            Objects.requireNonNullElse(po.displayName(), "")
                        );
                    })
                    .orElse(
                        new Subscription.User(
                            Objects.requireNonNullElse(ref.getOwnerId(), ""),
                            Objects.requireNonNullElse(ref.getOwnerDisplayName(), "")
                        )
                    );
                subscription.setApiProduct(
                    new Subscription.ApiProduct(ref.getId(), ref.getName(), Objects.requireNonNullElse(ref.getVersion(), ""), owner)
                );
            });
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
