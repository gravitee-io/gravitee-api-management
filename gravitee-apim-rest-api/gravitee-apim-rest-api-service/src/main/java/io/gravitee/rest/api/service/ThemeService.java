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
package io.gravitee.rest.api.service;

import io.gravitee.rest.api.model.PictureEntity;
import io.gravitee.rest.api.model.theme.NewThemeEntity;
import io.gravitee.rest.api.model.theme.ThemeEntity;
import io.gravitee.rest.api.model.theme.UpdateThemeEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ThemeService {
    Set<ThemeEntity> findAll(final ExecutionContext executionContext);
    ThemeEntity findById(final ExecutionContext executionContext, String themeId);
    ThemeEntity create(final ExecutionContext executionContext, NewThemeEntity theme);
    ThemeEntity update(final ExecutionContext executionContext, UpdateThemeEntity theme);
    void delete(final ExecutionContext executionContext, String themeId);
    ThemeEntity findEnabled(final ExecutionContext executionContext);
    ThemeEntity resetToDefaultTheme(final ExecutionContext executionContext, String themeId);
    PictureEntity getLogo(final ExecutionContext executionContext, String themeId);
    PictureEntity getOptionalLogo(final ExecutionContext executionContext, String themeId);
    PictureEntity getBackgroundImage(final ExecutionContext executionContext, String themeId);
    void updateDefaultTheme(final ExecutionContext executionContext);
    PictureEntity getFavicon(final ExecutionContext executionContext, String themeId);
}
