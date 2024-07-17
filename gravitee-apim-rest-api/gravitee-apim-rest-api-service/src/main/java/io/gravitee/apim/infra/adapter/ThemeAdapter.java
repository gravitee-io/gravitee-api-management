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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface ThemeAdapter {
    Logger LOGGER = LoggerFactory.getLogger(ThemeAdapter.class);
    ThemeAdapter INSTANCE = Mappers.getMapper(ThemeAdapter.class);

    @Mapping(target = "definition", expression = "java(serializeDefinition(theme))")
    io.gravitee.repository.management.model.Theme map(Theme theme);

    @Mapping(target = "definitionPortal", expression = "java(deserializeDefinitionPortal(theme))")
    @Mapping(target = "definitionPortalNext", expression = "java(deserializeDefinitionPortalNext(theme))")
    Theme map(io.gravitee.repository.management.model.Theme theme);

    @Mapping(target = "definitionPortal", source = "definition")
    Theme map(ThemeEntity themeEntity);

    List<Theme> map(List<io.gravitee.repository.management.model.Theme> themes);
    Set<Theme> map(Set<io.gravitee.repository.management.model.Theme> themes);

    default ThemeDefinition deserializeDefinitionPortal(io.gravitee.repository.management.model.Theme theme) {
        if (Objects.nonNull(theme.getDefinition()) && ThemeType.PORTAL.equals(theme.getType())) {
            try {
                return GraviteeJacksonMapper.getInstance().readValue(theme.getDefinition(), ThemeDefinition.class);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while deserializing PORTAL theme definition", ioe);
            }
        }

        return null;
    }

    default io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition deserializeDefinitionPortalNext(
        io.gravitee.repository.management.model.Theme theme
    ) {
        if (Objects.nonNull(theme.getDefinition()) && ThemeType.PORTAL_NEXT.equals(theme.getType())) {
            try {
                return GraviteeJacksonMapper
                    .getInstance()
                    .readValue(theme.getDefinition(), io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.class);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while deserializing PORTAL_NEXT theme definition", ioe);
            }
        }

        return null;
    }

    default String serializeDefinition(Theme theme) {
        Object definition = io.gravitee.apim.core.theme.model.ThemeType.PORTAL.equals(theme.getType())
            ? theme.getDefinitionPortal()
            : theme.getDefinitionPortalNext();

        return Optional
            .ofNullable(definition)
            .map(def -> {
                try {
                    return GraviteeJacksonMapper.getInstance().writeValueAsString(def);
                } catch (JsonProcessingException e) {
                    LOGGER.error("Unable to serialize definition: {}", theme.getDefinitionPortal());
                    return "";
                }
            })
            .orElse("");
    }
}
