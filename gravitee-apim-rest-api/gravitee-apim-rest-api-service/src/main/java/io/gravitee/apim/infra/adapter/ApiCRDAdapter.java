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

import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.api.model.crd.PlanCRD;
import io.gravitee.apim.core.documentation.model.Page;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ApiCRDAdapter {
    ApiCRDAdapter INSTANCE = Mappers.getMapper(ApiCRDAdapter.class);

    @Mapping(target = "version", source = "apiEntity.apiVersion")
    @Mapping(target = "metadata", source = "exportEntity.metadata")
    @Mapping(target = "definitionContext", ignore = true)
    @Mapping(target = "plans", expression = "java(mapPlans(exportEntity))")
    @Mapping(target = "pages", expression = "java(mapPages(exportEntity))")
    ApiCRDSpec toCRDSpec(ExportApiEntity exportEntity, ApiEntity apiEntity);

    PlanCRD toCRDPlan(PlanEntity planEntity);

    default Map<String, PlanCRD> mapPlans(ExportApiEntity definition) {
        return definition.getPlans().stream().map(this::toCRDPlan).collect(toMap(PlanCRD::getName, identity()));
    }

    Page toCRDPage(PageEntity pageEntity);

    default Map<String, Page> mapPages(ExportApiEntity definition) {
        return definition.getPages() != null
            ? definition.getPages().stream().map(this::toCRDPage).collect(toMap(Page::getName, identity()))
            : null;
    }
}
