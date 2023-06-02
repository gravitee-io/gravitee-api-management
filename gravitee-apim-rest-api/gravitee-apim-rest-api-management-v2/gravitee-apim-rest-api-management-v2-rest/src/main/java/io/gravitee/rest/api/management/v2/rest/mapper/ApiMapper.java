/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.management.v2.rest.utils.ManagementApiLinkHelper;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import jakarta.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
    uses = {
        DateMapper.class,
        DefinitionContextMapper.class,
        EndpointMapper.class,
        EntrypointMapper.class,
        FlowMapper.class,
        ListenerMapper.class,
        PropertiesMapper.class,
        ResourceMapper.class,
    }
)
public interface ApiMapper {
    Logger logger = LoggerFactory.getLogger(ApiMapper.class);
    ApiMapper INSTANCE = Mappers.getMapper(ApiMapper.class);

    // Api
    default Api convert(GenericApiEntity apiEntity, UriInfo uriInfo) {
        if (apiEntity == null) {
            return null;
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V4) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(this.convert((ApiEntity) apiEntity, uriInfo));
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V2) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(
                this.map((io.gravitee.rest.api.model.api.ApiEntity) apiEntity, uriInfo)
            );
        }
        if (apiEntity.getDefinitionVersion() == io.gravitee.definition.model.DefinitionVersion.V1) {
            return new io.gravitee.rest.api.management.v2.rest.model.Api(
                this.mapV1((io.gravitee.rest.api.model.api.ApiEntity) apiEntity, uriInfo)
            );
        }
        return null;
    }

    default List<Api> convert(List<GenericApiEntity> apiEntities, UriInfo uriInfo) {
        var result = new ArrayList<Api>();
        apiEntities.forEach(api -> {
            try {
                result.add(this.convert(api, uriInfo));
            } catch (Exception e) {
                // Ignore APIs throwing conversion issues in the list
                // As v4 was out there in alpha version, we still want to build the list event if some APIs cannot be converted
                logger.error("Unable to convert API {}", api.getId(), e);
            }
        });
        return result;
    }

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    @Mapping(target = "links", expression = "java(computeLinksFromApi(apiEntity, uriInfo))")
    ApiV4 convert(ApiEntity apiEntity, UriInfo uriInfo);

    @Mapping(target = "listeners", qualifiedByName = "fromListeners")
    @Mapping(target = "links", ignore = true)
    ApiV4 convert(ApiEntity apiEntity);

    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    ApiEntity convert(ApiV4 api);

    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    NewApiEntity convert(CreateApiV4 api);

    @Mapping(target = "links", expression = "java(computeLinksFromApi(apiEntity, uriInfo))")
    ApiV2 map(io.gravitee.rest.api.model.api.ApiEntity apiEntity, UriInfo uriInfo);

    @Mapping(target = "paths", qualifiedByName = "fromPaths")
    @Mapping(target = "links", expression = "java(computeLinksFromApi(apiEntity, uriInfo))")
    io.gravitee.rest.api.management.v2.rest.model.ApiV1 mapV1(io.gravitee.rest.api.model.api.ApiEntity apiEntity, UriInfo uriInfo);

    // UpdateApi
    @Mapping(target = "listeners", qualifiedByName = "toListeners")
    @Mapping(target = "id", expression = "java(apiId)")
    UpdateApiEntity map(UpdateApiV4 updateApi, String apiId);

    io.gravitee.rest.api.model.api.UpdateApiEntity map(UpdateApiV2 updateApi);

    // DefinitionVersion
    io.gravitee.definition.model.DefinitionVersion map(DefinitionVersion definitionVersion);

    // Rule
    Rule map(io.gravitee.definition.model.Rule rule);
    List<Rule> mapRuleList(List<io.gravitee.definition.model.Rule> rule);

    // ResponseTemplate
    Map<String, ResponseTemplate> convertFromResponseTemplateModel(
        Map<String, io.gravitee.definition.model.ResponseTemplate> responseTemplate
    );

    default Map<String, Map<String, ResponseTemplate>> mapFromModel(
        Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> value
    ) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, convertFromResponseTemplateModel(map)));
        return convertedMap;
    }

    Map<String, io.gravitee.definition.model.ResponseTemplate> convertToResponseTemplateModel(
        Map<String, ResponseTemplate> responseTemplate
    );

    default Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> mapToModel(
        Map<String, Map<String, ResponseTemplate>> value
    ) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, convertToResponseTemplateModel(map)));
        return convertedMap;
    }

    @Named("fromPaths")
    default Map<String, List<Rule>> fromPath(Map<String, List<io.gravitee.definition.model.Rule>> paths) {
        if (Objects.isNull(paths)) {
            return new HashMap<>();
        }
        return paths
            .entrySet()
            .stream()
            .map(entry -> Pair.of(entry.getKey(), this.mapRuleList(entry.getValue())))
            .collect(Collectors.toMap(m -> m.getKey(), m -> m.getValue()));
    }

    @Named("computeLinksFromApi")
    default ApiLinks computeLinksFromApi(GenericApiEntity api, UriInfo uriInfo) {
        return new ApiLinks()
            .pictureUrl(ManagementApiLinkHelper.apiPictureURL(uriInfo.getBaseUriBuilder(), api))
            .backgroundUrl(ManagementApiLinkHelper.apiBackgroundURL(uriInfo.getBaseUriBuilder(), api));
    }
}
