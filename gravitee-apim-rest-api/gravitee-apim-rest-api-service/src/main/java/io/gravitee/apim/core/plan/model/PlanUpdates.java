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
package io.gravitee.apim.core.plan.model;

import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
public class PlanUpdates {

    private String id;
    private String name;
    private String crossId;
    private String description;
    private ZonedDateTime updatedAt;
    private boolean commentRequired;
    private String commentMessage;
    private String generalConditions;
    private List<String> excludedGroups;
    private List<String> characteristics;
    private int order;

    private Set<String> tags;
    private String selectionRule;
    private String securityConfiguration;
    private Plan.PlanValidationType validation;
    private String referenceId;
    private GenericPlanEntity.ReferenceType referenceType;

    public Plan applyTo(Plan oldPlan) {
        Plan result = oldPlan
            .toBuilder()
            // Preserve existing name when not provided in updates to avoid null constraint violations.
            .name(name != null ? name : oldPlan.getName())
            .crossId(crossId != null ? crossId : oldPlan.getCrossId())
            .description(description)
            .updatedAt(ZonedDateTime.now())
            .commentRequired(commentRequired)
            .commentMessage(commentMessage)
            .generalConditions(generalConditions)
            .excludedGroups(excludedGroups)
            .characteristics(characteristics)
            .order(order)
            .validation(validation != null ? validation : oldPlan.getValidation())
            .referenceId(oldPlan.getReferenceId())
            .referenceType(oldPlan.getReferenceType())
            .build();

        result.setPlanTags(tags);
        var definition = result.getPlanDefinitionV4();
        definition.setSelectionRule(selectionRule);
        if (definition.getSecurity() != null) {
            definition.getSecurity().setConfiguration(securityConfiguration);
        }
        return result;
    }
}
