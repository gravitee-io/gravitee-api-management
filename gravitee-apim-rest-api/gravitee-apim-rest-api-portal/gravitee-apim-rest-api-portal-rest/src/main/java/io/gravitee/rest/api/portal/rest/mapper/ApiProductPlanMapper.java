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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.ApiProductPlan.ValidationEnum;
import io.gravitee.rest.api.portal.rest.model.PlanMode;

public final class ApiProductPlanMapper {

    public static final ApiProductPlanMapper INSTANCE = new ApiProductPlanMapper();

    private ApiProductPlanMapper() {}

    public ApiProductPlan map(Plan plan) {
        var result = new ApiProductPlan();

        result.setId(plan.getId());
        result.setName(plan.getName());
        result.setDescription(plan.getDescription());
        result.setCharacteristics(plan.getCharacteristics());
        result.setOrder(plan.getOrder());
        result.setCommentRequired(plan.isCommentRequired());
        result.setCommentQuestion(plan.getCommentMessage());
        if (plan.getPlanSecurity() != null && plan.getPlanSecurity().getType() != null) {
            var securityType = PlanSecurityType.valueOfLabel(plan.getPlanSecurity().getType());
            result.setSecurity(SecurityEnum.fromValue(securityType.name()));
        }
        result.setValidation(ValidationEnum.fromValue(plan.getPlanValidation().name()));
        result.setMode(PlanMode.valueOf(plan.getPlanMode().name()));

        return result;
    }
}
