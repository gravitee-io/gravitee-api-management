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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("PlanMapperV4")
@Slf4j
public class PlanMapper {

    public PlanEntity toEntity(Plan plan, List<Flow> flows) {
        PlanEntity entity = new PlanEntity();

        entity.setId(plan.getId());
        entity.setDefinitionVersion(plan.getDefinitionVersion());
        entity.setCrossId(plan.getCrossId());
        entity.setName(plan.getName());
        entity.setDescription(plan.getDescription());
        entity.setApiId(plan.getApi());
        entity.setEnvironmentId(plan.getEnvironmentId());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setClosedAt(plan.getClosedAt());
        entity.setNeedRedeployAt(plan.getNeedRedeployAt() == null ? plan.getUpdatedAt() : plan.getNeedRedeployAt());
        entity.setPublishedAt(plan.getPublishedAt());
        entity.setOrder(plan.getOrder());
        entity.setExcludedGroups(plan.getExcludedGroups());
        entity.setFlows(flows);
        entity.setType(PlanType.valueOf(plan.getType().name()));
        if (plan.getMode() != null) {
            entity.setMode(PlanMode.valueOf(plan.getMode().name()));
        } else {
            entity.setMode(PlanMode.STANDARD);
        }

        // Backward compatibility
        if (plan.getStatus() != null) {
            entity.setStatus(PlanStatus.valueOf(plan.getStatus().name()));
        } else {
            entity.setStatus(PlanStatus.PUBLISHED);
        }

        if (Plan.PlanMode.PUSH != plan.getMode()) {
            PlanSecurity security = new PlanSecurity();
            security.setType(PlanSecurityType.valueOf(plan.getSecurity().name()).getLabel());
            security.setConfiguration(plan.getSecurityDefinition());
            entity.setSecurity(security);
        }

        entity.setValidation(PlanValidationType.valueOf(plan.getValidation().name()));
        entity.setCharacteristics(plan.getCharacteristics());
        entity.setCommentRequired(plan.isCommentRequired());
        entity.setCommentMessage(plan.getCommentMessage());
        entity.setTags(plan.getTags());
        entity.setSelectionRule(plan.getSelectionRule());
        entity.setGeneralConditions(plan.getGeneralConditions());
        return entity;
    }

    public Plan toRepository(final NewPlanEntity newPlanEntity) {
        Plan plan = new Plan();
        plan.setDefinitionVersion(DefinitionVersion.V4);
        plan.setId(newPlanEntity.getId());
        plan.setCrossId(newPlanEntity.getCrossId());
        plan.setDefinitionVersion(DefinitionVersion.V4);
        plan.setApi(newPlanEntity.getApiId());
        plan.setName(newPlanEntity.getName());
        plan.setDescription(newPlanEntity.getDescription());
        plan.setCreatedAt(new Date());
        plan.setUpdatedAt(plan.getCreatedAt());
        plan.setNeedRedeployAt(plan.getCreatedAt());
        plan.setType(Plan.PlanType.valueOf(newPlanEntity.getType().name()));
        plan.setMode(Plan.PlanMode.valueOf(newPlanEntity.getMode().name()));
        if (newPlanEntity.getMode() == PlanMode.STANDARD && newPlanEntity.getSecurity() != null) {
            PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(newPlanEntity.getSecurity().getType());
            plan.setSecurity(Plan.PlanSecurityType.valueOf(planSecurityType.name()));
            plan.setSecurityDefinition(newPlanEntity.getSecurity().getConfiguration());
        }
        plan.setStatus(Plan.Status.valueOf(newPlanEntity.getStatus().name()));
        plan.setExcludedGroups(newPlanEntity.getExcludedGroups());
        plan.setCommentRequired(newPlanEntity.isCommentRequired());
        plan.setCommentMessage(newPlanEntity.getCommentMessage());
        plan.setTags(newPlanEntity.getTags());
        plan.setSelectionRule(newPlanEntity.getSelectionRule());
        plan.setGeneralConditions(newPlanEntity.getGeneralConditions());
        plan.setOrder(newPlanEntity.getOrder());

        if (newPlanEntity.getStatus() == PlanStatus.PUBLISHED) {
            plan.setPublishedAt(new Date());
        }

        if (plan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
            // There is no need for a validation when authentication is KEY_LESS, force to AUTO
            plan.setValidation(Plan.PlanValidationType.AUTO);
        } else {
            plan.setValidation(Plan.PlanValidationType.valueOf(newPlanEntity.getValidation().name()));
        }

        plan.setCharacteristics(newPlanEntity.getCharacteristics());
        return plan;
    }

    public List<io.gravitee.definition.model.v4.plan.Plan> toDefinitions(final Set<PlanEntity> planEntities) {
        return planEntities.stream().map(this::toDefinition).collect(Collectors.toList());
    }

