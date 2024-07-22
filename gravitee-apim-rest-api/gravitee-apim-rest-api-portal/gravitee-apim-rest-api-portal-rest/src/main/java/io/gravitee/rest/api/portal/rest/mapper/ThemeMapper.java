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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.portal.rest.model.ThemeLinks;
import io.gravitee.rest.api.portal.rest.model.ThemeResponse;
import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ThemeMapper {

    public ThemeResponse convert(ThemeEntity themeEntity, String basePath) {
        final ThemeResponse themeResponse = new ThemeResponse();
        themeResponse.setDefinition(themeEntity.getDefinition());
        themeResponse.setType(ThemeResponse.TypeEnum.valueOf(themeEntity.getType().name()));
        themeResponse.setLinks(computeLinks(themeEntity.getUpdatedAt(), basePath));
        return themeResponse;
    }

    public ThemeResponse convert(Theme theme, String basePath) {
        final ThemeResponse themeResponse = new ThemeResponse();
        if (ThemeType.PORTAL_NEXT.equals(theme.getType())) {
            themeResponse.setDefinition(theme.getDefinitionPortalNext());
        } else if (ThemeType.PORTAL.equals(theme.getType())) {
            themeResponse.setDefinition(theme.getDefinitionPortal());
        }
        themeResponse.setType(ThemeResponse.TypeEnum.valueOf(theme.getType().name()));
        themeResponse.setLinks(computeLinks(theme.getUpdatedAt(), basePath));
        return themeResponse;
    }

    public ThemeLinks computeLinks(Date updatedAt, String basePath) {
        return this.computeLinks(updatedAt == null ? "" : String.valueOf(updatedAt.getTime()), basePath);
    }

    public ThemeLinks computeLinks(ZonedDateTime updatedAt, String basePath) {
        return this.computeLinks(updatedAt == null ? "" : String.valueOf(updatedAt.toInstant().toEpochMilli()), basePath);
    }

    private ThemeLinks computeLinks(String hash, String basePath) {
        ThemeLinks themeLinks = new ThemeLinks();
        themeLinks.setSelf(basePath);
        themeLinks.setBackgroundImage(basePath + "/backgroundImage?" + hash);
        themeLinks.setLogo(basePath + "/logo?" + hash);
        themeLinks.setOptionalLogo(basePath + "/optionalLogo?" + hash);
        themeLinks.setFavicon(basePath + "/favicon?" + hash);
        return themeLinks;
    }
}
