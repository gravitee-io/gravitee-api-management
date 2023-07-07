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

import io.gravitee.rest.api.management.v2.rest.model.Resource;
import io.gravitee.rest.api.management.v2.rest.model.ResponseTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ResponseTemplateMapper {
    ResponseTemplateMapper INSTANCE = Mappers.getMapper(ResponseTemplateMapper.class);

    // ResponseTemplate
    Map<String, ResponseTemplate> mapToApiModel(Map<String, io.gravitee.definition.model.ResponseTemplate> responseTemplate);

    default Map<String, Map<String, ResponseTemplate>> mapResponseTemplateToApiModel(
        Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> value
    ) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, mapToApiModel(map)));
        return convertedMap;
    }

    Map<String, io.gravitee.definition.model.ResponseTemplate> mapToEntity(Map<String, ResponseTemplate> responseTemplate);

    default Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> mapResponseTemplateToEntity(
        Map<String, Map<String, ResponseTemplate>> value
    ) {
        if (Objects.isNull(value)) {
            return null;
        }
        Map<String, Map<String, io.gravitee.definition.model.ResponseTemplate>> convertedMap = new HashMap<>();
        value.forEach((key, map) -> convertedMap.put(key, mapToEntity(map)));
        return convertedMap;
    }
}
