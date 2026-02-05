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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class GetSubscriptionsUseCase {

    private final SubscriptionQueryService subscriptionQueryService;
    private final ApiCrudService apiCrudService;
    private final ApiProductCrudService apiProductCrudService;
    private final PlanCrudService planCrudService;

    public Output execute(Input input) {
        log.debug("Getting subscriptions for {} {} (subscriptionId: {})", input.referenceType, input.referenceId, input.subscriptionId);

        verifyReferenceExists(input.referenceId, input.referenceType);

        if (input.subscriptionId == null) {
            List<SubscriptionEntity> subscriptions = subscriptionQueryService.findAllByReferenceIdAndReferenceType(
                input.referenceId,
                input.referenceType
            );

            Set<String> planIds = subscriptions.stream().map(SubscriptionEntity::getPlanId).collect(Collectors.toSet());
            if (!planIds.isEmpty()) {
                Map<String, Plan> plansById = planCrudService
                    .findByIds(new ArrayList<>(planIds))
                    .stream()
                    .collect(Collectors.toMap(Plan::getId, plan -> plan));

                subscriptions = subscriptions
                    .stream()
                    .filter(sub -> {
                        Plan plan = plansById.get(sub.getPlanId());
                        return plan != null && plan.getPlanStatus() != PlanStatus.CLOSED && plan.getPlanStatus() != PlanStatus.DEPRECATED;
                    })
                    .toList();
            }

            log.debug("Found {} subscriptions for {} {}", subscriptions.size(), input.referenceType, input.referenceId);
            return Output.multiple(subscriptions);
        } else {
            Optional<SubscriptionEntity> subscription = subscriptionQueryService.findByIdAndReferenceIdAndReferenceType(
                input.subscriptionId,
                input.referenceId,
                input.referenceType
            );
            log.debug(
                "Subscription {} {} for {} {}",
                input.subscriptionId,
                subscription.isPresent() ? "found" : "not found",
                input.referenceType,
                input.referenceId
            );
            return Output.single(subscription);
        }
    }

    private void verifyReferenceExists(String referenceId, SubscriptionReferenceType referenceType) {
        if (referenceType == SubscriptionReferenceType.API) {
            apiCrudService.get(referenceId);
        } else if (referenceType == SubscriptionReferenceType.API_PRODUCT) {
            apiProductCrudService.get(referenceId);
        }
    }

    public record Input(String referenceId, SubscriptionReferenceType referenceType, String subscriptionId) {
        public static Input of(String referenceId, SubscriptionReferenceType referenceType) {
            return new Input(referenceId, referenceType, null);
        }

        public static Input of(String referenceId, SubscriptionReferenceType referenceType, String subscriptionId) {
            return new Input(referenceId, referenceType, subscriptionId);
        }
    }

    public record Output(List<SubscriptionEntity> subscriptions, Optional<SubscriptionEntity> subscription) {
        public static Output multiple(List<SubscriptionEntity> subscriptions) {
            return new Output(subscriptions, Optional.empty());
        }

        public static Output single(Optional<SubscriptionEntity> subscription) {
            return new Output(null, subscription);
        }
    }
}
