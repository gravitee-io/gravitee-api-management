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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.flow.domain_service.FlowValidationDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Replaces the flows, and optionally the name, of plans of one API Product, leaving every other field as stored.
 *
 * <p>{@link UpdateApiProductPlanUseCase} replaces the whole plan from its input, so a caller that only owns a plan's
 * flows would have to echo back every other field and would silently reset any field it does not know about.
 *
 * <p>Plans that are missing from the product, listed twice, without an HTTP v4 definition or carrying invalid flows are
 * all refused before any plan is written. The plan checks made by {@link UpdatePlanDomainService} on each write
 * (security configuration, tags, general conditions) read fields this use case keeps as stored, so they can only fail
 * if the stored plan was already invalid; a batch is not rolled back in that case.
 */
@UseCase
@RequiredArgsConstructor
@CustomLog
public class UpdateApiProductPlanFlowsUseCase {

    private final UpdatePlanDomainService updatePlanDomainService;
    private final FlowValidationDomainService flowValidationDomainService;
    private final PlanQueryService planQueryService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        rejectPlansListedTwice(input.plans());
        var apiProduct = apiProductCrudService.get(input.apiProductId());
        var productPlans = planQueryService
            .findAllByReferenceIdAndReferenceType(input.apiProductId(), GenericPlanEntity.ReferenceType.API_PRODUCT)
            .stream()
            .collect(toMap(Plan::getId, identity()));

        // Every plan is resolved and its flows validated before any is written, so a refused plan leaves the others untouched.
        var toWrite = input
            .plans()
            .stream()
            .map(update ->
                Optional.ofNullable(productPlans.get(update.planId()))
                    .map(plan -> withFlows(plan, update))
                    .map(plan -> {
                        flowValidationDomainService.validateAndSanitizeHttpV4(null, update.flows());
                        return plan;
                    })
                    .orElseThrow(() -> new PlanNotFoundException(update.planId()))
            )
            .toList();
        // The closed-plan check only reads the statuses of the plans it is given.
        var statuses = toWrite.stream().collect(toMap(Plan::getId, Plan::getPlanStatus));

        var updated = toWrite
            .stream()
            .map(plan -> updatePlanDomainService.updatePlanForApiProduct(plan, statuses, apiProduct, input.auditInfo()))
            .toList();
        log.debug("Updated the flows of {} plans of API Product {}", updated.size(), input.apiProductId());
        return new Output(updated);
    }

    private static void rejectPlansListedTwice(List<PlanFlows> plans) {
        var seen = new HashSet<String>();
        plans
            .stream()
            .map(PlanFlows::planId)
            .filter(planId -> !seen.add(planId))
            .findFirst()
            .ifPresent(planId -> {
                throw new ValidationDomainException("Plan '" + planId + "' is listed more than once");
            });
    }

    private static Plan withFlows(Plan plan, PlanFlows update) {
        var definition = plan.getPlanDefinitionHttpV4();
        if (definition == null) {
            throw new ValidationDomainException("Plan '" + plan.getId() + "' is not an HTTP v4 plan");
        }
        if (update.name() != null) {
            plan.setName(update.name());
            definition.setName(update.name());
        }
        definition.setFlows(update.flows());
        return plan;
    }

    public record Input(String apiProductId, List<PlanFlows> plans, AuditInfo auditInfo) {}

    /** A {@code null} name keeps the stored one; the flows are required, a plan to leave alone is left out of the input. */
    public record PlanFlows(String planId, @Nullable String name, List<Flow> flows) {
        public PlanFlows {
            Objects.requireNonNull(flows, () -> "flows of plan '" + planId + "' must not be null");
        }
    }

    public record Output(List<Plan> updated) {}
}
