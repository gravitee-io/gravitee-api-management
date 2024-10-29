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

import io.gravitee.apim.core.plan.model.PlanWithFlows;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.management.v2.rest.model.BasePlan;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.CreatePlanV4;
import io.gravitee.rest.api.management.v2.rest.model.Plan;
import io.gravitee.rest.api.management.v2.rest.model.PlanCRD;
import io.gravitee.rest.api.management.v2.rest.model.PlanFederated;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurity;
import io.gravitee.rest.api.management.v2.rest.model.PlanSecurityType;
import io.gravitee.rest.api.management.v2.rest.model.PlanV2;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanFederated;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV2;
import io.gravitee.rest.api.management.v2.rest.model.UpdatePlanV4;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    PlanV4 map(PlanEntity planEntity);

    @Mapping(target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    PlanFederated mapFederated(PlanEntity planEntity);

    @Mapping(target = "security.type", source = "planDefinitionV4.security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(
        target = "security.configuration",
        source = "planDefinitionV4.security.configuration",
        qualifiedByName = "deserializeConfiguration"
    )
    @Mapping(target = "selectionRule", source = "planDefinitionV4.selectionRule")
    @Mapping(target = "status", source = "planDefinitionV4.status")
    @Mapping(target = "tags", source = "planDefinitionV4.tags")
    @Mapping(target = "mode", source = "planDefinitionV4.mode")
    @Mapping(target = "definitionVersion", constant = "V4")
    PlanV4 map(PlanWithFlows source);

    @Mapping(target = "status", source = "federatedPlanDefinition.status")
    @Mapping(target = "mode", source = "federatedPlanDefinition.mode")
    @Mapping(target = "definitionVersion", constant = "FEDERATED")
    PlanFederated mapFederated(io.gravitee.apim.core.plan.model.Plan source);

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
            if (entity.getDefinitionVersion() == DefinitionVersion.V4) {
                return new Plan(this.map((PlanEntity) entity));
            }
            return new Plan(this.mapFederated((PlanEntity) entity));
        } else {
            return new Plan(this.map((io.gravitee.rest.api.model.PlanEntity) entity));
        }
    }

    @Mapping(target = "validation", defaultValue = "MANUAL")
    @Mapping(target = "definitionVersion", constant = "V4")
    @Mapping(target = "planDefinitionHttpV4", source = "source", qualifiedByName = "mapToPlanDefinitionV4")
    io.gravitee.apim.core.plan.model.Plan map(CreatePlanV4 source);

    @Mapping(target = "security", source = "security.type", qualifiedByName = "toV2PlanSecurityType")
    @Mapping(target = "securityDefinition", source = "security.configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.rest.api.model.NewPlanEntity map(CreatePlanV2 plan);

    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Named("toPlanEntity")
    PlanEntity map(PlanV4 plan);

    @Mapping(target = "definitionVersion", constant = "V4")
    @Mapping(target = "type", constant = "API")
    @Mapping(target = "planDefinitionHttpV4", source = "plan", qualifiedByName = "mapPlanV4ToPlanDefinitionV4")
    @Mapping(target = "flows", qualifiedByName = "mapListToFlowHttpV4")
    PlanWithFlows toHttpPlanWithFlows(PlanV4 plan);

    @Named("toPlansWithFlows")
    Set<PlanWithFlows> toPlansWithFlows(Set<PlanV4> plans);

    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "mode", defaultValue = "STANDARD")
    @Named("mapPlanV4ToPlanDefinitionV4")
    io.gravitee.definition.model.v4.plan.Plan mapPlanV4ToPlanDefinitionV4(PlanV4 planV4);

    @Mapping(target = "security", qualifiedByName = "mapToPlanSecurityV4")
    io.gravitee.apim.core.api.model.crd.PlanCRD fromPlanCRD(PlanCRD plan);

    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    UpdatePlanEntity map(UpdatePlanV4 plan);

    @Mapping(target = "federatedPlanDefinition", source = "source", qualifiedByName = "mapToPlanDefinitionFederated")
    io.gravitee.apim.core.plan.model.Plan map(UpdatePlanFederated source);

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

    @Named("toV2PlanSecurityType")
    default io.gravitee.rest.api.model.PlanSecurityType toV2PlanSecurityType(PlanSecurityType securityType) {
        // PlanSecurityType is a common enum containing MTLS plan. Such security type is not supported by V2, so it is ignored during mapping
        if (Objects.isNull(securityType) || PlanSecurityType.MTLS.equals(securityType)) {
            return null;
        }
        return io.gravitee.rest.api.model.PlanSecurityType.valueOf(securityType.name());
    }

    @Mapping(source = "planSecurity.type", target = "security.type", qualifiedByName = "mapToPlanSecurityType")
    @Mapping(target = "mode", source = "planMode")
    @Mapping(source = "planSecurity.configuration", target = "security.configuration", qualifiedByName = "deserializeConfiguration")
    BasePlan map(GenericPlanEntity plan);

    Collection<BasePlan> mapToBasePlans(Set<GenericPlanEntity> plans);

    @Named("mapToPlanDefinitionV4")
    @Mapping(target = "security.type", qualifiedByName = "mapFromSecurityType")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "mode", defaultValue = "STANDARD")
    io.gravitee.definition.model.v4.plan.Plan mapToPlanDefinitionV4(CreatePlanV4 source);

    @Named("mapToPlanDefinitionFederated")
    @Mapping(target = "security.configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.federation.FederatedPlan mapToPlanDefinitionFederated(UpdatePlanFederated source);

    default List<FlowV4> computeFlows(PlanWithFlows source) {
        if (source.getDefinitionVersion() != DefinitionVersion.V4) {
            return null;
        }
        if (source.getApiType() == ApiType.NATIVE) {
            return FlowMapper.INSTANCE.mapFromNativeV4((List<NativeFlow>) source.getFlows());
        }
        return FlowMapper.INSTANCE.mapFromHttpV4((List<Flow>) source.getFlows());
    }
}
