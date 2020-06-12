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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.portal.rest.model.Plan;
import io.gravitee.rest.api.portal.rest.model.Plan.SecurityEnum;
import io.gravitee.rest.api.portal.rest.model.Plan.ValidationEnum;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanMapper {

    public Plan convert(PlanEntity plan) {
        final Plan planItem = new Plan();

        planItem.setCharacteristics(plan.getCharacteristics());
        planItem.setCommentQuestion(plan.getCommentMessage());
        planItem.setCommentRequired(plan.isCommentRequired());
        planItem.setDescription(plan.getDescription());
        planItem.setId(plan.getId());
        planItem.setName(plan.getName());
        planItem.setOrder(plan.getOrder());
        planItem.setSecurity(SecurityEnum.fromValue(plan.getSecurity().name()));
        planItem.setValidation(ValidationEnum.fromValue(plan.getValidation().name()));
        return planItem;
    }
}
