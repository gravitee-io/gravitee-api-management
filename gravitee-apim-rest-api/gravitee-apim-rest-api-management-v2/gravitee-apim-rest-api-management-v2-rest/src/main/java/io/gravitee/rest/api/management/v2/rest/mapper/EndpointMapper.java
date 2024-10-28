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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.rest.api.management.v2.rest.model.*;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, KeyStoreMapper.class, ServiceMapper.class, TrustStoreMapper.class })
public interface EndpointMapper {
    EndpointMapper INSTANCE = Mappers.getMapper(EndpointMapper.class);

    // V4
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.Endpoint mapToHttpV4(EndpointV4 entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "deserializeConfiguration")
    EndpointV4 mapFromHttpV4(io.gravitee.definition.model.v4.endpointgroup.Endpoint endpoint);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.nativeapi.NativeEndpoint mapToNativeV4(EndpointV4 entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "deserializeConfiguration")
    EndpointV4 mapFromNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeEndpoint endpoint);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.EndpointGroup mapEndpointGroupHttpV4(EndpointGroupV4 endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "deserializeConfiguration")
    EndpointGroupV4 mapEndpointGroupHttpV4(io.gravitee.definition.model.v4.endpointgroup.EndpointGroup endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup mapEndpointGroupNativeV4(EndpointGroupV4 endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "deserializeConfiguration")
    EndpointGroupV4 mapEndpointGroupNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup endpointGroup);

    // V2
    io.gravitee.definition.model.Endpoint mapEndpoint(HttpEndpointV2 endpoint);

    // Remove healthCheck. The healthCheck to use is the one defined in configuration
    @Mapping(target = "healthCheck", expression = "java(null)")
    io.gravitee.definition.model.Endpoint mapHttpEndpoint(HttpEndpointV2 endpoint, String configuration);

    default io.gravitee.definition.model.Endpoint map(EndpointV2 endpoint) {
        if (endpoint == null) {
            return null;
        }

        if (endpoint.getActualInstance() instanceof HttpEndpointV2) {
            ObjectMapper mapper = new GraviteeMapper();
            String configuration;
            try {
                // We need to map the httpProxy, httpClientOptions and httpClientSslOptions to the new configuration
                var configurationNode = mapper.valueToTree(endpoint);
                var proxy = configurationNode.get("httpProxy");
                ((ObjectNode) configurationNode).set("proxy", proxy);
                ((ObjectNode) configurationNode).remove("httpProxy");
                var httpclient = configurationNode.get("httpClientOptions");
                ((ObjectNode) configurationNode).set("http", httpclient);
                ((ObjectNode) configurationNode).remove("httpClientOptions");
                var ssl = configurationNode.get("httpClientSslOptions");
                ((ObjectNode) configurationNode).set("ssl", ssl);
                ((ObjectNode) configurationNode).remove("httpClientSslOptions");
                var healthcheck = configurationNode.get("healthCheck");
                ((ObjectNode) configurationNode).set("healthcheck", healthcheck);
                ((ObjectNode) configurationNode).remove("healthCheck");

                configuration = mapper.writeValueAsString(configurationNode);
            } catch (JsonProcessingException e) {
                throw new TechnicalManagementException("An error occurred while trying to serialize endpoint configuration " + e);
            }

            return mapHttpEndpoint(endpoint.getHttpEndpointV2(), configuration);
        }

        return mapEndpoint(endpoint.getHttpEndpointV2());
    }

    HttpEndpointV2 mapEndpoint(io.gravitee.definition.model.Endpoint endpoint);

    HttpEndpointV2 mapHttpEndpoint(io.gravitee.definition.model.endpoint.HttpEndpoint httpEndpoint);

    default EndpointV2 map(io.gravitee.definition.model.Endpoint endpoint) {
        if (endpoint == null) {
            return null;
        }

        // If endpoint is a http or grpc endpoint, we need to parse the configuration and use it as the actual instance
        if (
            (endpoint.getType().equalsIgnoreCase("http") || endpoint.getType().equalsIgnoreCase("grpc")) &&
            endpoint.getConfiguration() != null
        ) {
            ObjectMapper mapper = new GraviteeMapper();
            io.gravitee.definition.model.endpoint.HttpEndpoint config;
            try {
                config = mapper.readValue(endpoint.getConfiguration(), io.gravitee.definition.model.endpoint.HttpEndpoint.class);
            } catch (JsonProcessingException e) {
                throw new TechnicalManagementException("An error occurred while trying to parse endpoint configuration " + e);
            }

            return new EndpointV2(mapHttpEndpoint(config));
        }
        return new EndpointV2(mapEndpoint(endpoint));
    }

    EndpointGroupV2 mapEndpointGroup(io.gravitee.definition.model.EndpointGroup endpointGroup);
    io.gravitee.definition.model.EndpointGroup mapEndpointGroup(EndpointGroupV2 endpointGroup);
}
