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
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.domain_service.UpdatePlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.v4.flow.AbstractFlow;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdatePlanUseCase {

    private final UpdatePlanDomainService updatePlanDomainService;
    private final PlanCrudService planCrudService;
    private final ApiCrudService apiCrudService;

    public Output execute(Input input) {
        final Plan planEntity = planCrudService
            .findById(input.planToUpdate.getId())
            .orElseThrow(() -> new PlanNotFoundException(input.planToUpdate.getId()));

        if (!planEntity.getApiId().equals(input.apiId)) {
            throw new PlanNotFoundException(input.planToUpdate.getId());
        }

        var updatedEntity = update(planEntity, input.planToUpdate);

        var api = apiCrudService.get(input.apiId);
        List<? extends AbstractFlow> flows = input.flowProvider.apply(api);

        var updated = updatePlanDomainService.update(updatedEntity, flows, Map.of(), api, input.auditInfo);

        return new Output(new PlanWithFlows(updated, flows));
    }

    private Plan update(Plan oldPlan, UpdatePlanEntity updatePlan) {
        Plan newPlan = oldPlan
            .toBuilder()
            .name(updatePlan.getName())
            .crossId(updatePlan.getCrossId() != null ? updatePlan.getCrossId() : oldPlan.getCrossId())
            .description(updatePlan.getDescription())
            .updatedAt(ZonedDateTime.now())
            .commentRequired(updatePlan.isCommentRequired())
            .commentMessage(updatePlan.getCommentMessage())
            .generalConditions(updatePlan.getGeneralConditions())
            .excludedGroups(updatePlan.getExcludedGroups())
            .characteristics(updatePlan.getCharacteristics())
            .order(updatePlan.getOrder())
            .build();

        newPlan.setPlanTags(updatePlan.getTags());
        var planDefinitionV4 = newPlan.getPlanDefinitionV4();
        planDefinitionV4.setSelectionRule(updatePlan.getSelectionRule());
        planDefinitionV4.setSecurity(updatePlan.getSecurity());

        if (
            newPlan.getPlanSecurity() != null && Objects.equals(newPlan.getPlanSecurity().getType(), PlanSecurityType.KEY_LESS.getLabel())
        ) {
            newPlan.setValidation(Plan.PlanValidationType.AUTO);
        } else {
            newPlan.setValidation(Plan.PlanValidationType.valueOf(updatePlan.getValidation().name()));
        }

        return newPlan;
    }

    @Builder
    public record Input(
        UpdatePlanEntity planToUpdate,
        Function<Api, List<? extends AbstractFlow>> flowProvider,
        String apiId,
        AuditInfo auditInfo
    ) {}

    public record Output(PlanWithFlows updated) {}
}
