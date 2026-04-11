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
package io.gravitee.apim.core.api.domain_service.import_definition;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
class ImportDefinitionPlanDomainService {

    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final PlanCrudService planCrudService;

    ImportDefinitionPlanDomainService(
        CreatePlanDomainService createPlanDomainService,
        UpdatePlanDomainService updatePlanDomainService,
        DeletePlanDomainService deletePlanDomainService,
        PlanCrudService planCrudService
    ) {
        this.createPlanDomainService = createPlanDomainService;
        this.updatePlanDomainService = updatePlanDomainService;
        this.deletePlanDomainService = deletePlanDomainService;
        this.planCrudService = planCrudService;
    }

    void upsertPlanWithFlows(Api api, @NotNull Set<PlanWithFlows> plansWithFlows, AuditInfo auditInfo) {
        if (plansWithFlows.isEmpty()) {
            return;
        }

        var savedPlans = planCrudService.findByApiId(api.getId());

        // Build indexed lookups once — O(m) — to avoid O(n*m) per-item streaming inside the loop.
        var savedByCrossId = savedPlans
            .stream()
            .filter(p -> p.getCrossId() != null)
            .collect(Collectors.toMap(Plan::getCrossId, p -> p, (a, b) -> a));
        var savedById = savedPlans.stream().collect(Collectors.toMap(Plan::getId, p -> p, (a, b) -> a));

        var unmatchedSavedPlanIds = new HashSet<>(savedById.keySet());

        for (PlanWithFlows planWithFlow : plansWithFlows) {
            findSavedPlanForImport(savedByCrossId, savedById, planWithFlow).ifPresentOrElse(
                existing -> updateMatchedPlan(existing, planWithFlow, unmatchedSavedPlanIds, api, auditInfo),
                () -> createPlanDomainService.create(planWithFlow, planWithFlow.getFlows(), api, auditInfo)
            );
        }

        // FIXME: Support closing plans when export includes closed ones (currently mimics V2 promotion behavior and remove missing plans).
        unmatchedSavedPlanIds.forEach(planId -> deletePlanDomainService.delete(savedById.get(planId), auditInfo));
    }

    private void updateMatchedPlan(Plan existing, PlanWithFlows incoming, HashSet<String> unmatchedIds, Api api, AuditInfo auditInfo) {
        incoming.setId(existing.getId());
        incoming.setReferenceId(existing.getReferenceId());
        incoming.setReferenceType(existing.getReferenceType());
        updatePlanDomainService.update(incoming, incoming.getFlows(), Collections.emptyMap(), api, auditInfo);
        unmatchedIds.remove(existing.getId());
    }

    /** Match by {@code crossId} when present and found; else by {@code id}. */
    private static Optional<Plan> findSavedPlanForImport(
        Map<String, Plan> savedByCrossId,
        Map<String, Plan> savedById,
        PlanWithFlows incomingPlan
    ) {
        if (incomingPlan.getCrossId() != null) {
            var byCrossId = Optional.ofNullable(savedByCrossId.get(incomingPlan.getCrossId()));
            if (byCrossId.isPresent()) return byCrossId;
        }
        // incomingPlan.getId() is null only in manually crafted definitions that omit the id field.
        // A normal Gravitee export always includes the plan id; the guard prevents NPE in the map lookup.
        return incomingPlan.getId() == null ? Optional.empty() : Optional.ofNullable(savedById.get(incomingPlan.getId()));
    }
}
