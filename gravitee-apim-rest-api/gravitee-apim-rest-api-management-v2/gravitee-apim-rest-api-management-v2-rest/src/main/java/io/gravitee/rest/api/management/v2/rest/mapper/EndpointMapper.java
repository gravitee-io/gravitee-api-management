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

import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointGroupV4;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV2;
import io.gravitee.rest.api.management.v2.rest.model.EndpointV4;
import io.gravitee.rest.api.management.v2.rest.model.HttpEndpointV2;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class, KeyStoreMapper.class, TrustStoreMapper.class })
public interface EndpointMapper {
    EndpointMapper INSTANCE = Mappers.getMapper(EndpointMapper.class);

    // V4
    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.Endpoint mapToEndpointEntityV4(EndpointV4 entrypoint);

    List<io.gravitee.definition.model.v4.endpointgroup.Endpoint> mapToEndpointEntityV4List(List<EndpointV4> endpointList);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    @Mapping(target = "sharedConfigurationOverride", qualifiedByName = "serializeConfiguration")
    EndpointV4 mapFromEndpointEntityV4(io.gravitee.definition.model.v4.endpointgroup.Endpoint endpoint);

    List<EndpointV4> mapFromEndpointEntityV4List(List<io.gravitee.definition.model.v4.endpointgroup.Endpoint> endpointList);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.v4.endpointgroup.EndpointGroup mapToEndpointGroupEntityV4(EndpointGroupV4 endpointGroup);

    @Mapping(target = "sharedConfiguration", qualifiedByName = "serializeConfiguration")
    EndpointGroupV4 mapFromEndpointGroupEntityV4(io.gravitee.definition.model.v4.endpointgroup.EndpointGroup endpointGroup);

    // V2
    io.gravitee.definition.model.Endpoint mapHttpEndpointToEndpointEntityV2(HttpEndpointV2 endpoint);
    List<io.gravitee.definition.model.Endpoint> mapHttpEndpointListToEndpointEntityV2List(List<HttpEndpointV2> endpointList);

    default io.gravitee.definition.model.Endpoint mapToEndpointEntityV2(EndpointV2 endpoint) {
        if (endpoint == null) {
            return null;
        }
        return mapHttpEndpointToEndpointEntityV2(endpoint.getHttpEndpointV2());
    }

    default List<io.gravitee.definition.model.Endpoint> mapToEndpointEntityV2List(List<EndpointV2> endpointList) {
        if (endpointList == null) {
            return null;
        }
        return mapHttpEndpointListToEndpointEntityV2List(
            endpointList.stream().map(EndpointV2::getHttpEndpointV2).collect(java.util.stream.Collectors.toList())
        );
    }

    HttpEndpointV2 mapHttpEndpointFromEndpointEntityV2To(io.gravitee.definition.model.Endpoint endpoint);
    List<HttpEndpointV2> mapHttpEndpointListFromEndpointEntityV2List(List<io.gravitee.definition.model.Endpoint> endpointList);

    default EndpointV2 mapFromEndpointEntityV2(io.gravitee.definition.model.Endpoint endpoint) {
        if (endpoint == null) {
            return null;
        }
        return new EndpointV2(mapHttpEndpointFromEndpointEntityV2To(endpoint));
    }

    default List<EndpointV2> mapFromEndpointEntityV2List(List<io.gravitee.definition.model.Endpoint> endpointList) {
        if (endpointList == null) {
            return null;
        }
        return mapHttpEndpointListFromEndpointEntityV2List(endpointList)
            .stream()
            .map(EndpointV2::new)
            .collect(java.util.stream.Collectors.toList());
    }

    EndpointGroupV2 mapFromEndpointGroupEntityV2(io.gravitee.definition.model.EndpointGroup endpointGroup);
    io.gravitee.definition.model.EndpointGroup mapToEndpointGroupEntityV2(EndpointGroupV2 endpointGroup);
}
