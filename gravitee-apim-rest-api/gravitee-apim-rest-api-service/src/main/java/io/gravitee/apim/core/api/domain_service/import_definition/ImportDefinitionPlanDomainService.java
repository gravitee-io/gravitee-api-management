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
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.CreatePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.DeletePlanDomainService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@DomainService
class ImportDefinitionPlanDomainService {

    private final CreatePlanDomainService createPlanDomainService;
    private final UpdatePlanDomainService updatePlanDomainService;
    private final DeletePlanDomainService deletePlanDomainService;
    private final PlanCrudService planCrudService;
    private final FlowCrudService flowCrudService;

    ImportDefinitionPlanDomainService(
        CreatePlanDomainService createPlanDomainService,
        UpdatePlanDomainService updatePlanDomainService,
        DeletePlanDomainService deletePlanDomainService,
        PlanCrudService planCrudService,
        FlowCrudService flowCrudService
    ) {
        this.createPlanDomainService = createPlanDomainService;
        this.updatePlanDomainService = updatePlanDomainService;
        this.deletePlanDomainService = deletePlanDomainService;
        this.planCrudService = planCrudService;
        this.flowCrudService = flowCrudService;
    }

    void upsertPlanWithFlows(Api api, Set<PlanWithFlows> plansWithFlows, AuditInfo auditInfo) {
        if (plansWithFlows == null || plansWithFlows.isEmpty()) {
            return;
        }

        var savedPlans = planCrudService.findByApiId(api.getId());
        var unmatchedSavedPlanIds = savedPlans.stream().map(Plan::getId).collect(Collectors.toCollection(HashSet::new));

        for (PlanWithFlows planWithFlow : plansWithFlows) {
            findSavedPlanForImport(savedPlans, planWithFlow).ifPresentOrElse(
                existing -> {
                    planWithFlow.setId(existing.getId());
                    planWithFlow.setReferenceId(existing.getReferenceId());
                    planWithFlow.setReferenceType(existing.getReferenceType());

                    restoreFlowIdsByPosition(planWithFlow.getFlows(), existing.getId());
                    updatePlanDomainService.update(planWithFlow, planWithFlow.getFlows(), Collections.emptyMap(), api, auditInfo);
                    unmatchedSavedPlanIds.remove(existing.getId());
                },
                () -> createPlanDomainService.create(planWithFlow, planWithFlow.getFlows(), api, auditInfo)
            );
        }

        // FIXME: Support closing plans when export includes closed ones (currently mimics V2 promotion behavior and remove missing plans).
        if (!unmatchedSavedPlanIds.isEmpty()) {
            savedPlans
                .stream()
                .filter(plan -> unmatchedSavedPlanIds.contains(plan.getId()))
                .forEach(removedPlan -> deletePlanDomainService.delete(removedPlan, auditInfo));
        }
    }

    /** Match by {@code crossId} when present and found; else by {@code id}. */
    private static Optional<Plan> findSavedPlanForImport(Collection<Plan> savedPlans, PlanWithFlows imported) {
        if (imported.getCrossId() != null) {
            var byCrossId = savedPlans
                .stream()
                .filter(s -> imported.getCrossId().equals(s.getCrossId()))
                .findFirst();
            if (byCrossId.isPresent()) return byCrossId;
        }
        return imported.getId() == null
            ? Optional.empty()
            : savedPlans
                .stream()
                .filter(s -> imported.getId().equals(s.getId()))
                .findFirst();
    }

    private void restoreFlowIdsByPosition(List<? extends AbstractFlow> incomingFlows, String planId) {
        if (incomingFlows == null || incomingFlows.isEmpty()) {
            return;
        }
        List<? extends AbstractFlow> existingFlows = incomingFlows.get(0) instanceof NativeFlow
            ? flowCrudService.getNativePlanFlows(planId)
            : flowCrudService.getPlanV4Flows(planId);
        for (int i = 0; i < incomingFlows.size() && i < existingFlows.size(); i++) {
            incomingFlows.get(i).setId(existingFlows.get(i).getId());
        }
    }
}
