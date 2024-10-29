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
package io.gravitee.apim.infra.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.federation.FederatedPlan;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface PlanAdapter {
    Logger LOGGER = LoggerFactory.getLogger(PlanAdapter.class);
    PlanAdapter INSTANCE = Mappers.getMapper(PlanAdapter.class);

    @Mapping(source = "api", target = "apiId")
    @Mapping(target = "definitionVersion", defaultValue = "V2")
    @Mapping(target = "planDefinitionHttpV4", expression = "java(deserializeDefinitionHttpV4(plan))")
    @Mapping(target = "planDefinitionNativeV4", expression = "java(deserializeDefinitionNativeV4(plan))")
    @Mapping(target = "planDefinitionV2", expression = "java(deserializeDefinitionV2(plan))")
    @Mapping(target = "federatedPlanDefinition", expression = "java(deserializeDefinitionFederated(plan))")
    Plan fromRepository(io.gravitee.repository.management.model.Plan plan);

    @Mapping(source = "apiId", target = "api")
    @Mapping(target = "security", source = "planSecurity", qualifiedByName = "computeRepositorySecurityType")
    @Mapping(target = "securityDefinition", source = "planSecurity.configuration")
    @Mapping(target = "definition", expression = "java(serializeDefinition(source))")
    @Mapping(target = "mode", source = "planMode")
    @Mapping(target = "selectionRule", expression = "java(serializeSelectionRule(source))")
    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "tags", expression = "java(serializeTags(source))")
    io.gravitee.repository.management.model.Plan toRepository(Plan source);

    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "security", source = "planSecurity")
    @Mapping(target = "mode", source = "planMode")
    PlanEntity toEntityV4(Plan plan);

    @Mapping(target = "api", source = "apiId")
    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "security", source = "planSecurity", conditionQualifiedByName = "mapPlanSecurityTypeV2")
    @Mapping(target = "securityDefinition", source = "planSecurity.configuration")
    io.gravitee.rest.api.model.PlanEntity toEntityV2(Plan source);

    NewPlanEntity entityToNewPlanEntity(PlanEntity entity);

    @Mapping(target = "security", source = "planSecurity")
    @Mapping(target = "mode", source = "planMode")
    @Mapping(target = "selectionRule", expression = "java(serializeSelectionRule(source))")
    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "tags", expression = "java(serializeTags(source))")
    PlanCRD toCRD(Plan source);

    PlanEntity toEntityV4(PlanCRD source);
    io.gravitee.definition.model.v4.plan.Plan toApiDefinition(PlanCRD source);

    @Mapping(target = "security", expression = "java(computeBasePlanEntitySecurityV4(source))")
    io.gravitee.definition.model.v4.plan.Plan toPlanDefinitionHttpV4(io.gravitee.repository.management.model.Plan source);

    @Mapping(target = "security", expression = "java(computeBasePlanEntitySecurityV4(source))")
    io.gravitee.definition.model.v4.nativeapi.NativePlan toPlanDefinitionNativeV4(io.gravitee.repository.management.model.Plan source);

    @Mapping(target = "paths", expression = "java(computeBasePlanEntityPaths(source))")
    @Mapping(target = "security", qualifiedByName = "serializeV2PlanSecurityType")
    io.gravitee.definition.model.Plan toPlanDefinitionV2(io.gravitee.repository.management.model.Plan source);

    default io.gravitee.definition.model.v4.plan.Plan deserializeDefinitionHttpV4(io.gravitee.repository.management.model.Plan source) {
        if (source.getDefinitionVersion() != DefinitionVersion.V4 || source.getApiType() == ApiType.NATIVE) {
            return null;
        }

        return toPlanDefinitionHttpV4(source);
    }

    default io.gravitee.definition.model.v4.nativeapi.NativePlan deserializeDefinitionNativeV4(
        io.gravitee.repository.management.model.Plan source
    ) {
        if (source.getDefinitionVersion() != DefinitionVersion.V4 || source.getApiType() != ApiType.NATIVE) {
            return null;
        }

        return toPlanDefinitionNativeV4(source);
    }

    default io.gravitee.definition.model.Plan deserializeDefinitionV2(io.gravitee.repository.management.model.Plan source) {
        if (source.getDefinitionVersion() != null) {
            return null;
        }

        return toPlanDefinitionV2(source);
    }

    default FederatedPlan deserializeDefinitionFederated(io.gravitee.repository.management.model.Plan source) {
        if (source.getDefinitionVersion() != DefinitionVersion.FEDERATED) {
            return null;
        }

        try {
            return GraviteeJacksonMapper
                .getInstance()
                .readValue(source.getDefinition(), io.gravitee.definition.model.federation.FederatedPlan.class);
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while deserializing Federated Plan definition", ioe);
            return null;
        }
    }

    default Map<String, io.gravitee.definition.model.v4.plan.Plan> toApiDefinition(Map<String, PlanCRD> source) {
        return source
            .values()
            .stream()
            .map(planCRD -> Map.entry(planCRD.getId(), PlanAdapter.INSTANCE.toApiDefinition(planCRD)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    default Set<PlanEntity> toPlanEntityV4(Map<String, PlanCRD> source) {
        return source.values().stream().map(PlanAdapter.INSTANCE::toEntityV4).collect(Collectors.toSet());
    }

    @Named("computeBasePlanEntityMode")
    default PlanMode computeBasePlanEntityMode(io.gravitee.repository.management.model.Plan plan) {
        return plan.getMode() != null ? PlanMode.valueOf(plan.getMode().name()) : PlanMode.STANDARD;
    }

    @Named("computeBasePlanEntityStatusV4")
    default PlanStatus computeBasePlanEntityStatusV4(io.gravitee.repository.management.model.Plan plan) {
        // Ensure backward compatibility
        return plan.getStatus() != null ? PlanStatus.valueOf(plan.getStatus().name()) : PlanStatus.PUBLISHED;
    }

    @Named("computeBasePlanEntityStatusV2")
    default io.gravitee.rest.api.model.PlanStatus computeBasePlanEntityStatusV2(io.gravitee.repository.management.model.Plan plan) {
        // Ensure backward compatibility
        return plan.getStatus() != null
            ? io.gravitee.rest.api.model.PlanStatus.valueOf(plan.getStatus().name())
            : io.gravitee.rest.api.model.PlanStatus.PUBLISHED;
    }

    @Named("computeBasePlanEntitySecurityV4")
    default PlanSecurity computeBasePlanEntitySecurityV4(io.gravitee.repository.management.model.Plan plan) {
        if (io.gravitee.repository.management.model.Plan.PlanMode.PUSH != plan.getMode()) {
            return PlanSecurity
                .builder()
                .type(PlanSecurityType.valueOf(plan.getSecurity().name()).getLabel())
                .configuration(plan.getSecurityDefinition())
                .build();
        } else {
            return null;
        }
    }

    default io.gravitee.rest.api.model.PlanSecurityType mapPlanSecurityTypeV2(PlanSecurity planSecurity) {
        return planSecurity != null
            ? io.gravitee.rest.api.model.PlanSecurityType.valueOf(PlanSecurityType.valueOfLabel(planSecurity.getType()).name())
            : io.gravitee.rest.api.model.PlanSecurityType.API_KEY;
    }

    default PlanSecurityType mapPlanSecurityTypeV4(PlanSecurity planSecurity) {
        return planSecurity != null ? PlanSecurityType.valueOfLabel(planSecurity.getType()) : null;
    }

    @Named("computeBasePlanEntityPaths")
    default Map<String, List<Rule>> computeBasePlanEntityPaths(io.gravitee.repository.management.model.Plan plan) {
        if (plan.getDefinition() != null && !plan.getDefinition().isEmpty()) {
            try {
                return GraviteeJacksonMapper.getInstance().readValue(plan.getDefinition(), new TypeReference<>() {});
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating policy definition", ioe);
                return null;
            }
        } else {
            return null;
        }
    }

    default String serializeDefinition(Plan source) {
        return switch (source.getDefinitionVersion()) {
            case V4 -> null;
            case FEDERATED -> serializeFederatedPlan(source.getFederatedPlanDefinition());
            default -> serializeV2PlanPaths(source.getPlanDefinitionV2());
        };
    }

    default String serializeFederatedPlan(FederatedPlan source) {
        try {
            return GraviteeJacksonMapper.getInstance().writeValueAsString(source);
        } catch (IOException ioe) {
            LOGGER.error("Unexpected error while serializing federated plan definition", ioe);
            return null;
        }
    }

    default String serializeV2PlanPaths(io.gravitee.definition.model.Plan plan) {
        if (plan != null && plan.getPaths() != null && !plan.getPaths().isEmpty()) {
            try {
                return GraviteeJacksonMapper.getInstance().writeValueAsString(plan.getPaths());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while serializing v2 plan paths", ioe);
                return null;
            }
        } else {
            return null;
        }
    }

    @Named("serializeV2PlanSecurityType")
    default String serializeV2PlanSecurityType(io.gravitee.repository.management.model.Plan.PlanSecurityType planSecurityType) {
        if (planSecurityType != null) {
            return PlanSecurityType.valueOfLabel(planSecurityType.name()).getLabel();
        }
        return null;
    }

    @Named("serializeSelectionRule")
    default String serializeSelectionRule(Plan source) {
        return switch (source.getDefinitionVersion()) {
            case V4 -> source.getPlanDefinitionV4().getSelectionRule();
            case V1, V2 -> source.getPlanDefinitionV2().getSelectionRule();
            case FEDERATED -> null;
        };
    }

    @Named("serializeTags")
    default Set<String> serializeTags(Plan source) {
        return switch (source.getDefinitionVersion()) {
            case V4 -> source.getPlanDefinitionV4().getTags();
            case V1, V2 -> source.getPlanDefinitionV2().getTags();
            case FEDERATED -> null;
        };
    }

    @Named("computeRepositorySecurityType")
    default io.gravitee.repository.management.model.Plan.PlanSecurityType computeRepositorySecurityType(PlanSecurity planSecurity) {
        if (planSecurity != null) {
            PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(planSecurity.getType());
            return io.gravitee.repository.management.model.Plan.PlanSecurityType.valueOf(planSecurityType.name());
        }
        return null;
    }
}
