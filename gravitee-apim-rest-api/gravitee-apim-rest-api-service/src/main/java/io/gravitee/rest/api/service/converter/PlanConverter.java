/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
public class PlanConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanConverter.class);

    @Autowired
    private ObjectMapper objectMapper;

    public PlanEntity toPlanEntity(Plan plan) {
        return toPlanEntity(plan, null);
    }

    public PlanEntity toPlanEntity(Plan plan, ApiEntity apiEntity) {
        PlanEntity entity = new PlanEntity();

        entity.setId(plan.getId());
        entity.setCrossId(plan.getCrossId());
        entity.setName(plan.getName());
        entity.setDescription(plan.getDescription());
        entity.setApi(plan.getApi());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setOrder(plan.getOrder());
        entity.setExcludedGroups(plan.getExcludedGroups());

        if (plan.getDefinition() != null && !plan.getDefinition().isEmpty()) {
            try {
                HashMap<String, List<Rule>> rules = objectMapper.readValue(plan.getDefinition(), new TypeReference<>() {});
                entity.setPaths(rules);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating policy definition", ioe);
            }
        }

        if (apiEntity != null) {
            if (DefinitionVersion.V2.equals(DefinitionVersion.valueOfLabel(apiEntity.getGraviteeDefinitionVersion()))) {
                Optional<List<Flow>> planFlows = apiEntity
                    .getPlans()
                    .stream()
                    .filter(pl -> plan.getId() != null && plan.getId().equals(pl.getId()))
                    .map(io.gravitee.definition.model.Plan::getFlows)
                    .findFirst();
                planFlows.ifPresent(entity::setFlows);
            }
        }

        entity.setType(PlanType.valueOf(plan.getType().name()));

        // Backward compatibility
        if (plan.getStatus() != null) {
            entity.setStatus(PlanStatus.valueOf(plan.getStatus().name()));
        } else {
            entity.setStatus(PlanStatus.PUBLISHED);
        }

        if (plan.getSecurity() != null) {
            entity.setSecurity(PlanSecurityType.valueOf(plan.getSecurity().name()));
        } else {
            entity.setSecurity(PlanSecurityType.API_KEY);
        }

        entity.setSecurityDefinition(plan.getSecurityDefinition());
        entity.setClosedAt(plan.getClosedAt());
        entity.setNeedRedeployAt(plan.getNeedRedeployAt() == null ? plan.getUpdatedAt() : plan.getNeedRedeployAt());
        entity.setPublishedAt(plan.getPublishedAt());
        entity.setValidation(PlanValidationType.valueOf(plan.getValidation().name()));
        entity.setCharacteristics(plan.getCharacteristics());
        entity.setCommentRequired(plan.isCommentRequired());
        entity.setCommentMessage(plan.getCommentMessage());
        entity.setTags(plan.getTags());
        entity.setSelectionRule(plan.getSelectionRule());
        entity.setGeneralConditions(plan.getGeneralConditions());
        return entity;
    }

    public UpdatePlanEntity toUpdatePlanEntity(PlanEntity planEntity) {
        UpdatePlanEntity updatePlanEntity = new UpdatePlanEntity();
        updatePlanEntity.setId(planEntity.getId());
        updatePlanEntity.setCrossId(planEntity.getCrossId());
        updatePlanEntity.setName(planEntity.getName());
        updatePlanEntity.setDescription(planEntity.getDescription());
        updatePlanEntity.setValidation(planEntity.getValidation());
        if (planEntity.getPaths() != null) {
            updatePlanEntity.setPaths(planEntity.getPaths());
        }
        updatePlanEntity.setCharacteristics(planEntity.getCharacteristics());
        updatePlanEntity.setOrder(planEntity.getOrder());
        updatePlanEntity.setExcludedGroups(planEntity.getExcludedGroups());
        updatePlanEntity.setSecurityDefinition(planEntity.getSecurityDefinition());
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

    public NewPlanEntity toNewPlanEntity(PlanEntity planEntity, boolean resetCrossId) {
        NewPlanEntity newPlanEntity = new NewPlanEntity();
        newPlanEntity.setId(planEntity.getId());
        newPlanEntity.setCrossId(resetCrossId ? null : planEntity.getCrossId());
        newPlanEntity.setApi(planEntity.getApi());
        newPlanEntity.setName(planEntity.getName());
        newPlanEntity.setDescription(planEntity.getDescription());
        if (planEntity.getValidation() != null) {
            newPlanEntity.setValidation(planEntity.getValidation());
        }
        if (planEntity.getSecurity() != null) {
            newPlanEntity.setSecurity(planEntity.getSecurity());
        }
        newPlanEntity.setSecurityDefinition(planEntity.getSecurityDefinition());
        if (planEntity.getType() != null) {
            newPlanEntity.setType(planEntity.getType());
        }
        if (planEntity.getStatus() != null) {
            newPlanEntity.setStatus(planEntity.getStatus());
        }
        if (planEntity.getPaths() != null) {
            newPlanEntity.setPaths(planEntity.getPaths());
        }
        if (planEntity.getFlows() != null) {
            newPlanEntity.setFlows(planEntity.getFlows());
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

    public Plan toPlan(NewPlanEntity newPlan, DefinitionVersion graviteeDefinitionVersion) throws JsonProcessingException {
        Plan plan = new Plan();
        plan.setId(newPlan.getId());
        plan.setCrossId(newPlan.getCrossId());
        plan.setApi(newPlan.getApi());
        plan.setName(newPlan.getName());
        plan.setDescription(newPlan.getDescription());
        plan.setCreatedAt(new Date());
        plan.setUpdatedAt(plan.getCreatedAt());
        plan.setNeedRedeployAt(plan.getCreatedAt());
        plan.setType(Plan.PlanType.valueOf(newPlan.getType().name()));
        plan.setSecurity(Plan.PlanSecurityType.valueOf(newPlan.getSecurity().name()));
        plan.setSecurityDefinition(newPlan.getSecurityDefinition());
        plan.setStatus(Plan.Status.valueOf(newPlan.getStatus().name()));
        plan.setExcludedGroups(newPlan.getExcludedGroups());
        plan.setCommentRequired(newPlan.isCommentRequired());
        plan.setCommentMessage(newPlan.getCommentMessage());
        plan.setTags(newPlan.getTags());
        plan.setSelectionRule(newPlan.getSelectionRule());
        plan.setGeneralConditions(newPlan.getGeneralConditions());
        plan.setOrder(newPlan.getOrder());

        if (plan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
            // There is no need for a validation when authentication is KEY_LESS, force to AUTO
            plan.setValidation(Plan.PlanValidationType.AUTO);
        } else {
            plan.setValidation(Plan.PlanValidationType.valueOf(newPlan.getValidation().name()));
        }

        plan.setCharacteristics(newPlan.getCharacteristics());

        if (!DefinitionVersion.V2.equals(graviteeDefinitionVersion)) {
            String planPolicies = objectMapper.writeValueAsString(newPlan.getPaths());
            plan.setDefinition(planPolicies);
        }

        return plan;
    }
}
