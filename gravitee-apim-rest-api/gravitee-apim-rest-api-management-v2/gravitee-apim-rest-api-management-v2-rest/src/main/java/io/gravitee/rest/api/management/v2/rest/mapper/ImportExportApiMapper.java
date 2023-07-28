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

import io.gravitee.rest.api.management.v2.rest.model.ExportApiV4;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.Metadata;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ApiMapper.class, DateMapper.class, MemberMapper.class, MetadataMapper.class, PageMapper.class, PlanMapper.class })
public interface ImportExportApiMapper {
    ImportExportApiMapper INSTANCE = Mappers.getMapper(ImportExportApiMapper.class);

    @Mapping(target = "apiPicture", source = "apiEntity.picture")
    @Mapping(target = "apiBackground", source = "apiEntity.background")
    @Mapping(target = "api", source = "apiEntity")
    ExportApiV4 map(ExportApiEntity exportApiEntityV4);

    @Mapping(target = "apiEntity", expression = "java(buildApiEntity(exportApiV4))")
    @Mapping(target = "members", expression = "java(buildMembers(exportApiV4))")
    @Mapping(target = "metadata", expression = "java(buildMetadata(exportApiV4))")
    ExportApiEntity map(ExportApiV4 exportApiV4);

    @Mapping(target = "type", constant = "USER")
    @Mapping(target = "referenceType", constant = "API")
    @Mapping(target = "referenceId", expression = "java(apiId)")
    MemberEntity map(Member member, String apiId);

    default ApiEntity buildApiEntity(ExportApiV4 exportApiV4) {
        final ApiEntity apiEntity = ApiMapper.INSTANCE.map(exportApiV4.getApi());
        apiEntity.setPicture(exportApiV4.getApiPicture());
        apiEntity.setBackground(exportApiV4.getApiBackground());
        return apiEntity;
    }

    default Set<MemberEntity> buildMembers(ExportApiV4 exportApiV4) {
        return exportApiV4.getMembers().stream().map(member -> map(member, exportApiV4.getApi().getId())).collect(Collectors.toSet());
    }

    @Mapping(target = "apiId", expression = "java(apiId)")
    ApiMetadataEntity map(Metadata metadata, String apiId);

    default Set<ApiMetadataEntity> buildMetadata(ExportApiV4 exportApiV4) {
        return exportApiV4.getMetadata().stream().map(metadata -> map(metadata, exportApiV4.getApi().getId())).collect(Collectors.toSet());
    }
}
