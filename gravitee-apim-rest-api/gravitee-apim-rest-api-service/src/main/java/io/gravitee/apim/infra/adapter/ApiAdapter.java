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

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import java.io.IOException;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(uses = { PlanAdapter.class })
public interface ApiAdapter {
    Logger LOGGER = LoggerFactory.getLogger(ApiAdapter.class);
    ApiAdapter INSTANCE = Mappers.getMapper(ApiAdapter.class);

    @Mapping(target = "definitionContext.mode", source = "mode")
    @Mapping(target = "definitionContext.origin", source = "origin")
    @Mapping(target = "apiDefinitionV4", expression = "java(deserializeApiDefinitionV4(source))")
    @Mapping(target = "apiDefinition", expression = "java(deserializeApiDefinitionV2(source))")
    Api toCoreModel(io.gravitee.repository.management.model.Api source);

    Stream<Api> toCoreModelStream(Stream<io.gravitee.repository.management.model.Api> source);

    @Mapping(target = "mode", source = "definitionContext.mode")
    @Mapping(target = "origin", source = "definitionContext.origin")
    @Mapping(target = "definition", expression = "java(serializeApiDefinition(source))")
    io.gravitee.repository.management.model.Api toRepository(Api source);

    Stream<io.gravitee.repository.management.model.Api> toRepositoryStream(Stream<Api> source);

    @Mapping(target = "apiVersion", source = "version")
    @Mapping(target = "tags", source = "source.apiDefinitionV4.tags")
    @Mapping(target = "listeners", source = "source.apiDefinitionV4.listeners")
    @Mapping(target = "endpointGroups", source = "source.apiDefinitionV4.endpointGroups")
    @Mapping(target = "analytics", source = "source.apiDefinitionV4.analytics")
    @Mapping(target = "flowExecution", source = "source.apiDefinitionV4.flowExecution")
    @Mapping(target = "flows", source = "source.apiDefinitionV4.flows")
    NewApiEntity toNewApiEntity(Api source);

    @Mapping(target = "apiVersion", source = "version")
    io.gravitee.definition.model.v4.Api toApiDefinition(ApiCRD source);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    UpdateApiEntity toUpdateApiEntity(ApiCRD api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    ApiEntity toApiEntity(ApiCRD api);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    ApiEntity toApiEntity(Api api);

    @Mapping(source = "state", target = "lifecycleState")
    @Mapping(source = "lifecycleState", target = "apiLifecycleState")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api fromApiEntity(GenericApiEntity apiEntity);

    default io.gravitee.definition.model.v4.Api deserializeApiDefinitionV4(io.gravitee.repository.management.model.Api api) {
        if (api.getDefinition() == null) {
            // This can happen when filtering the definition using ApiFieldFilter
            return null;
        }

        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            try {
                return GraviteeJacksonMapper.getInstance().readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while deserializing V4 API definition", ioe);
                return null;
            }
        }

        return null;
    }

    default io.gravitee.definition.model.Api deserializeApiDefinitionV2(io.gravitee.repository.management.model.Api api) {
        if (api.getDefinition() == null) {
            // This can happen when filtering the definition using ApiFieldFilter
            return null;
        }

        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            try {
                return GraviteeJacksonMapper.getInstance().readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while deserializing V2 API definition", ioe);
                return null;
            }
        }

        return null;
    }

    default String serializeApiDefinition(Api api) {
        if (api.getDefinitionVersion() == DefinitionVersion.V4) {
            try {
                return GraviteeJacksonMapper.getInstance().writeValueAsString(api.getApiDefinitionV4());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while serializing V4 API definition", ioe);
                return null;
            }
        } else {
            try {
                return GraviteeJacksonMapper.getInstance().writeValueAsString(api.getApiDefinition());
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while serializing V2 API definition", ioe);
                return null;
            }
        }
    }
}
