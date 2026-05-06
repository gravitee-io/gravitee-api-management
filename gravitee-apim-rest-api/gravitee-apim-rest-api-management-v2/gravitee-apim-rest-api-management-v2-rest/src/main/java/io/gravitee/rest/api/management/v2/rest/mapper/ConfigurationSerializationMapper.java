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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;

@Mapper
public interface ConfigurationSerializationMapper {
    ConfigurationSerializationMapper INSTANCE = Mappers.getMapper(ConfigurationSerializationMapper.class);
    Logger log = NodeLoggerFactory.getLogger(ConfigurationSerializationMapper.class);
    GraviteeMapper JSON_MAPPER = new GraviteeMapper();

    @Named("serializeConfiguration")
    default String serializeConfiguration(Object configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }
        if (configuration instanceof LinkedHashMap) {
            try {
                JsonNode jsonNode = JSON_MAPPER.valueToTree(configuration);
                return JSON_MAPPER.writeValueAsString(jsonNode);
            } catch (IllegalArgumentException | JsonProcessingException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse configuration " + e);
            }
        } else if (configuration instanceof String) {
            try {
                JsonNode node = JSON_MAPPER.readTree((String) configuration);
                if (!node.isObject()) {
                    throw new InvalidDataException("Step.configuration must be a JSON object");
                }
                return (String) configuration;
            } catch (JsonProcessingException e) {
                throw new InvalidDataException("Step.configuration must be a JSON object");
            }
        } else {
            throw new InvalidDataException("Step.configuration must be a JSON object");
        }
    }

    @Named("convertToMapConfiguration")
    default Map<String, Object> convertToMapConfiguration(Object configuration) {
        if (Objects.isNull(configuration)) {
            return Map.of();
        }
        if (configuration instanceof LinkedHashMap) {
            return (Map<String, Object>) configuration;
        } else {
            return Map.of();
        }
    }

    @Named("deserializeConfiguration")
    default Object deserializeConfiguration(String configuration) {
        if (Objects.isNull(configuration)) {
            return null;
        }

        try {
            return JSON_MAPPER.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            log.debug("Cannot parse configuration as LinkedHashMap: " + configuration);
        }

        try {
            return JSON_MAPPER.readValue(configuration, List.class);
        } catch (JsonProcessingException jse) {
            log.debug("Cannot parse configuration as List: " + configuration);
        }

        return configuration;
    }
}
