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

import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpEndpointV2;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, KeyStoreMapper.class, ServiceMapper.class, TrustStoreMapper.class })
public interface EndpointMapper {
    EndpointMapper INSTANCE = Mappers.getMapper(EndpointMapper.class);

    // V4
    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.Endpoint map(EndpointV4 entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "deserializeConfiguration")
    EndpointV4 map(io.gravitee.definition.model.v4.endpointgroup.Endpoint endpoint);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.EndpointGroup mapEndpointGroup(EndpointGroupV4 endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "deserializeConfiguration")
    EndpointGroupV4 mapEndpointGroup(io.gravitee.definition.model.v4.endpointgroup.EndpointGroup endpointGroup);

    // V2
    io.gravitee.definition.model.Endpoint mapHttpEndpoint(HttpEndpointV2 endpoint);

    default io.gravitee.definition.model.Endpoint map(EndpointV2 endpoint) {
        if (endpoint == null) {
            return null;
        }
        return mapHttpEndpoint(endpoint.getHttpEndpointV2());
    }

    HttpEndpointV2 mapHttpEndpoint(io.gravitee.definition.model.Endpoint endpoint);

    default EndpointV2 map(io.gravitee.definition.model.Endpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        return new EndpointV2(mapHttpEndpoint(endpoint));
    }

    EndpointGroupV2 mapEndpointGroup(io.gravitee.definition.model.EndpointGroup endpointGroup);
    io.gravitee.definition.model.EndpointGroup mapEndpointGroup(EndpointGroupV2 endpointGroup);
}
