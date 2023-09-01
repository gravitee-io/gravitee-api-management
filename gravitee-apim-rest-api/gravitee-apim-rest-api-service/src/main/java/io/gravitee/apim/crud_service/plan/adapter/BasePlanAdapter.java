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
package io.gravitee.apim.crud_service.plan.adapter;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.converter.PlanConverter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BasePlanAdapter {

    private final PlanConverter planConverter;

    public BasePlanAdapter(PlanConverter planConverter) {
        this.planConverter = planConverter;
    }

    public GenericPlanEntity toEntityV4(Plan plan) {
        BasePlanEntity entity = new BasePlanEntity();

        entity.setId(plan.getId());
        entity.setCrossId(plan.getCrossId());
        entity.setName(plan.getName());
        entity.setDescription(plan.getDescription());
        entity.setApiId(plan.getApi());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setClosedAt(plan.getClosedAt());
        entity.setNeedRedeployAt(plan.getNeedRedeployAt() == null ? plan.getUpdatedAt() : plan.getNeedRedeployAt());
        entity.setPublishedAt(plan.getPublishedAt());
        entity.setOrder(plan.getOrder());
        entity.setExcludedGroups(plan.getExcludedGroups());
        entity.setType(PlanType.valueOf(plan.getType().name()));
        entity.setMode(plan.getMode() != null ? PlanMode.valueOf(plan.getMode().name()) : PlanMode.STANDARD);

        // Backward compatibility
        entity.setStatus(plan.getStatus() != null ? PlanStatus.valueOf(plan.getStatus().name()) : PlanStatus.PUBLISHED);

        if (Plan.PlanMode.PUSH != plan.getMode()) {
            entity.setSecurity(
                PlanSecurity
                    .builder()
                    .type(PlanSecurityType.valueOf(plan.getSecurity().name()).getLabel())
                    .configuration(plan.getSecurityDefinition())
                    .build()
            );
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

    public GenericPlanEntity toEntityV2(Plan plan) {
        return planConverter.toPlanEntity(plan, List.of());
    }
}
