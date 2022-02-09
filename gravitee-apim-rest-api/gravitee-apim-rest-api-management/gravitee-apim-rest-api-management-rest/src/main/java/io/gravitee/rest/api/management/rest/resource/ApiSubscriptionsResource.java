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

import static java.lang.String.format;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Pageable;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.management.rest.resource.param.ListStringParam;
import io.gravitee.rest.api.management.rest.resource.param.ListSubscriptionStatusParam;
import io.gravitee.rest.api.management.rest.security.Permission;
import io.gravitee.rest.api.management.rest.security.Permissions;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionMetadataQuery;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.validator.CustomApiKey;
import io.swagger.annotations.*;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "API Subscriptions" })
public class ApiSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private ParameterService parameterService;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("api")
    @ApiParam(name = "api", hidden = true)
    private String api;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for the API", notes = "User must have the READ_SUBSCRIPTION permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "Paged result of API's subscriptions", response = PagedResult.class),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public PagedResult<SubscriptionEntity> getApiSubscriptions(
        @BeanParam SubscriptionParam subscriptionParam,
        @Valid @BeanParam Pageable pageable,
        @ApiParam(allowableValues = "keys, security", value = "Expansion of data to return in subscriptions") @QueryParam(
            "expand"
        ) List<String> expand
    ) {
        // Transform query parameters to a subscription query
        SubscriptionQuery subscriptionQuery = subscriptionParam.toQuery();
        subscriptionQuery.setApi(api);

        boolean expandApiKeys = expand != null && expand.contains("keys");
        boolean expandPlanSecurity = expand != null && expand.contains("security");
        Page<SubscriptionEntity> subscriptions = subscriptionService.search(
            subscriptionQuery,
            pageable.toPageable(),
            expandApiKeys,
            expandPlanSecurity
        );

        PagedResult<SubscriptionEntity> result = new PagedResult<>(subscriptions, pageable.getSize());
        SubscriptionMetadataQuery subscriptionMetadataQuery = new SubscriptionMetadataQuery(
            GraviteeContext.getCurrentOrganization(),
            GraviteeContext.getCurrentEnvironment(),
            subscriptions.getContent()
        )
            .withApis(true)
            .withApplications(true)
            .withPlans(true);
        result.setMetadata(subscriptionService.getMetadata(subscriptionMetadataQuery).toMap());
        return result;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Subscribe to a plan", notes = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponses(
        {
            @ApiResponse(code = 201, message = "Subscription successfully created", response = Subscription.class),
            @ApiResponse(code = 400, message = "Bad custom API Key format or custom API Key definition disabled"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response createSubscriptionToApi(
        @ApiParam(name = "application", required = true) @NotNull @QueryParam("application") String application,
        @ApiParam(name = "plan", required = true) @NotNull @QueryParam("plan") String plan,
        @ApiParam(name = "customApiKey") @CustomApiKey @QueryParam("customApiKey") String customApiKey
    ) {
        if (
            StringUtils.isNotEmpty(customApiKey) &&
            !parameterService.findAsBoolean(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)
        ) {
            return Response.status(Response.Status.BAD_REQUEST).entity("You are not allowed to provide a custom API Key").build();
        }

        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity(plan, application);

        // Create subscription
        SubscriptionEntity subscription = subscriptionService.create(newSubscriptionEntity, customApiKey);

        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            ProcessSubscriptionEntity process = new ProcessSubscriptionEntity();
            process.setId(subscription.getId());
            process.setAccepted(true);
            process.setStartingAt(new Date());
            process.setCustomApiKey(customApiKey);
            subscription = subscriptionService.process(process, getAuthenticatedUser());
        }

        return Response
            .created(
                this.getRequestUriBuilder()
                    .path(subscription.getId())
                    .replaceQueryParam("application", null)
                    .replaceQueryParam("plan", null)
                    .build()
            )
            .entity(convert(subscription))
            .build();
    }

    @GET
    @Path("export")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Export API logs as CSV")
    @ApiResponses({ @ApiResponse(code = 200, message = "API logs as CSV"), @ApiResponse(code = 500, message = "Internal server error") })
    @Permissions({ @Permission(value = RolePermission.API_LOG, acls = RolePermissionAction.READ) })
    public Response exportApiSubscriptionsLogsAsCSV(@BeanParam SubscriptionParam subscriptionParam, @Valid @BeanParam Pageable pageable) {
        final PagedResult<SubscriptionEntity> subscriptions = getApiSubscriptions(subscriptionParam, pageable, null);
        return Response
            .ok(subscriptionService.exportAsCsv(subscriptions.getData(), subscriptions.getMetadata()))
            .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=subscriptions-%s-%s.csv", api, System.currentTimeMillis()))
            .build();
    }

    @GET
    @Path("_canCreate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        value = "Check a subscription can be created with given api key, and application",
        notes = "User must have the API_SUBSCRIPTION:READ permission to use this service"
    )
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "API Key creation successfully checked", response = Boolean.class),
            @ApiResponse(code = 400, message = "Bad API Key parameter"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    @Permissions({ @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Response verifyApiKeyCreation(
        @ApiParam(name = "key", required = true) @CustomApiKey @NotNull @QueryParam("key") String key,
        @ApiParam(name = "application", required = true) @NotNull @QueryParam("application") String application
    ) {
        boolean canCreate = apiKeyService.canCreate(key, api, application);
        return Response.ok(canCreate).build();
    }

    @Path("{subscription}")
    public ApiSubscriptionResource getApiSubscriptionResource() {
        return resourceContext.getResource(ApiSubscriptionResource.class);
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
        subscription.setSubscribedBy(
            new Subscription.User(
                subscriptionEntity.getSubscribedBy(),
                userService.findById(subscriptionEntity.getSubscribedBy()).getDisplayName()
            )
        );

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        ApplicationEntity application = applicationService.findById(
            GraviteeContext.getCurrentEnvironment(),
            subscriptionEntity.getApplication()
        );
        subscription.setApplication(
            new Subscription.Application(
                application.getId(),
                application.getName(),
                application.getType(),
                application.getDescription(),
                application.getDomain(),
                new Subscription.User(application.getPrimaryOwner().getId(), application.getPrimaryOwner().getDisplayName())
            )
        );

        subscription.setClosedAt(subscriptionEntity.getClosedAt());

        return subscription;
    }

    private static class SubscriptionParam {

        @QueryParam("plan")
        @ApiParam(value = "plan")
        private ListStringParam plans;

        @QueryParam("application")
        @ApiParam(value = "application")
        private ListStringParam applications;

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

        public ListStringParam getApplications() {
            return applications;
        }

        public void setApplications(ListStringParam applications) {
            this.applications = applications;
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

            if (plans != null && plans.getValue() != null) {
                query.setPlans(plans.getValue());
            }

            if (status != null) {
                query.setStatuses(status.getStatus());
            }

            if (applications != null && applications.getValue() != null) {
                query.setApplications(applications.getValue());
            }

            query.setApiKey(this.apiKey);

            return query;
        }
    }
}
