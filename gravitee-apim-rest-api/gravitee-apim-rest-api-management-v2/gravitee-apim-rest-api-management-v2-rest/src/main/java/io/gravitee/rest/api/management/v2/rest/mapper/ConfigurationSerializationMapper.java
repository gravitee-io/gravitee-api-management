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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ConfigurationSerializationMapper {
    ConfigurationSerializationMapper INSTANCE = Mappers.getMapper(ConfigurationSerializationMapper.class);

    @Named("deserializeConfiguration")
    default String deserializeConfiguration(Object configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration instanceof LinkedHashMap) {
            ObjectMapper mapper = new GraviteeMapper();
            try {
                JsonNode jsonNode = mapper.valueToTree(configuration);
                return mapper.writeValueAsString(jsonNode);
            } catch (IllegalArgumentException | JsonProcessingException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse connector configuration " + e);
            }
        } else {
            return configuration.toString();
        }
    }

    @Named("serializeConfiguration")
    default Object serializeConfiguration(String configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }
        try {
            ObjectMapper mapper = new GraviteeMapper();
            return mapper.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            throw new TechnicalManagementException("An error occurred while trying to parse connector configuration");
        }
    }
}
