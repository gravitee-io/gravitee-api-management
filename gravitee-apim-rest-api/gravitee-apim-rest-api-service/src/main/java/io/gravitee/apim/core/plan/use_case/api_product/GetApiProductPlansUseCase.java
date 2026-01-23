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
package io.gravitee.apim.core.plan.use_case.api_product;

import static java.util.Comparator.comparingInt;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.ApiProductPlanSearchQueryService;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetApiProductPlansUseCase {

    private final ApiProductPlanSearchQueryService apiProductPlanSearchQueryService;

    public Output execute(Input input) {
        log.debug("Getting API Product plans for API Product {} (planId={})", input.apiProductId, input.planId);
        if (input.planId == null) {
            log.debug(
                "Searching API Product plans for API Product {} (user={}, isAdmin={}, query={})",
                input.apiProductId,
                input.authenticatedUser,
                input.isAdmin,
                input.query
            );
            Stream<Plan> plansStream = apiProductPlanSearchQueryService
                .searchForApiProductPlans(input.apiProductId, input.query, input.authenticatedUser, input.isAdmin)
                .stream()
                .sorted(comparingInt(Plan::getOrder));
            //TODO ask if need API_GATEWAY_DEFINITION and API_PRODUCT_PLAN filtersensitiveData is needed like api plans

            //TODO filter based on subscriptions

            List<Plan> plans = plansStream.toList();
            log.debug("Found {} API Product plans for API Product {}", plans.size(), input.apiProductId);
            return Output.multiple(plans);
        } else {
            log.debug("Getting API Product plan {} for API Product {}", input.planId, input.apiProductId);
            final Plan plan = apiProductPlanSearchQueryService.findByPlanIdIdForApiProduct(input.planId, input.apiProductId);

            return Output.single(Optional.of(plan));
        }
    }

    public record Input(String apiProductId, String authenticatedUser, boolean isAdmin, PlanQuery query, String planId) {
        public static Input of(String apiProductId, String authenticatedUser, boolean isAdmin, PlanQuery query) {
            log.debug(
                "Building GetApiProductPlansUseCase.Input (apiProductId={}, user={}, isAdmin={}, query={})",
                apiProductId,
                authenticatedUser,
                isAdmin,
                query
            );
            return new Input(apiProductId, authenticatedUser, isAdmin, query, null);
        }

        public static Input of(String apiProductId, String planId) {
            log.debug("Building GetApiProductPlansUseCase.Input (apiProductId={}, planId={})", apiProductId, planId);
            return new Input(apiProductId, null, false, null, planId);
        }
    }

    public record Output(List<Plan> apiProductPlans, Optional<Plan> apiProductPlan) {
        public static GetApiProductPlansUseCase.Output multiple(List<Plan> apiProductPlans) {
            log.debug("Building GetApiProductPlansUseCase.Output (multiple)", apiProductPlans);
            return new GetApiProductPlansUseCase.Output(apiProductPlans, Optional.empty());
        }

        public static GetApiProductPlansUseCase.Output single(Optional<Plan> apiProductPlan) {
            log.debug("Building GetApiProductPlansUseCase.Output (single)", apiProductPlan);
            return new GetApiProductPlansUseCase.Output(null, apiProductPlan);
        }
    }
}
