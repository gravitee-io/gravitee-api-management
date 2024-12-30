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

import io.gravitee.apim.core.api.model.import_definition.GraviteeDefinition;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.rest.api.model.v4.api.ExportApiEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { ApiAdapter.class, PlanAdapter.class, MemberAdapter.class, MetadataAdapter.class, PageAdapter.class })
public interface GraviteeDefinitionAdapter {
    GraviteeDefinitionAdapter INSTANCE = Mappers.getMapper(GraviteeDefinitionAdapter.class);
    Logger logger = LoggerFactory.getLogger(GraviteeDefinitionAdapter.class);

    @Mapping(target = "api", source = "apiEntity")
    GraviteeDefinition map(ExportApiEntity source);
}
