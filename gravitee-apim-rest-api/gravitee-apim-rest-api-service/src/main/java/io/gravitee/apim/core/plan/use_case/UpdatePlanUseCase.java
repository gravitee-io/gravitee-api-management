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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.apim.core.secret.domain_service.SecretReferencePermissionDomainService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePlanUseCase {

    private final UpdatePlanDomainService updatePlanDomainService;
    private final PlanCrudService planCrudService;
    private final ApiCrudService apiCrudService;
    private final FlowCrudService flowCrudService;
    private final SecretReferencePermissionDomainService secretReferencePermissionDomainService;

    public Output execute(Input input) {
        final Plan planEntity = planCrudService
            .findByPlanIdAndReferenceIdAndReferenceType(input.planToUpdate.getId(), input.apiId, GenericPlanEntity.ReferenceType.API.name())
            .orElseThrow(() -> new PlanNotFoundException(input.planToUpdate.getId()));
        if (input.planToUpdate.getValidation() == null) {
            input.planToUpdate.setValidation(planEntity.getValidation());
        }

        // applyTo changes the stored plan definition in place, so its references are read first.
        var storedReferenceSources = referenceSources(planEntity.getPlanDefinitionV4());

        var updatedEntity = input.planToUpdate.applyTo(planEntity);

        var api = apiCrudService.get(input.apiId);
        List<? extends AbstractFlow> flows = input.flowProvider.apply(api);
        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            secretReferencePermissionDomainService.validate(
                input.auditInfo.organizationId(),
                input.auditInfo.environmentId(),
                input.auditInfo.actor().userId(),
                List.of(storedReferenceSources, storedFlows(api, planEntity.getId())),
                List.of(referenceSources(updatedEntity.getPlanDefinitionV4()), flows)
            );
        }

        var updated = updatePlanDomainService.update(updatedEntity, flows, Map.of(), api, input.auditInfo);

        return new Output(new PlanWithFlows(updated, flows));
    }

    private static List<String> referenceSources(AbstractPlan definition) {
        var security = definition.getSecurity();
        return Arrays.asList(definition.getSelectionRule(), security == null ? null : security.getConfiguration());
    }

    private List<? extends AbstractFlow> storedFlows(Api api, String planId) {
        return api.getType() == ApiType.NATIVE ? flowCrudService.getNativePlanFlows(planId) : flowCrudService.getPlanV4Flows(planId);
    }

    @Builder
    public record Input(
        PlanUpdates planToUpdate,
        Function<Api, List<? extends AbstractFlow>> flowProvider,
        String apiId,
        AuditInfo auditInfo
    ) {}

    public record Output(PlanWithFlows updated) {}
}
