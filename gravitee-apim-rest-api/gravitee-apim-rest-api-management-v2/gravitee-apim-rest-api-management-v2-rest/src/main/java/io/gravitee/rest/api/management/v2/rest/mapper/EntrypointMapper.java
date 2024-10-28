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

import io.gravitee.rest.api.management.v2.rest.model.Entrypoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { ConfigurationSerializationMapper.class })
public interface EntrypointMapper {
    EntrypointMapper INSTANCE = Mappers.getMapper(EntrypointMapper.class);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint mapToHttpV4(Entrypoint entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    Entrypoint mapFromHttpV4(io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "serializeConfiguration")
    io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint mapToNativeV4(Entrypoint entrypoint);

    @Mapping(target = "configuration", qualifiedByName = "deserializeConfiguration")
    Entrypoint mapFromNativeV4(io.gravitee.definition.model.v4.nativeapi.NativeEntrypoint entrypoint);
}
