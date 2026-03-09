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

import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.model.PlanUpdates;
import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.CreateApiProductPlan;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.UpdateGenericApiProductPlan;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.factory.Mappers;

@Mapper(
    uses = { ConfigurationSerializationMapper.class, DateMapper.class, FlowMapper.class, RuleMapper.class },
    nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface ApiProductPlanMapper {
    ApiProductPlanMapper INSTANCE = Mappers.getMapper(ApiProductPlanMapper.class);

    @Mapping(source = "security.type", target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(source = "security.configuration", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    ApiProductPlan map(PlanEntity planEntity);

    default List<ApiProductPlan> convert(List<Plan> entities) {
        if (Objects.isNull(entities) || CollectionUtils.isEmpty(entities)) {
            return new ArrayList<>();
        }
        return entities.stream().map(this::map).collect(Collectors.toList());
    }

    default ApiProductPlan mapGenericPlan(GenericPlanEntity entity) {
        return switch (entity) {
            case PlanEntity planEntity -> map(planEntity);
            case Plan plan -> map(plan);
            case null -> null;
            default -> throw new IllegalArgumentException("Unsupported plan type: " + entity.getClass());
        };
    }

    @Mapping(source = "planSecurity.type", target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(source = "planSecurity.configuration", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(source = "planStatus", target = "status")
    @Mapping(source = "planMode", target = "mode")
    ApiProductPlan map(Plan plan);

    @Mapping(target = "validation", defaultValue = "MANUAL")
    @Mapping(target = "definitionVersion", constant = "V4")
    @Mapping(target = "planDefinitionHttpV4", source = "source", qualifiedByName = "mapToPlanDefinitionHttpV4")
    io.gravitee.apim.core.plan.model.Plan map(CreateApiProductPlan source);

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

    @Named("mapToPlanDefinitionHttpV4")
    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "mode", defaultValue = "STANDARD")
    io.gravitee.definition.model.v4.plan.Plan mapToPlanDefinitionHttpV4(CreateApiProductPlan source);

    @Mapping(source = "planSecurity.type", target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(target = "mode", source = "planMode")
    @Mapping(source = "planSecurity.configuration", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    BasePlan map(GenericPlanEntity plan);

    @Mapping(target = "securityConfiguration", source = "security.configuration", qualifiedByName = "serializeConfiguration")
    PlanUpdates mapToPlanUpdates(UpdateGenericApiProductPlan updatePlanV4);

    /**
     * API Products don't send tags in update requests. Set tags to empty set so PlanUpdates.applyTo()
     * receives non-null and sets result to empty (matching DB), avoiding false deploy banner on cosmetic changes.
     */
    @AfterMapping
    @SuppressWarnings("unused")
    default void setEmptyTagsForApiProductUpdate(UpdateGenericApiProductPlan source, @MappingTarget PlanUpdates target) {
        if (target.getTags() == null) {
            target.setTags(Collections.emptySet());
        }
    }
}
