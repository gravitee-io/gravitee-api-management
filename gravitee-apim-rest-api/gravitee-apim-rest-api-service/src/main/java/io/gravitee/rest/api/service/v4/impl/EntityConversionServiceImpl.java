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
package io.gravitee.rest.api.service.v4.impl;

import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.v4.EntityConversionService;
import io.gravitee.rest.api.service.v4.mapper.PlanEntityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EntityConversionServiceImpl implements EntityConversionService {

    @Override
    public PlanEntity convertV4ToPlanEntity(GenericPlanEntity genericPlanEntity) {
        if (genericPlanEntity instanceof io.gravitee.rest.api.model.v4.plan.PlanEntity v4PlanEntity) {
            PlanEntity convertedPlanEntity = PlanEntityMapper.INSTANCE.convertV4ToPlanEntity(v4PlanEntity);
            log.info("Converted v4 plan entity to rest.api.model plan entity: {}", convertedPlanEntity);
            return convertedPlanEntity;
        }
        throw new IllegalArgumentException("Unsupported entity type: " + genericPlanEntity.getClass().getName());
    }
}
