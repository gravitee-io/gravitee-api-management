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
import java.util.Collections;
import java.util.HashSet;
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

    void upsertPlanWithFlows(Api api, Set<PlanWithFlows> plansWithFlows, AuditInfo auditInfo) {
        var savedPlans = planCrudService.findByApiId(api.getId());
        var crossIds = savedPlans.stream().map(Plan::getCrossId).collect(Collectors.toCollection(HashSet::new));

        if (plansWithFlows != null) {
            for (PlanWithFlows planWithFlow : plansWithFlows) {
                var crossId = planWithFlow.getCrossId();
                if (crossIds.contains(crossId)) {
                    updatePlanDomainService.update(planWithFlow, planWithFlow.getFlows(), Collections.emptyMap(), api, auditInfo);
                } else {
                    createPlanDomainService.create(planWithFlow, planWithFlow.getFlows(), api, auditInfo);
                }
                crossIds.remove(crossId);
            }
        }

        // FIXME: Support closing plans when export includes closed ones (currently mimics V2 promotion behavior and remove missing plans).
        if (!crossIds.isEmpty()) {
            savedPlans
                .stream()
                .filter(plan -> crossIds.contains(plan.getCrossId()))
                .forEach(removedPlan -> deletePlanDomainService.delete(removedPlan, auditInfo));
        }
    }
}
