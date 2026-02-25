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

import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * MapStruct mapper for converting between SubscriptionForm (domain) and SubscriptionForm (OpenAPI DTO).
 *
 * @author Gravitee.io Team
 */
@Mapper(uses = { DateMapper.class })
public interface SubscriptionFormMapper {
    SubscriptionFormMapper INSTANCE = Mappers.getMapper(SubscriptionFormMapper.class);

    /**
     * Converts domain entity to OpenAPI DTO.
     *
     * @param entity domain entity
     * @return OpenAPI DTO
     */
    @Mapping(target = "id", expression = "java(mapId(entity.getId()))")
    @Mapping(target = "gmdContent", source = "gmdContent", qualifiedByName = "graviteeMarkdownToString")
    io.gravitee.rest.api.management.v2.rest.model.SubscriptionForm toResponse(SubscriptionForm entity);

    /**
     * Converts SubscriptionFormId to UUID.
     *
     * @param id subscription form ID
     * @return UUID
     */
    default UUID mapId(SubscriptionFormId id) {
        return id != null ? UUID.fromString(id.toString()) : null;
    }

    @Named("graviteeMarkdownToString")
    default String graviteeMarkdownToString(GraviteeMarkdown gmd) {
        return gmd != null ? gmd.value() : null;
    }
}
