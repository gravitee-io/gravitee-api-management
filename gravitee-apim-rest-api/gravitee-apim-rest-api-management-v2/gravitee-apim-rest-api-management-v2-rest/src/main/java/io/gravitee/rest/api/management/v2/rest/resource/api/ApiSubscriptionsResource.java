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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ApplicationMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PageMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.PlanMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.SubscriptionMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.management.v2.rest.security.Permission;
import io.gravitee.rest.api.management.v2.rest.security.Permissions;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/environments/{envId}/apis/{apiId}/subscriptions")
public class ApiSubscriptionsResource extends AbstractResource {

    private final SubscriptionMapper subscriptionMapper = SubscriptionMapper.INSTANCE;
    private final PageMapper pageMapper = PageMapper.INSTANCE;
    private final PlanMapper planMapper = PlanMapper.INSTANCE;
    private final ApplicationMapper applicationMapper = ApplicationMapper.INSTANCE;

    private static final String EXPAND_PLAN = "plan";
    private static final String EXPAND_APPLICATION = "application";

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private PlanSearchService planSearchService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ParameterService parameterService;

    @Inject
    private ApiKeyService apiKeyService;

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public SubscriptionsResponse getApiSubscriptions(
        @QueryParam("applicationIds") Set<String> applicationIds,
        @QueryParam("planIds") Set<String> planIds,
        @QueryParam("statuses") @DefaultValue("ACCEPTED") Set<SubscriptionStatus> statuses,
        @QueryParam("apiKey") String apiKey,
        @QueryParam("expands") Set<String> expands,
        @BeanParam @Valid PaginationParam paginationParam
    ) {
        final SubscriptionQuery subscriptionQuery = SubscriptionQuery
            .builder()
            .apis(List.of(apiId))
            .applications(applicationIds)
            .plans(planIds)
            .statuses(subscriptionMapper.mapToStatusSet(statuses))
            .apiKey(apiKey)
            .build();

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final Page<SubscriptionEntity> subscriptionPage = subscriptionService.search(
            executionContext,
            subscriptionQuery,
            paginationParam.toPageable(),
            false,
            false
        );

        final List<Subscription> subscriptions = subscriptionMapper.mapToList(subscriptionPage.getContent());
        expandData(subscriptions, expands);

        return new SubscriptionsResponse()
            .data(subscriptions)
            .pagination(
                computePaginationInfo(
                    Math.toIntExact(subscriptionPage.getTotalElements()),
                    Math.toIntExact(subscriptionPage.getPageElements()),
                    paginationParam
                )
            )
            .links(computePaginationLinks(Math.toIntExact(subscriptionPage.getTotalElements()), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.CREATE }) })
    public Response createApiSubscription(@Valid @NotNull CreateSubscription createSubscription) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (
            StringUtils.isNotEmpty(createSubscription.getCustomApiKey()) &&
            !parameterService.findAsBoolean(executionContext, Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(subscriptionInvalid("You are not allowed to provide a custom API Key"))
                .build();
        }

        final NewSubscriptionEntity newSubscriptionEntity = subscriptionMapper.map(createSubscription);
        SubscriptionEntity subscription = subscriptionService.create(
            executionContext,
            newSubscriptionEntity,
            createSubscription.getCustomApiKey()
        );

        if (subscription.getStatus() == io.gravitee.rest.api.model.SubscriptionStatus.PENDING) {
            ProcessSubscriptionEntity process = new ProcessSubscriptionEntity();
            process.setId(subscription.getId());
            process.setAccepted(true);
            process.setStartingAt(new Date());
            process.setCustomApiKey(createSubscription.getCustomApiKey());
            subscription = subscriptionService.process(executionContext, process, getAuthenticatedUser());
        }

        return Response.created(this.getLocationHeader(subscription.getId())).entity(subscriptionMapper.map(subscription)).build();
    }

    @POST
    @Path("_verify")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response verifyCreateApiSubscription(@Valid @NotNull VerifySubscription verifySubscriptionSubscription) {
        boolean canCreate = apiKeyService.canCreate(
            GraviteeContext.getExecutionContext(),
            verifySubscriptionSubscription.getApiKey(),
            apiId,
            verifySubscriptionSubscription.getApplicationId()
        );
        return Response.ok(VerifySubscriptionResponse.builder().ok(canCreate).build()).build();
    }

    @GET
    @Path("/{subscriptionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.READ }) })
    public Response getApiSubscription(@PathParam("subscriptionId") String subscriptionId, @QueryParam("expands") Set<String> expands) {
        final SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final Subscription subscription = subscriptionMapper.map(subscriptionEntity);
        expandData(subscription, expands);

        return Response.ok(subscription).build();
    }

    @PUT
    @Path("/{subscriptionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = { RolePermissionAction.UPDATE }) })
    public Response updateApiPlan(
        @PathParam("subscriptionId") String subscriptionId,
        @Valid @NotNull UpdateSubscription updateSubscription
    ) {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        SubscriptionEntity subscriptionEntity = subscriptionService.findById(subscriptionId);

        if (!subscriptionEntity.getApi().equals(apiId)) {
            return Response.status(Response.Status.NOT_FOUND).entity(subscriptionNotFoundError(subscriptionId)).build();
        }

        final UpdateSubscriptionEntity updateSubscriptionEntity = subscriptionMapper.map(updateSubscription, subscriptionId);
        return Response
            .status(Response.Status.OK)
            .entity(subscriptionMapper.map(subscriptionService.update(executionContext, updateSubscriptionEntity)))
            .build();
    }

    private void expandData(Subscription subscription, Set<String> expands) {
        expandData(List.of(subscription), expands);
    }

    private void expandData(List<Subscription> subscriptions, Set<String> expands) {
        if (expands == null || expands.isEmpty()) {
            return;
        }

        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();

        if (expands.contains(EXPAND_PLAN)) {
            final Set<String> planIds = subscriptions
                .stream()
                .map(subscription -> (subscription.getPlan()).getId())
                .collect(Collectors.toSet());
            final Collection<BasePlan> plans = planMapper.mapToBasePlans(planSearchService.findByIdIn(executionContext, planIds));
            plans.forEach(plan ->
                subscriptions
                    .stream()
                    .filter(subscription -> subscription.getPlan().getId().equals(plan.getId()))
                    .forEach(subscription -> subscription.setPlan(plan))
            );
        }

        if (expands.contains(EXPAND_APPLICATION)) {
            final Set<String> applicationIds = subscriptions
                .stream()
                .map(subscription -> (subscription.getApplication()).getId())
                .collect(Collectors.toSet());
            final Collection<BaseApplication> applications = applicationMapper.mapToBaseApplicationList(
                applicationService.findByIds(executionContext, applicationIds)
            );
            applications.forEach(application ->
                subscriptions
                    .stream()
                    .filter(subscription -> subscription.getApplication().getId().equals(application.getId()))
                    .forEach(subscription -> subscription.setApplication(application))
            );
        }
    }

    private Error subscriptionNotFoundError(String subscriptionId) {
        return new Error()
            .httpStatus(Response.Status.NOT_FOUND.getStatusCode())
            .message("Subscription [" + subscriptionId + "] cannot be found.")
            .putParametersItem("subscription", subscriptionId)
            .technicalCode("subscription.notFound");
    }

    private Error subscriptionInvalid(String message) {
        return new Error().httpStatus(Response.Status.BAD_REQUEST.getStatusCode()).message(message).technicalCode("subscription.invalid");
    }
}
