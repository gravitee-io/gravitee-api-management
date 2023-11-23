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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import java.util.*;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = { ConfigurationSerializationMapper.class, DateMapper.class, FlowMapper.class, RuleMapper.class },
    nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface PlanMapper {
    PlanMapper INSTANCE = Mappers.getMapper(PlanMapper.class);

    @Mapping(target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(constant = "V4", target = "definitionVersion")
    PlanV4 map(PlanEntity planEntity);

    Set<PlanV4> map(Set<PlanEntity> planEntityList);

    @Mapping(source = "security", target = "security.type")
    @Mapping(source = "securityDefinition", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(constant = "V2", target = "definitionVersion")
    PlanV2 map(io.gravitee.rest.api.model.PlanEntity planEntity);

    default List<Plan> convert(List<GenericPlanEntity> entities) {
        if (Objects.isNull(entities)) {
            return null;
        }
        if (entities.isEmpty()) {
            return new ArrayList<>();
        }
        return entities.stream().map(this::mapGenericPlan).collect(Collectors.toList());
    }

    default Plan mapGenericPlan(GenericPlanEntity entity) {
        if (entity instanceof PlanEntity) {
            return new Plan(this.map((PlanEntity) entity));
        } else {
            return new Plan(this.map((io.gravitee.rest.api.model.PlanEntity) entity));
        }
    }

    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "validation", defaultValue = "MANUAL")
    @Mapping(target = "mode", defaultValue = "STANDARD")
    NewPlanEntity map(CreatePlanV4 plan);

    @Mapping(target = "security", source = "security.type")
    @Mapping(target = "securityDefinition", source = "security.configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.rest.api.model.NewPlanEntity map(CreatePlanV2 plan);

    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Named("toPlanEntity")
    PlanEntity map(PlanV4 plan);

    @Mapping(target = "name", expression = "java(planName)")
    @Mapping(target = "security", qualifiedByName = "mapToPlanSecurityV4")
    PlanEntity fromPlanCRD(PlanCRD plan, String planName);

    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    UpdatePlanEntity map(UpdatePlanV4 plan);

    @Mapping(target = "securityDefinition", source = "security.configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.rest.api.model.UpdatePlanEntity map(UpdatePlanV2 plan);

    @Named("mapToPlanSecurityV4")
    @Mapping(target = "type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.plan.PlanSecurity mapPlanSecurityV4(PlanSecurity planSecurity);

    @Named("mapToPlanSecurityType")
    default PlanSecurityType mapToPlanSecurityType(String securityType) {
        if (Objects.isNull(securityType)) {
            return null;
        }
        return PlanSecurityType.fromValue(io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOfLabel(securityType).name());
    }

    @Named("mapFromSecurityType")
    default String mapFromSecurityType(PlanSecurityType securityType) {
        if (Objects.isNull(securityType)) {
            return null;
        }
        return io.gravitee.rest.api.model.v4.plan.PlanSecurityType.valueOf(securityType.name()).getLabel();
    }

    @Mapping(source = "planSecurity.type", target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(source = "planSecurity.configuration", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    BasePlan map(GenericPlanEntity plan);

    Collection<BasePlan> mapToBasePlans(Set<GenericPlanEntity> plans);
}
