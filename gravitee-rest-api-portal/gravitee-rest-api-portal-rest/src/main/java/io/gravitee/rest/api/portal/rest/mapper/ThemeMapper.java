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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.rest.api.model.theme.ThemeEntity;
import io.gravitee.rest.api.portal.rest.model.ThemeLinks;
import io.gravitee.rest.api.portal.rest.model.ThemeResponse;
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
        themeResponse.setLinks(computeLinks(themeEntity, basePath));
        return themeResponse;
    }

    public ThemeLinks computeLinks(ThemeEntity themeEntity, String basePath) {
        ThemeLinks themeLinks = new ThemeLinks();
        themeLinks.setSelf(basePath);
        final String hash = themeEntity.getUpdatedAt() == null ? "" : String.valueOf(themeEntity.getUpdatedAt().getTime());
        themeLinks.setBackgroundImage(basePath + "/backgroundImage?" + hash);
        themeLinks.setLogo(basePath + "/logo?" + hash);
        themeLinks.setOptionalLogo(basePath + "/optionalLogo?" + hash);
        themeLinks.setFavicon(basePath + "/favicon?" + hash);
        return themeLinks;
    }
}
