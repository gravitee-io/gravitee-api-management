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

import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.metadata.model.Metadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MetadataAdapter {
    MetadataAdapter INSTANCE = Mappers.getMapper(MetadataAdapter.class);

    Metadata toEntity(io.gravitee.repository.management.model.Metadata source);

    io.gravitee.repository.management.model.Metadata toRepository(Metadata source);

    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "referenceType", expression = "java(io.gravitee.apim.core.metadata.model.Metadata.ReferenceType.API)")
    @Mapping(target = "referenceId", source = "apiId")
    Metadata toMetadata(ApiMetadata source);
}
