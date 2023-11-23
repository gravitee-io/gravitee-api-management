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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiCRD;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import java.util.stream.Stream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ApiAdapter {
    ApiAdapter INSTANCE = Mappers.getMapper(ApiAdapter.class);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api toEntity(io.gravitee.repository.management.model.Api criteria);

    Stream<Api> toEntityStream(Stream<io.gravitee.repository.management.model.Api> criteria);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    io.gravitee.repository.management.model.Api fromEntity(Api criteria);

    Stream<io.gravitee.repository.management.model.Api> fromEntityStream(Stream<Api> criteria);

    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    @Mapping(source = "version", target = "apiVersion")
    @Mapping(target = "metadata", ignore = true)
    ApiEntity toApiEntity(ApiCRD api);

    @Mapping(source = "state", target = "lifecycleState")
    @Mapping(source = "lifecycleState", target = "apiLifecycleState")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api fromApiEntity(ApiEntity apiEntity);

    @Mapping(source = "state", target = "lifecycleState")
    @Mapping(source = "lifecycleState", target = "apiLifecycleState")
    @ValueMapping(source = MappingConstants.ANY_REMAINING, target = MappingConstants.NULL)
    Api fromApiEntity(GenericApiEntity apiEntity);
}
