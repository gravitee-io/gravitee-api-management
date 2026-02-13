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
package io.gravitee.rest.api.management.rest.mapper;

import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.sql.Date;

public class SubscriptionMapper {

    public static Subscription convert(
        io.gravitee.apim.core.subscription.model.SubscriptionEntity subscriptionEntity,
        String userDisplayName,
        GenericPlanEntity plan,
        ApplicationEntity application
    ) {
        Subscription subscription = new Subscription();

        subscription.setId(subscriptionEntity.getId());
        subscription.setCreatedAt(Date.from(subscriptionEntity.getCreatedAt().toInstant()));
        subscription.setUpdatedAt(Date.from(subscriptionEntity.getUpdatedAt().toInstant()));
        subscription.setStartingAt(
            subscriptionEntity.getStartingAt() != null ? Date.from(subscriptionEntity.getStartingAt().toInstant()) : null
        );
        subscription.setEndingAt(subscriptionEntity.getEndingAt() != null ? Date.from(subscriptionEntity.getEndingAt().toInstant()) : null);
        subscription.setClosedAt(subscriptionEntity.getClosedAt() != null ? Date.from(subscriptionEntity.getClosedAt().toInstant()) : null);
        subscription.setPausedAt(subscriptionEntity.getPausedAt() != null ? Date.from(subscriptionEntity.getPausedAt().toInstant()) : null);
        subscription.setConsumerPausedAt(
            subscriptionEntity.getConsumerPausedAt() != null ? Date.from(subscriptionEntity.getConsumerPausedAt().toInstant()) : null
        );
        subscription.setProcessedAt(
            subscriptionEntity.getProcessedAt() != null ? Date.from(subscriptionEntity.getProcessedAt().toInstant()) : null
        );
        subscription.setProcessedBy(subscriptionEntity.getProcessedBy());
        subscription.setRequest(subscriptionEntity.getRequestMessage());
        subscription.setReason(subscriptionEntity.getReasonMessage());
        subscription.setStatus(
            switch (subscriptionEntity.getStatus()) {
                case PENDING -> SubscriptionStatus.PENDING;
                case REJECTED -> SubscriptionStatus.REJECTED;
                case ACCEPTED -> SubscriptionStatus.ACCEPTED;
                case CLOSED -> SubscriptionStatus.CLOSED;
                case PAUSED -> SubscriptionStatus.PAUSED;
            }
        );
        subscription.setConsumerStatus(
            switch (subscriptionEntity.getConsumerStatus()) {
                case STARTED -> SubscriptionConsumerStatus.STARTED;
                case STOPPED -> SubscriptionConsumerStatus.STOPPED;
                case FAILURE -> SubscriptionConsumerStatus.FAILURE;
            }
        );
        subscription.setSubscribedBy(new Subscription.User(subscriptionEntity.getSubscribedBy(), userDisplayName));
        subscription.setClientId(subscriptionEntity.getClientId());
        subscription.setMetadata(subscriptionEntity.getMetadata());

        subscription.setPlan(convertPlan(plan));
        subscription.setApplication(convertApplication(application));
        subscription.setReferenceId(subscriptionEntity.getReferenceId());
        subscription.setReferenceType(subscriptionEntity.getReferenceType().name());

        return subscription;
    }

    private static Subscription.Plan convertPlan(GenericPlanEntity genericPlan) {
        var plan = new Subscription.Plan(genericPlan.getId(), genericPlan.getName());
        if (genericPlan.getPlanMode() == PlanMode.STANDARD) {
            plan.setSecurity(genericPlan.getPlanSecurity().getType());
        }

        return plan;
    }

    private static Subscription.Application convertApplication(ApplicationEntity application) {
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
}
