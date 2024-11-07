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

import io.gravitee.definition.model.v4.flow.Flow;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PlanWithFlows extends Plan {

    List<Flow> flows;

    public PlanWithFlows(Plan plan, List<Flow> flows) {
        super(
            plan.getId(),
            plan.getDefinitionVersion(),
            plan.getCrossId(),
            plan.getName(),
            plan.getDescription(),
            plan.getCreatedAt(),
            plan.getUpdatedAt(),
            plan.getPublishedAt(),
            plan.getClosedAt(),
            plan.getNeedRedeployAt(),
            plan.getValidation(),
            plan.getType(),
            plan.getApiId(),
            plan.getEnvironmentId(),
            plan.getOrder(),
            plan.getCharacteristics(),
            plan.getExcludedGroups(),
            plan.isCommentRequired(),
            plan.getCommentMessage(),
            plan.getGeneralConditions(),
            plan.getPlanDefinitionHttpV4(),
            plan.getPlanDefinitionNativeV4(),
            plan.getPlanDefinitionV2(),
            plan.getFederatedPlanDefinition(),
            plan.getApiType()
        );
        this.flows = flows;
    }
}
