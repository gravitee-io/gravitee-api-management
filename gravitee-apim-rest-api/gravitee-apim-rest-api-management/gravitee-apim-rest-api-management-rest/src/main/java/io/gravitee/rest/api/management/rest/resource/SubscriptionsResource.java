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

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = { "Subscription" })
public class SubscriptionsResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List subscriptions for authenticated user")
    @ApiResponses(
        {
            @ApiResponse(code = 200, message = "List of subscriptions", response = Subscription.class, responseContainer = "Set"),
            @ApiResponse(code = 500, message = "Internal server error"),
        }
    )
    public Set<Subscription> getUserSubscriptions(@QueryParam("application") String application, @QueryParam("plan") String plan) {
        return subscriptionService.findByApplicationAndPlan(application, plan).stream().map(this::convert).collect(Collectors.toSet());
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
                new Subscription.User(application.getPrimaryOwner().getId(), application.getPrimaryOwner().getDisplayName())
            )
        );

        PlanEntity plan = planService.findById(subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());

        return subscription;
    }
}
