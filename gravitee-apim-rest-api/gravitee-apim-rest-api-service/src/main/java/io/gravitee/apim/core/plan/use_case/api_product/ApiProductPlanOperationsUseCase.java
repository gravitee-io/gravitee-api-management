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

import io.gravitee.apim.core.UseCase;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApiProductPlanOperationsUseCase {

    private final PlanSearchService planSearchService;
    private final PlanService planService;
    private String DELETE = "DELETE";
    private String PUBLISH = "PUBLISH";
    private String CLOSE = "CLOSE";
    private String DEPRECATE = "DEPRECATE";

    public Output execute(Input input) {
        final GenericPlanEntity genericPlanEntity = planSearchService.findByPlanIdIdForApiProduct(
            GraviteeContext.getExecutionContext(),
            input.planId,
            input.apiProductId
        );
        if (
            genericPlanEntity.getReferenceType().equals(GenericPlanEntity.ReferenceType.API_PRODUCT) &&
            !genericPlanEntity.getReferenceId().equals(input.apiProductId)
        ) {
            throw new PlanNotFoundException(input.planId);
        }
        if (input.operation.equals(DELETE)) {
            planService.delete(GraviteeContext.getExecutionContext(), input.planId);
            return new Output(genericPlanEntity);
        } else if (input.operation.equals(CLOSE)) {
            return new Output(planService.closePlanForApiProduct(GraviteeContext.getExecutionContext(), input.planId));
        } else if (input.operation.equals(PUBLISH)) {
            return new Output(planService.publish(GraviteeContext.getExecutionContext(), input.planId));
        } else if (input.operation.equals(DEPRECATE)) {
            return new Output(planService.deprecate(GraviteeContext.getExecutionContext(), input.planId));
        } else {
            return null;
        }
    }

    @Builder
    public record Input(String planId, String apiProductId, String operation) {}

    public record Output(GenericPlanEntity planEntity) {}
}
