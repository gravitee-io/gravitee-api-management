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

import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.rest.api.management.v2.rest.model.ApiV4;
import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.Metadata;
import io.gravitee.rest.api.management.v2.rest.model.PlanV4;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ApiMapper.class, DateMapper.class, MemberMapper.class, MetadataMapper.class, PageMapper.class, PlanMapper.class })
public interface ImportExportApiMapper {
    ImportExportApiMapper INSTANCE = Mappers.getMapper(ImportExportApiMapper.class);

    @Mapping(target = "apiPicture", source = "apiEntity.picture")
    @Mapping(target = "apiBackground", source = "apiEntity.background")
    @Mapping(target = "api", source = "apiEntity")
    @Mapping(target = "plans", source = "plans", qualifiedByName = "toPlanV4Nullable")
    ExportApiV4 map(ExportApiEntity exportApiEntityV4);

    @Mapping(target = "apiExport", expression = "java(buildApiExport(exportApiV4))")
    ImportDefinition toImportDefinition(ExportApiV4 exportApiV4);

    @Mapping(target = "type", constant = "USER")
    @Mapping(target = "referenceType", constant = "API")
    @Mapping(target = "referenceId", expression = "java(apiId)")
    MemberEntity map(Member member, String apiId);

    default ApiExport buildApiExport(ExportApiV4 exportApiV4) {
        final ApiExport apiExport = ApiMapper.INSTANCE.toApiExport(exportApiV4.getApi());
        apiExport.setPicture(exportApiV4.getApiPicture());
        apiExport.setBackground(exportApiV4.getApiBackground());
        return apiExport;
    }

    @Named("toPlanV4Nullable")
    default Set<PlanV4> toPlanV4Nullable(Set<? extends GenericPlanEntity> plans) {
        if (plans == null) {
            return null;
        }
        return PlanMapper.INSTANCE.map(plans);
    }

    @Mapping(target = "apiId", expression = "java(apiId)")
    ApiMetadataEntity map(Metadata metadata, String apiId);
}
