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
package io.gravitee.apim.core.plan.use_case;

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanSearchQueryService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class GetPlansUseCase {

    private final PlanSearchQueryService planSearchQueryService;
    private final SubscriptionQueryService subscriptionQueryService;

    public Output execute(Input input) {
        log.debug("Getting plans for reference {} (planId={})", input.referenceId, input.planId);
        if (input.planId == null) {
            log.debug(
                "Searching plans for reference {} (user={}, isAdmin={}, query={})",
                input.referenceId,
                input.authenticatedUser,
                input.isAdmin,
                input.query
            );
            Stream<Plan> plansStream = planSearchQueryService
                .searchPlans(input.referenceId, input.referenceType, input.query, input.authenticatedUser, input.isAdmin)
                .stream()
                .sorted(comparingInt(Plan::getOrder));
            //TODO ask if need API_GATEWAY_DEFINITION and API_PRODUCT_PLAN filtersensitiveData is needed like api plans

            if (input.subscribableBy != null) {
                var subscriptions = subscriptionQueryService.findActiveByApplicationIdAndReferenceIdAndReferenceType(
                    input.subscribableBy,
                    input.referenceId,
                    SubscriptionReferenceType.valueOf(input.referenceType.name())
                );
                var subscribedPlans = subscriptions.stream().map(SubscriptionEntity::getPlanId).toList();

                plansStream = plansStream.filter(
                    plan ->
                        (plan.getPlanSecurity() == null ||
                            !List.of(PlanSecurityType.KEY_LESS.getLabel(), PlanSecurityType.KEY_LESS.name()).contains(
                                plan.getPlanSecurity().getType()
                            )) &&
                        (subscribedPlans.isEmpty() || !subscribedPlans.contains(plan.getId()))
                );
            }

            List<Plan> plans = plansStream.toList();
            log.debug("Found {} plans for reference {}", plans.size(), input.referenceId);
            return Output.multiple(plans);
        } else {
            log.debug("Getting plan {} for reference {}", input.planId, input.referenceId);
            final Plan plan = planSearchQueryService.findByPlanIdAndReferenceIdAndReferenceType(
                input.planId,
                input.referenceId,
                input.referenceType
            );

            return Output.single(Optional.of(plan));
        }
    }

    public record Input(
        String referenceId,
        String authenticatedUser,
        boolean isAdmin,
        PlanQuery query,
        String planId,
        String subscribableBy,
        GenericPlanEntity.ReferenceType referenceType
    ) {
        public static Input of(
            String referenceId,
            GenericPlanEntity.ReferenceType referenceType,
            String authenticatedUser,
            boolean isAdmin,
            PlanQuery query,
            String subscribableBy
        ) {
            return new Input(referenceId, authenticatedUser, isAdmin, query, null, subscribableBy, referenceType);
        }

        public static Input of(String referenceId, String planId, GenericPlanEntity.ReferenceType referenceType) {
            return new Input(referenceId, null, false, null, planId, null, referenceType);
        }
    }

    public record Output(List<Plan> plans, Optional<Plan> plan) {
        public static GetPlansUseCase.Output multiple(List<Plan> plans) {
            return new GetPlansUseCase.Output(plans, Optional.empty());
        }

        public static GetPlansUseCase.Output single(Optional<Plan> plan) {
            return new GetPlansUseCase.Output(null, plan);
        }
    }
}
