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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.*;
import io.gravitee.management.model.log.SearchLogResponse;
import io.gravitee.management.model.permissions.RolePermission;
import io.gravitee.management.model.permissions.RolePermissionAction;
import io.gravitee.management.model.subscription.SubscriptionQuery;
import io.gravitee.management.rest.model.Pageable;
import io.gravitee.management.rest.model.PagedResult;
import io.gravitee.management.rest.model.Subscription;
import io.gravitee.management.rest.resource.param.ListStringParam;
import io.gravitee.management.rest.resource.param.ListSubscriptionStatusParam;
import io.gravitee.management.rest.resource.param.LogsParam;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.SubscriptionService;
import io.gravitee.management.service.UserService;
import io.swagger.annotations.*;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Date;

import static java.lang.String.format;

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
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private UserService userService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for the API",
            notes = "User must have the READ_SUBSCRIPTION permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Paged result of API's subscriptions", response = PagedResult.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.READ)
    })
    public PagedResult<SubscriptionEntity> listApiSubscriptions(
            @BeanParam SubscriptionParam subscriptionParam,
            @Valid @BeanParam Pageable pageable) {
        // Transform query parameters to a subscription query
        SubscriptionQuery subscriptionQuery = subscriptionParam.toQuery();

        Page<SubscriptionEntity> subscriptions = subscriptionService
                .search(subscriptionQuery, pageable.toPageable());
        PagedResult<SubscriptionEntity> result = new PagedResult<>(subscriptions, pageable.getSize());
        result.setMetadata(subscriptionService.getMetadata(subscriptions.getContent()).getMetadata());
        return result;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Subscribe to a plan",
            notes = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponses({
            @ApiResponse(code = 201, message = "Subscription successfully created", response = Subscription.class),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({
            @Permission(value = RolePermission.API_SUBSCRIPTION, acls = RolePermissionAction.CREATE)
    })
    public Response createSubscription(
            @PathParam("api") String api,
            @ApiParam(name = "application", required = true)
            @NotNull @QueryParam("application") String application,
            @ApiParam(name = "plan", required = true)
            @NotNull @QueryParam("plan") String plan) {
        // Create subscription
        SubscriptionEntity subscription = subscriptionService.create(new NewSubscriptionEntity(plan, application));

        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            ProcessSubscriptionEntity process = new ProcessSubscriptionEntity();
            process.setId(subscription.getId());
            process.setAccepted(true);
            process.setStartingAt(new Date());
            subscription = subscriptionService.process(process, getAuthenticatedUser());
        }

        return Response
                .created(URI.create("/apis/" + api + "/subscriptions/" + subscription.getId()))
                .entity(convert(subscription))
                .build();
    }

    @GET
    @Path("export")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Export API logs as CSV")
    @ApiResponses({
            @ApiResponse(code = 200, message = "API logs as CSV"),
            @ApiResponse(code = 500, message = "Internal server error")})
    @Permissions({@Permission(value = RolePermission.API_LOG, acls = RolePermissionAction.READ)})
    public Response exportAPILogsAsCSV(
            @PathParam("api") String api,
            @BeanParam SubscriptionParam subscriptionParam,
            @Valid @BeanParam Pageable pageable) {
        final PagedResult<SubscriptionEntity> subscriptions = listApiSubscriptions(subscriptionParam, pageable);
        return Response
                .ok(subscriptionService.exportAsCsv(subscriptions.getData(), subscriptions.getMetadata()))
                .header(HttpHeaders.CONTENT_DISPOSITION, format("attachment;filename=subscriptions-%s-%s.csv", api, System.currentTimeMillis()))
                .build();
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
                new Subscription.User(subscriptionEntity.getSubscribedBy(),
                        userService.findById(subscriptionEntity.getSubscribedBy()).getDisplayName()
                ));

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        ApplicationEntity application = applicationService.findById(subscriptionEntity.getApplication());
        subscription.setApplication(
                new Subscription.Application(
                        application.getId(),
                        application.getName(),
                        //application.getType(),
                        null,
                        new Subscription.User(
                                application.getPrimaryOwner().getId(),
                                application.getPrimaryOwner().getDisplayName()
                        )
                ));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());

        return subscription;
    }

    private static class SubscriptionParam {
        @PathParam("api")
        private String api;

        @QueryParam("plan")
        @ApiParam(value = "plan", required = true)
        private ListStringParam plans;

        @QueryParam("application")
        @ApiParam(value = "application", required = true)
        private ListStringParam applications;

        @QueryParam("status")
        @DefaultValue("accepted,pending,paused")
        @ApiModelProperty(dataType = "string", allowableValues = "accepted, pending, rejected, closed", value = "Subscription status")
        private ListSubscriptionStatusParam status;

        @QueryParam("api_key")
        private String apiKey;

        public String getApi() {
            return api;
        }

        public void setApi(String api) {
            this.api = api;
        }

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

            query.setApi(this.api);

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
