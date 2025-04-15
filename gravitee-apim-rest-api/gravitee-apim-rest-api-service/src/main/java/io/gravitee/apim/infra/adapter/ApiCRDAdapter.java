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

import static com.google.common.base.Functions.identity;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PageCRD;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.member.model.crd.MemberCRD;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.RandomStringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ApiCRDAdapter {
    ApiCRDAdapter INSTANCE = Mappers.getMapper(ApiCRDAdapter.class);
    Logger logger = LoggerFactory.getLogger(ApiCRDAdapter.class);

    @Mapping(target = "version", source = "apiEntity.apiVersion")
    @Mapping(target = "metadata", source = "exportEntity.metadata")
    @Mapping(target = "definitionContext", ignore = true)
    @Mapping(target = "plans", expression = "java(mapPlans(exportEntity))")
    @Mapping(target = "pages", expression = "java(mapPages(exportEntity))")
    @Mapping(target = "members", expression = "java(mapMembers(exportEntity))")
    @Mapping(target = "notifyMembers", expression = "java(!exportEntity.getApiEntity().isDisableMembershipNotifications())")
    ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, ApiEntity apiEntity);

    @Mapping(target = "version", source = "apiEntity.apiVersion")
    @Mapping(target = "metadata", source = "exportEntity.metadata")
    @Mapping(target = "definitionContext", ignore = true)
    @Mapping(target = "plans", expression = "java(mapPlans(exportEntity))")
    @Mapping(target = "pages", expression = "java(mapPages(exportEntity))")
    @Mapping(target = "members", expression = "java(mapMembers(exportEntity))")
    @Mapping(target = "notifyMembers", expression = "java(!exportEntity.getApiEntity().isDisableMembershipNotifications())")
    ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, NativeApiEntity apiEntity);

    default ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, GenericApiEntity apiEntity) {
        if (apiEntity instanceof ApiEntity) {
            return toCRDSpec(exportEntity, (ApiEntity) apiEntity);
        } else if (apiEntity instanceof NativeApiEntity) {
            return toCRDSpec(exportEntity, (NativeApiEntity) apiEntity);
        }
        return null;
    }

    PlanCRD toCRDPlan(PlanEntity planEntity);
    PlanCRD toCRDPlan(NativePlanEntity planEntity);

    default PlanCRD toCRDPlan(GenericPlanEntity genericPlanEntity) {
        if (genericPlanEntity instanceof PlanEntity planEntity) {
            return toCRDPlan(planEntity);
        } else if (genericPlanEntity instanceof NativePlanEntity nativePlanEntity) {
            return toCRDPlan(nativePlanEntity);
        }
        return null;
    }

    default Map<String, PlanCRD> mapPlans(ExportApiEntity definition) {
        var plansMap = new HashMap<String, PlanCRD>();
        var nonClosedPlans = definition.getPlans().stream().filter(plan -> !plan.isClosed()).toList();
        for (var plan : nonClosedPlans) {
            var key = plansMap.containsKey(plan.getName()) ? randomize(plan.getName()) : plan.getName();
            plansMap.put(key, toCRDPlan(plan));
        }
        return plansMap;
    }

    @Mapping(target = "source.configurationMap", source = "source.configuration", qualifiedByName = "deserializeConfig")
    PageCRD toCRDPage(PageEntity pageEntity);

    default Map<String, PageCRD> mapPages(ExportApiEntity definition) {
        return definition.getPages() != null
            ? definition.getPages().stream().map(this::toCRDPage).collect(toMap(this::pageKey, identity()))
            : null;
    }

    default String pageKey(PageCRD page) {
        return page.getName() == null ? page.getId() : page.getName();
    }

    default Set<MemberCRD> mapMembers(ExportApiEntity definition) {
        return definition.getMembers() != null
            ? definition
                .getMembers()
                .stream()
                .map(me -> new MemberCRD(me.getId(), null, null, me.getRoles().get(0).getName()))
                .collect(Collectors.toSet())
            : null;
    }

    @Named("deserializeConfig")
    default Map<String, Object> deserializeConfig(String configuration) {
        if (Objects.isNull(configuration)) {
            return Map.of();
        }

        ObjectMapper mapper = new GraviteeMapper();
        try {
            return mapper.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            logger.debug("Cannot parse configuration as LinkedHashMap: " + configuration);
        }

        return Map.of();
    }

    private static String randomize(String strToRandomize) {
        return strToRandomize + "-" + RandomStringUtils.randomNumeric(8);
    }
}
