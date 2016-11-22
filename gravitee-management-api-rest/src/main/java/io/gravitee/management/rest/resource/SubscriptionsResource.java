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
import io.gravitee.management.model.ApplicationEntity;
import io.gravitee.management.model.PlanEntity;
import io.gravitee.management.model.SubscriptionEntity;
import io.gravitee.management.rest.model.Subscription;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.ApplicationService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.SubscriptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Path("/subscriptions")
@Api(tags = {"Subscription"})
public class SubscriptionsResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @Inject
    private ApiService apiService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for authenticated user")
    @ApiResponses({
            @ApiResponse(code = 200, message = "List of subscriptions", response = Subscription.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error")})
    public Set<Subscription> listUserSubscriptions(
            @QueryParam("application") String application,
            @QueryParam("plan") String plan) {
        return subscriptionService.findByApplicationAndPlan(application, plan)
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
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

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        subscription.getPlan().setApis(plan.getApis().stream().map(api -> {
            io.gravitee.management.model.ApiEntity apiEntity = apiService.findById(api);
            return new Subscription.Api(apiEntity.getId(), apiEntity.getName());
        }).collect(Collectors.toList()));

        return subscription;
    }
}
