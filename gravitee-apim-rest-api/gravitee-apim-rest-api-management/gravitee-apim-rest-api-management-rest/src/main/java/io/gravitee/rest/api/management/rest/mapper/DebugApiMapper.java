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
package io.gravitee.rest.api.management.rest.mapper;

import io.gravitee.definition.model.Plan;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.rest.api.model.DebugApiEntity;
import io.gravitee.rest.api.model.PlanEntity;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface DebugApiMapper {
    DebugApiMapper INSTANCE = Mappers.getMapper(DebugApiMapper.class);

    @Mapping(target = "pathMappings", ignore = true)
    DebugApi fromEntity(DebugApiEntity source);

    default Map<String, Plan> mapPlans(Set<PlanEntity> plans) {
        return plans.stream().map(this::fromEntity).collect(Collectors.toMap(Plan::getId, plan -> plan));
    }

    Plan fromEntity(PlanEntity source);
}