    public io.gravitee.definition.model.v4.plan.Plan toDefinition(final PlanEntity planEntity) {
        io.gravitee.definition.model.v4.plan.Plan planDefinition = new io.gravitee.definition.model.v4.plan.Plan();
        planDefinition.setId(planEntity.getId());
        planDefinition.setSecurity(planEntity.getSecurity());
        planDefinition.setMode(io.gravitee.definition.model.v4.plan.PlanMode.valueOf(planEntity.getMode().name()));
        planDefinition.setFlows(planEntity.getFlows());
        planDefinition.setId(planEntity.getId());
        planDefinition.setName(planEntity.getName());
        planDefinition.setSelectionRule(planEntity.getSelectionRule());
        planDefinition.setStatus(planEntity.getStatus());
        planDefinition.setTags(planEntity.getTags());
        return planDefinition;
    }

    public List<NativePlan> toNativeDefinitions(final Set<PlanEntity> planEntities) {
        return planEntities.stream().map(this::toNativeDefinition).collect(Collectors.toList());
    }

    public NativePlan toNativeDefinition(final PlanEntity planEntity) {
        NativePlan planDefinition = new NativePlan();
        planDefinition.setId(planEntity.getId());
        planDefinition.setSecurity(planEntity.getSecurity());
        planDefinition.setMode(io.gravitee.definition.model.v4.plan.PlanMode.valueOf(planEntity.getMode().name()));
        // FIXME: Kafka Gateway - PlanEntity is HTTP oriented, probably need a NativePlanEntity ?
        planDefinition.setFlows(List.of());
        planDefinition.setId(planEntity.getId());
        planDefinition.setName(planEntity.getName());
        planDefinition.setSelectionRule(planEntity.getSelectionRule());
        planDefinition.setStatus(planEntity.getStatus());
        planDefinition.setTags(planEntity.getTags());
        return planDefinition;
    }

    public UpdatePlanEntity toUpdatePlanEntity(final PlanEntity planEntity) {
        UpdatePlanEntity updatePlanEntity = new UpdatePlanEntity();
        updatePlanEntity.setId(planEntity.getId());
        updatePlanEntity.setCrossId(planEntity.getCrossId());
        updatePlanEntity.setName(planEntity.getName());
        updatePlanEntity.setDescription(planEntity.getDescription());
        updatePlanEntity.setValidation(planEntity.getValidation());

        updatePlanEntity.setCharacteristics(planEntity.getCharacteristics());
        updatePlanEntity.setOrder(planEntity.getOrder());
        updatePlanEntity.setExcludedGroups(planEntity.getExcludedGroups());
        updatePlanEntity.setSecurity(planEntity.getSecurity());
        updatePlanEntity.setCommentRequired(planEntity.isCommentRequired());
        updatePlanEntity.setCommentMessage(planEntity.getCommentMessage());
        updatePlanEntity.setGeneralConditions(planEntity.getGeneralConditions());
        updatePlanEntity.setTags(planEntity.getTags());
        updatePlanEntity.setSelectionRule(planEntity.getSelectionRule());
        updatePlanEntity.setFlows(planEntity.getFlows());
        return updatePlanEntity;
    }

    public NewPlanEntity toNewPlanEntity(PlanEntity planEntity) {
        return toNewPlanEntity(planEntity, false);
    }

    public NewPlanEntity toNewPlanEntity(final PlanEntity planEntity, final boolean resetCrossId) {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setId(planEntity.getId());
        newPlanEntity.setCrossId(resetCrossId ? null : planEntity.getCrossId());
        newPlanEntity.setApiId(planEntity.getApiId());
        newPlanEntity.setName(planEntity.getName());
        newPlanEntity.setDescription(planEntity.getDescription());
        newPlanEntity.setOrder(planEntity.getOrder());

        if (planEntity.getValidation() != null) {
            newPlanEntity.setValidation(planEntity.getValidation());
        }
        if (planEntity.getSecurity() != null) {
            newPlanEntity.setSecurity(planEntity.getSecurity());
        }
        newPlanEntity.setSecurity(planEntity.getSecurity());
        if (planEntity.getType() != null) {
            newPlanEntity.setType(planEntity.getType());
        }
        if (planEntity.getStatus() != null) {
            newPlanEntity.setStatus(planEntity.getStatus());
        }
        if (planEntity.getFlows() != null) {
            newPlanEntity.setFlows(planEntity.getFlows());
        }
        if (planEntity.getType() != null) {
            newPlanEntity.setType(planEntity.getType());
        }
        if (planEntity.getMode() != null) {
            newPlanEntity.setMode(planEntity.getMode());
        }
        newPlanEntity.setCharacteristics(planEntity.getCharacteristics());
        newPlanEntity.setExcludedGroups(planEntity.getExcludedGroups());
        newPlanEntity.setCommentRequired(planEntity.isCommentRequired());
        newPlanEntity.setCommentMessage(planEntity.getCommentMessage());
        newPlanEntity.setGeneralConditions(planEntity.getGeneralConditions());
        newPlanEntity.setTags(planEntity.getTags());
        newPlanEntity.setSelectionRule(planEntity.getSelectionRule());
        return newPlanEntity;
    }
}
