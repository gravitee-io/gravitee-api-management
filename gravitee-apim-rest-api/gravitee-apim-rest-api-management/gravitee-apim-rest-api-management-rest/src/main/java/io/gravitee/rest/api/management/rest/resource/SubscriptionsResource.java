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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Subscription")
public class SubscriptionsResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private PlanService planService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List subscriptions for authenticated user")
    @ApiResponse(
        responseCode = "200",
        description = "List of subscriptions",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = Subscription.class), uniqueItems = true)
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public Set<Subscription> getUserSubscriptions(@QueryParam("application") String application, @QueryParam("plan") String plan) {
        return subscriptionService
            .findByApplicationAndPlan(GraviteeContext.getExecutionContext(), application, plan)
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

        ApplicationEntity application = applicationService.findById(
            GraviteeContext.getExecutionContext(),
            subscriptionEntity.getApplication()
        );
        subscription.setApplication(
            new Subscription.Application(
                application.getId(),
                application.getName(),
                application.getType(),
                application.getDescription(),
                application.getDomain(),
                new Subscription.User(application.getPrimaryOwner().getId(), application.getPrimaryOwner().getDisplayName()),
                application.getApiKeyMode()
            )
        );

        PlanEntity plan = planService.findById(GraviteeContext.getExecutionContext(), subscriptionEntity.getPlan());
        subscription.setPlan(new Subscription.Plan(plan.getId(), plan.getName()));

        subscription.setClosedAt(subscriptionEntity.getClosedAt());

        return subscription;
    }
}
