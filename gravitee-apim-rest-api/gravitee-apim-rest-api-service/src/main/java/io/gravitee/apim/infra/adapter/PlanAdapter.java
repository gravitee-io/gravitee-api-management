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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanMode;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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

    @Mapping(target = "apiId", source = "api")
    @Mapping(target = "mode", expression = "java(computeBasePlanEntityMode(plan))")
    @Mapping(target = "status", expression = "java(computeBasePlanEntityStatusV4(plan))")
    @Mapping(target = "security", expression = "java(computeBasePlanEntitySecurityV4(plan))")
    BasePlanEntity toEntityV4(Plan plan);

    @Mapping(target = "paths", expression = "java(computeBasePlanEntityPaths(plan))")
    @Mapping(target = "status", expression = "java(computeBasePlanEntityStatusV2(plan))")
    @Mapping(target = "security", expression = "java(computeBasePlanEntitySecurityV2(plan))")
    io.gravitee.rest.api.model.BasePlanEntity toEntityV2(Plan plan);

    default GenericPlanEntity toGenericEntity(Plan plan, DefinitionVersion definitionVersion) {
        return definitionVersion == DefinitionVersion.V4 ? PlanAdapter.INSTANCE.toEntityV4(plan) : PlanAdapter.INSTANCE.toEntityV2(plan);
    }

    @Named("computeBasePlanEntityMode")
    default PlanMode computeBasePlanEntityMode(Plan plan) {
        return plan.getMode() != null ? PlanMode.valueOf(plan.getMode().name()) : PlanMode.STANDARD;
    }

    @Named("computeBasePlanEntityStatusV4")
    default PlanStatus computeBasePlanEntityStatusV4(Plan plan) {
        // Ensure backward compatibility
        return plan.getStatus() != null ? PlanStatus.valueOf(plan.getStatus().name()) : PlanStatus.PUBLISHED;
    }

    @Named("computeBasePlanEntityStatusV2")
    default io.gravitee.rest.api.model.PlanStatus computeBasePlanEntityStatusV2(Plan plan) {
        // Ensure backward compatibility
        return plan.getStatus() != null
            ? io.gravitee.rest.api.model.PlanStatus.valueOf(plan.getStatus().name())
            : io.gravitee.rest.api.model.PlanStatus.PUBLISHED;
    }

    @Named("computeBasePlanEntitySecurityV4")
    default PlanSecurity computeBasePlanEntitySecurityV4(Plan plan) {
        if (Plan.PlanMode.PUSH != plan.getMode()) {
            return PlanSecurity
                .builder()
                .type(PlanSecurityType.valueOf(plan.getSecurity().name()).getLabel())
                .configuration(plan.getSecurityDefinition())
                .build();
        } else {
            return null;
        }
    }

    @Named("computeBasePlanEntitySecurityV2")
    default io.gravitee.rest.api.model.PlanSecurityType computeBasePlanEntitySecurityV2(Plan plan) {
        return plan.getSecurity() != null
            ? io.gravitee.rest.api.model.PlanSecurityType.valueOf(plan.getSecurity().name())
            : io.gravitee.rest.api.model.PlanSecurityType.API_KEY;
    }

    @Named("computeBasePlanEntityPaths")
    default Map<String, List<Rule>> computeBasePlanEntityPaths(Plan plan) {
        if (plan.getDefinition() != null && !plan.getDefinition().isEmpty()) {
            try {
                return GraviteeJacksonMapper.getInstance().readValue(plan.getDefinition(), new TypeReference<Map<String, List<Rule>>>() {});
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating policy definition", ioe);
                return null;
            }
        } else {
            return null;
        }
    }
}
