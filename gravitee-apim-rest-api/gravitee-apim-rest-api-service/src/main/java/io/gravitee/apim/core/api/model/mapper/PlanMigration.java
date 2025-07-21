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
package io.gravitee.apim.core.api.model.mapper;

import io.gravitee.apim.core.api.model.utils.MigrationResult;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import java.util.Date;
import java.util.List;

class PlanMigration {

    MigrationResult<io.gravitee.apim.core.plan.model.Plan> mapPlan(io.gravitee.apim.core.plan.model.Plan plan) {
        if (plan.getPlanDefinitionV2().getFlows() != null && !plan.getPlanDefinitionV2().getFlows().isEmpty()) {
            return MigrationResult.issues(
                List.of(new MigrationResult.Issue("Flow are not supported yet", MigrationResult.State.IMPOSSIBLE))
            );
        }
        return MigrationResult.value(
            plan
                .toBuilder()
                .definitionVersion(DefinitionVersion.V4)
                .needRedeployAt(Date.from(TimeProvider.instantNow()))
                .planDefinitionHttpV4(mapPlanDefinition(plan.getPlanDefinitionV2()))
                .planDefinitionV2(null)
                .apiType(ApiType.PROXY)
                .build()
        );
    }

    private Plan mapPlanDefinition(io.gravitee.definition.model.Plan planDefinitionV2) {
        return new Plan(
            planDefinitionV2.getId(),
            planDefinitionV2.getName(),
            new PlanSecurity(planDefinitionV2.getSecurity(), planDefinitionV2.getSecurityDefinition()),
            PlanMode.STANDARD,
            planDefinitionV2.getSelectionRule(),
            planDefinitionV2.getTags(),
            PlanStatus.valueOf(planDefinitionV2.getStatus()),
            List.of() // TODO
        );
    }
}
