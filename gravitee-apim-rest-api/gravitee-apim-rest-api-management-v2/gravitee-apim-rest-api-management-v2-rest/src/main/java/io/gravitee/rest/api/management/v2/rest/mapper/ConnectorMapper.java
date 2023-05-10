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
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.LinkedHashMap;
import java.util.Objects;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface ConnectorMapper {
    ConnectorMapper INSTANCE = Mappers.getMapper(ConnectorMapper.class);
    Logger LOGGER = LoggerFactory.getLogger(ConnectorMapper.class);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint map(Entrypoint entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.Endpoint map(EndpointV4 entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Entrypoint map(io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "serializeConfiguration")
    EndpointV4 map(io.gravitee.definition.model.v4.endpointgroup.Endpoint endpoint);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "deserializeConfiguration")
    EndpointGroup map(EndpointGroupV4 endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "serializeConfiguration")
    EndpointGroupV4 map(EndpointGroup endpointGroup);

    @Named("deserializeConfiguration")
    default String deserializeConfiguration(Object configuration) throws JsonProcessingException {
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
    default Object serializeConfiguration(String configuration) throws JsonProcessingException {
        if (Objects.isNull(configuration)) {
            return null;
        }
        try {
            ObjectMapper mapper = new GraviteeMapper();
            return mapper.readValue(configuration, LinkedHashMap.class);
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating connector", jse);
            throw new TechnicalManagementException("An error occurred while trying to parse connector configuration");
        }
    }
}
