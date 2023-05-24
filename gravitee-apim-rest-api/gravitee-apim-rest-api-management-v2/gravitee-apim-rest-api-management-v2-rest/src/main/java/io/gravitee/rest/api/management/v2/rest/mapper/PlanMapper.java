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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, DateMapper.class, FlowMapper.class })
public interface PlanMapper {
    PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);

    @Mapping(target = "security.type", qualifiedByName = "convertToSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(constant = "V4", target = "definitionVersion")
    PlanV4 convert(PlanEntity planEntity);

    Set<PlanV4> convertSet(Set<PlanEntity> planEntityList);

    @Mapping(target = "status", qualifiedByName = "convertPlanStatusV2")
    @Mapping(source = "security", target = "security.type")
    @Mapping(source = "securityDefinition", target = "security.configuration")
    @Mapping(constant = "V2", target = "definitionVersion")
    PlanV2 convert(io.gravitee.rest.api.model.PlanEntity planEntity);

    default List<Plan> convert(List<GenericPlanEntity> entities) {
        if (Objects.isNull(entities)) {
            return null;
        }
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream().map(this::convert).collect(Collectors.toList());
    }

    default Plan convert(GenericPlanEntity entity) {
        if (entity instanceof PlanEntity) {
            return new Plan(this.convert((PlanEntity) entity));
        } else {
            return new Plan(this.convert((io.gravitee.rest.api.model.PlanEntity) entity));
        }
    }

    @Mapping(target = "security.type", qualifiedByName = "convertFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    PlanEntity convert(PlanV4 plan);

    @Named("convertToSecurityType")
    default PlanSecurityType convertToSecurityType(String securityType) {
        if (Objects.isNull(securityType)) {
            return null;
        }
        return PlanSecurityType.fromValue(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOfLabel(securityType).name());
    }

    @Named("convertFromSecurityType")
    default String convertFromSecurityType(PlanSecurityType securityType) {
        if (Objects.isNull(securityType)) {
            return null;
        }
        return io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOf(securityType.name()).getLabel();
    }

    @Named("convertPlanStatusV2")
    default PlanStatus convertPlanStatusV2(io.gravitee.rest.api.model.PlanStatus planStatus) {
        if (Objects.isNull(planStatus)) {
            return null;
        }
        return PlanStatus.valueOf(planStatus.name());
    }

    // Rule -- for < V4 plans
    Rule map(io.gravitee.definition.model.Rule rule);

    List<Rule> map(List<io.gravitee.definition.model.Rule> rule);

    default Map<String, List<Rule>> map(Map<String, List<io.gravitee.definition.model.Rule>> paths) {
        if (Objects.isNull(paths)) {
            return null;
        }

        var output = new HashMap<String, List<Rule>>();
        paths.forEach((k, v) -> output.put(k, this.map(v)));

        return output;
    }
}
