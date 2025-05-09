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
package io.gravitee.rest.api.service.v4.mapper;

import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PlanEntityMapper {
    PlanEntityMapper INSTANCE = Mappers.getMapper(PlanEntityMapper.class);

    @Mapping(source = "security.type", target = "security", qualifiedByName = "mapStringToStatusEnum")
    PlanEntity convertV4ToPlanEntity(io.gravitee.rest.api.model.v4.plan.PlanEntity planEntity);

    @Named("mapStringToStatusEnum")
    default PlanSecurityType mapStringToStatusEnum(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "jwt" -> PlanSecurityType.JWT;
            case "api-key" -> PlanSecurityType.API_KEY;
            case "outh2" -> PlanSecurityType.OAUTH2;
            case "key-less" -> PlanSecurityType.KEY_LESS;
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }
}
