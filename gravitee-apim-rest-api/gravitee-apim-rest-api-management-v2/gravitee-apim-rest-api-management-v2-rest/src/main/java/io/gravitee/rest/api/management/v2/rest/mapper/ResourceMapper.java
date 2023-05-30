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
import io.gravitee.rest.api.management.v2.rest.model.Resource;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface ResourceMapper {
    ResourceMapper INSTANCE = Mappers.getMapper(ResourceMapper.class);

    // V4
    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.v4.resource.Resource mapToResourceEntityV4(Resource resource);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Resource mapFromResourceEntityV4(io.gravitee.definition.model.v4.resource.Resource resource);

    // V2
    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    io.gravitee.definition.model.plugins.resources.Resource mapToResourceEntityV2(Resource resource);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    Resource mapFromResourceEntityV2(io.gravitee.definition.model.plugins.resources.Resource resource);
}
