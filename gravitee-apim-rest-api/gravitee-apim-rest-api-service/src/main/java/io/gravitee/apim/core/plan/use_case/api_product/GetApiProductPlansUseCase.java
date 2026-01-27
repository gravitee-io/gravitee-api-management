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
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class GetApiProductPlansUseCase {

    private final PlanSearchService planSearchService;

    public Output execute(Input input) {
        if (input.planId == null) {
            Stream<GenericPlanEntity> plansStream = planSearchService
                .searchForApiProductPlans(GraviteeContext.getExecutionContext(), input.query, input.authenticatedUser, input.isAdmin)
                .stream()
                .sorted(comparingInt(GenericPlanEntity::getOrder));
            //TODO ask if need API_GATEWAY_DEFINITION and API_PRODUCT_PLAN filtersensitiveData is needed like api plans

            //TODO filter based on subscriptions

            List<GenericPlanEntity> plans = plansStream.toList();

            return Output.multiple(plans);
        } else {
            final GenericPlanEntity planEntity = planSearchService.findByPlanIdIdForApiProduct(
                GraviteeContext.getExecutionContext(),
                input.planId,
                input.apiProductId
            );

            return Output.single(Optional.of(planEntity));
        }
    }

    public record Input(String apiProductId, String authenticatedUser, boolean isAdmin, PlanQuery query, String planId) {
        public static Input of(String apiProductId, String authenticatedUser, boolean isAdmin, PlanQuery query) {
            return new Input(apiProductId, authenticatedUser, isAdmin, query, null);
        }

        public static Input of(String apiProductId, String planId) {
            return new Input(apiProductId, null, false, null, planId);
        }
    }

    public record Output(List<GenericPlanEntity> apiProductPlans, Optional<GenericPlanEntity> apiProductPlan) {
        public static GetApiProductPlansUseCase.Output multiple(List<GenericPlanEntity> apiProductPlans) {
            return new GetApiProductPlansUseCase.Output(apiProductPlans, Optional.empty());
        }

        public static GetApiProductPlansUseCase.Output single(Optional<GenericPlanEntity> apiProductPlan) {
            return new GetApiProductPlansUseCase.Output(null, apiProductPlan);
        }
    }
}
