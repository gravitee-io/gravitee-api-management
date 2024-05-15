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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.theme.GenericThemeEntity;
import io.gravitee.rest.api.model.theme.ThemeType;
import io.gravitee.rest.api.model.theme.portal.NewThemeEntity;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.model.theme.portal.UpdateThemeEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ThemeService {
    Set<GenericThemeEntity> findAllByType(final ExecutionContext executionContext, ThemeType type);
    GenericThemeEntity findById(final ExecutionContext executionContext, String themeId);
    void delete(final ExecutionContext executionContext, String themeId);
    GenericThemeEntity resetToDefaultTheme(final ExecutionContext executionContext, String themeId);
    PictureEntity getLogo(final ExecutionContext executionContext, String themeId);
    PictureEntity getOptionalLogo(final ExecutionContext executionContext, String themeId);
    PictureEntity getBackgroundImage(final ExecutionContext executionContext, String themeId);
    PictureEntity getFavicon(final ExecutionContext executionContext, String themeId);

    // PORTAL THEME
    ThemeEntity findOrCreateDefaultPortalTheme(ExecutionContext executionContext);
    ThemeEntity findEnabledPortalTheme(final ExecutionContext executionContext);
    void updateDefaultPortalTheme(final ExecutionContext executionContext);
    ThemeEntity createPortalTheme(final ExecutionContext executionContext, NewThemeEntity theme);
    ThemeEntity updatePortalTheme(final ExecutionContext executionContext, UpdateThemeEntity theme);
}
