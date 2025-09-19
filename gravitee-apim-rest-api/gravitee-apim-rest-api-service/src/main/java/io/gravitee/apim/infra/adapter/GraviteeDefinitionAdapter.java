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

import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.PlanExport;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ApiAdapter.class, PlanAdapter.class, MemberAdapter.class, MetadataAdapter.class, PageAdapter.class })
public interface GraviteeDefinitionAdapter {
    GraviteeDefinitionAdapter INSTANCE = Mappers.getMapper(GraviteeDefinitionAdapter.class);
    Logger logger = LoggerFactory.getLogger(GraviteeDefinitionAdapter.class);

    @Mapping(target = "api", expression = "java(mapApi(source.getApiEntity()))")
    @Mapping(target = "plans", expression = "java(mapPlans(source.getPlans()))")
    GraviteeDefinition map(ExportApiEntity source);

    default ApiExport mapApi(GenericApiEntity source) {
        if (source instanceof ApiEntity v4) {
            return mapApiEntity(v4);
        }
        return mapGenericApiEntity(source);
    }

    ApiExport mapApiEntity(ApiEntity source);

    ApiExport mapGenericApiEntity(GenericApiEntity source);

    default Set<PlanExport> mapPlans(Set<? extends GenericPlanEntity> source) {
        return source
            .stream()
            .map(plan -> {
                if (plan instanceof PlanEntity v4) {
                    return mapPlanEntity(v4);
                }
                return mapGenericApiEntity(plan);
            })
            .collect(Collectors.toSet());
    }

    @Mapping(target = "mode", source = "planMode")
    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getPlanSecurity()))")
    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "type", source = "planType")
    @Mapping(target = "validation", source = "planValidation")
    PlanExport mapPlanEntity(PlanEntity source);

    @Mapping(target = "mode", source = "planMode")
    @Mapping(target = "security", expression = "java(mapPlanSecurity(source.getPlanSecurity()))")
    @Mapping(target = "status", source = "planStatus")
    @Mapping(target = "type", source = "planType")
    @Mapping(target = "validation", source = "planValidation")
    PlanExport mapGenericApiEntity(GenericPlanEntity source);

    /**
     * Map a PlanSecurity.
     * <p>
     *     This is required to ensure the PlanSecurityType as the same form as the one in the export.
     * </p>
     * @param source the source PlanSecurity
     * @return the mapped PlanSecurity
     */
    default PlanSecurity mapPlanSecurity(io.gravitee.definition.model.v4.plan.PlanSecurity source) {
        return PlanSecurity.builder()
            .type(PlanSecurityType.valueOfLabel(source.getType()).name())
            .configuration(source.getConfiguration())
            .build();
    }
}
