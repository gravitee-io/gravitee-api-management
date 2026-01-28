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

import static io.gravitee.rest.api.model.SubscriptionStatus.CLOSED;
import static io.gravitee.rest.api.model.SubscriptionStatus.PAUSED;
import static io.gravitee.rest.api.model.SubscriptionStatus.RESUMED;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.CloseSubscriptionUseCase;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.management.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
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
import java.sql.Date;
import java.time.ZoneId;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "API Subscriptions")
public class ApiSubscriptionResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private CloseSubscriptionUseCase closeSubscriptionUsecase;

    @Inject
    private AcceptSubscriptionUseCase acceptSubscriptionUsecase;

    @Inject
    private RejectSubscriptionUseCase rejectSubscriptionUsecase;

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @Inject
    private ParameterService parameterService;

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
        return convert(GraviteeContext.getExecutionContext(), checkSubscription(subscription));
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
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to process")
                .build();
        }

        var executionContext = GraviteeContext.getExecutionContext();
        var user = getAuthenticatedUserDetails();
        var auditInfo = AuditInfo.builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(AuditActor.builder().userId(user.getUsername()).userSource(user.getSource()).userSourceId(user.getSourceId()).build())
            .build();
        io.gravitee.apim.core.subscription.model.SubscriptionEntity result;

        if (processSubscriptionEntity.isAccepted()) {
            result = acceptSubscriptionUsecase
                .execute(
                    AcceptSubscriptionUseCase.Input.builder()
                        .referenceId(api)
                        .referenceType(SubscriptionReferenceType.API)
                        .subscriptionId(subscription)
                        .startingAt(
                            processSubscriptionEntity.getStartingAt() != null
                                ? processSubscriptionEntity.getStartingAt().toInstant().atZone(ZoneId.systemDefault())
                                : null
                        )
                        .endingAt(
                            processSubscriptionEntity.getEndingAt() != null
                                ? processSubscriptionEntity.getEndingAt().toInstant().atZone(ZoneId.systemDefault())
                                : null
                        )
                        .reasonMessage(processSubscriptionEntity.getReason())
                        .customKey(processSubscriptionEntity.getCustomApiKey())
                        .auditInfo(auditInfo)
                        .build()
                )
                .subscription();
        } else {
            result = rejectSubscriptionUsecase
                .execute(
                    RejectSubscriptionUseCase.Input.builder()
                        .referenceId(api)
                        .referenceType(SubscriptionReferenceType.API)
                        .subscriptionId(subscription)
                        .reasonMessage(processSubscriptionEntity.getReason())
                        .auditInfo(auditInfo)
                        .build()
                )
                .subscription();
        }

        return Response.ok(convert(executionContext, result)).build();
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
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to update")
                .build();
        }

        // Check subscription to update exists and belongs to API
        checkSubscription(subscription);

        // Force ID
        updateSubscriptionEntity.setId(subscription);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.update(executionContext, updateSubscriptionEntity);
        return Response.ok(convert(executionContext, subscriptionEntity)).build();
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
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        if (CLOSED.equals(subscriptionStatus)) {
            var result = closeSubscriptionUsecase.execute(
                CloseSubscriptionUseCase.Input.builder()
                    .subscriptionId(subscription)
                    .referenceId(api)
                    .referenceType(SubscriptionReferenceType.API)
                    .auditInfo(
                        AuditInfo.builder()
                            .organizationId(executionContext.getOrganizationId())
                            .environmentId(executionContext.getEnvironmentId())
                            .actor(
                                AuditActor.builder()
                                    .userId(user.getUsername())
                                    .userSource(user.getSource())
                                    .userSourceId(user.getSourceId())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );
            return Response.ok(convert(executionContext, result.subscription())).build();
        } else if (PAUSED.equals(subscriptionStatus)) {
            SubscriptionEntity updatedSubscriptionEntity = subscriptionService.pause(executionContext, subscription);
            return Response.ok(convert(executionContext, updatedSubscriptionEntity)).build();
        } else if (RESUMED.equals(subscriptionStatus)) {
            SubscriptionEntity updatedSubscriptionEntity = subscriptionService.resume(executionContext, subscription);
            return Response.ok(convert(executionContext, updatedSubscriptionEntity)).build();
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
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("'subscription' parameter does not correspond to the subscription to process")
                .build();
        }

        // Check subscription exists and belongs to API
        checkSubscription(subscription);

        // Force subscription ID
        transferSubscriptionEntity.setId(subscription);

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.transfer(
            executionContext,
            transferSubscriptionEntity,
            getAuthenticatedUser()
        );
        return Response.ok(convert(executionContext, subscriptionEntity)).build();
    }

    @Path("apikeys")
    public ApiSubscriptionApiKeysResource getApiSubscriptionApiKeysResourceResource() {
        return resourceContext.getResource(ApiSubscriptionApiKeysResource.class);
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
        subscription.setRequest(subscriptionEntity.getRequest());
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
        subscription.setClientId(subscriptionEntity.getClientId());
        subscription.setMetadata(subscriptionEntity.getMetadata());

        subscription.setPlan(fetchPlan(executionContext, subscriptionEntity.getPlan()));
        subscription.setApplication(fetchApplication(executionContext, subscriptionEntity.getApplication()));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());
        subscription.setConsumerPausedAt(subscriptionEntity.getConsumerPausedAt());

        return subscription;
    }

    private Subscription convert(
        final ExecutionContext executionContext,
        io.gravitee.apim.core.subscription.model.SubscriptionEntity subscriptionEntity
    ) {
        var userDisplayName = userService.findById(executionContext, subscriptionEntity.getSubscribedBy(), true).getDisplayName();
        var genericPlan = planSearchService.findById(executionContext, subscriptionEntity.getPlanId());
        var application = applicationService.findById(executionContext, subscriptionEntity.getApplicationId());

        return SubscriptionMapper.convert(subscriptionEntity, userDisplayName, genericPlan, application);
    }

    private Subscription.Plan fetchPlan(ExecutionContext executionContext, String planId) {
        GenericPlanEntity genericPlan = planSearchService.findById(executionContext, planId);

        var plan = new Subscription.Plan(genericPlan.getId(), genericPlan.getName());
        if (genericPlan.getPlanMode() == PlanMode.STANDARD) {
            plan.setSecurity(genericPlan.getPlanSecurity().getType());
        }

        return plan;
    }

    private Subscription.Application fetchApplication(ExecutionContext executionContext, String applicationId) {
        ApplicationEntity application = applicationService.findById(executionContext, applicationId);
        return new Subscription.Application(
            application.getId(),
            application.getName(),
            application.getType(),
            application.getDescription(),
            application.getDomain(),
            new Subscription.User(application.getPrimaryOwner().getId(), application.getPrimaryOwner().getDisplayName()),
            application.getApiKeyMode()
        );
    }

    private SubscriptionEntity checkSubscription(String subscription) {
        SubscriptionEntity searchedSubscription = subscriptionService.findById(subscription);
        if (api.equalsIgnoreCase(searchedSubscription.getApi())) {
            return searchedSubscription;
        }
        throw new SubscriptionNotFoundException(subscription);
    }
}
