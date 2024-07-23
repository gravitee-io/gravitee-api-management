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

import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.management.v2.rest.model.ThemePortal;
import io.gravitee.rest.api.management.v2.rest.model.ThemePortalNext;
import io.gravitee.rest.api.management.v2.rest.model.UpdateThemePortal;
import io.gravitee.rest.api.management.v2.rest.model.UpdateThemePortalNext;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = { DateMapper.class })
public interface ThemeMapper {
    ThemeMapper INSTANCE = Mappers.getMapper(ThemeMapper.class);

    ThemeType map(io.gravitee.rest.api.management.v2.rest.model.ThemeType type);

    @Mapping(source = "definitionPortal", target = "definition")
    ThemePortal mapToThemePortal(Theme theme);

    @Mapping(source = "definitionPortalNext", target = "definition")
    @Mapping(source = "definitionPortalNext.color.background.page", target = "definition.color.pageBackground")
    @Mapping(source = "definitionPortalNext.color.background.card", target = "definition.color.cardBackground")
    ThemePortalNext mapToThemePortalNext(Theme theme);

    List<io.gravitee.rest.api.management.v2.rest.model.Theme> map(List<Theme> themes);

    @Mapping(source = "definition", target = "definitionPortal")
    UpdateTheme map(UpdateThemePortal updateThemePortal);

    @Mapping(target = "definitionPortalNext", source = "definition")
    @Mapping(target = "definitionPortalNext.color.background.page", source = "definition.color.pageBackground")
    @Mapping(target = "definitionPortalNext.color.background.card", source = "definition.color.cardBackground")
    UpdateTheme map(UpdateThemePortalNext updateThemePortalNext);

    default io.gravitee.rest.api.management.v2.rest.model.Theme map(Theme theme) {
        if (ThemeType.PORTAL.equals(theme.getType())) {
            return new io.gravitee.rest.api.management.v2.rest.model.Theme(this.mapToThemePortal(theme));
        }
        if (ThemeType.PORTAL_NEXT.equals(theme.getType())) {
            return new io.gravitee.rest.api.management.v2.rest.model.Theme(this.mapToThemePortalNext(theme));
        }
        return null;
    }

    default UpdateTheme map(io.gravitee.rest.api.management.v2.rest.model.UpdateTheme updateTheme) {
        try {
            return this.map(updateTheme.getUpdateThemePortal());
        } catch (ClassCastException e) {
            // do nothing
        }

        try {
            return this.map(updateTheme.getUpdateThemePortalNext());
        } catch (ClassCastException e) {
            // do nothing
        }

        return null;
    }
}
