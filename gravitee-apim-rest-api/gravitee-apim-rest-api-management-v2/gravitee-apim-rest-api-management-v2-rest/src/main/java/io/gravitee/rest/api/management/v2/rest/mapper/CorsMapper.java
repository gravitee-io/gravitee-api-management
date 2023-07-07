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

import io.gravitee.rest.api.management.v2.rest.model.Cors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CorsMapper {
    CorsMapper INSTANCE = Mappers.getMapper(CorsMapper.class);

    @Mapping(target = "accessControlAllowCredentials", source = "allowCredentials")
    @Mapping(target = "accessControlAllowHeaders", source = "allowHeaders")
    @Mapping(target = "accessControlAllowMethods", source = "allowMethods")
    @Mapping(target = "accessControlAllowOrigin", source = "allowOrigin")
    @Mapping(target = "accessControlExposeHeaders", source = "exposeHeaders")
    @Mapping(target = "accessControlMaxAge", source = "maxAge")
    io.gravitee.definition.model.Cors map(Cors cors);

    @Mapping(target = "allowCredentials", source = "accessControlAllowCredentials")
    @Mapping(target = "allowHeaders", source = "accessControlAllowHeaders")
    @Mapping(target = "allowMethods", source = "accessControlAllowMethods")
    @Mapping(target = "allowOrigin", source = "accessControlAllowOrigin")
    @Mapping(target = "exposeHeaders", source = "accessControlExposeHeaders")
    @Mapping(target = "maxAge", source = "accessControlMaxAge")
    Cors map(io.gravitee.definition.model.Cors cors);
}
