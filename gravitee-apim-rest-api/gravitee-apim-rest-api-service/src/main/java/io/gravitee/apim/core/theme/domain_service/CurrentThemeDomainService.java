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
package io.gravitee.apim.core.theme.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZonedDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CurrentThemeDomainService {

    private final ThemeQueryService themeQueryService;
    private final ThemeCrudService themeCrudService;

    public void disablePreviousEnabledTheme(Theme newCurrentTheme) {
        var themes = this.themeQueryService.findByThemeTypeAndEnvironmentId(newCurrentTheme.getType(), newCurrentTheme.getReferenceId());

        if (themes.isEmpty()) {
            return;
        }

        // Set enabled to false for currently enabled themes and update
        themes
            .stream()
            .filter(theme -> !Objects.equals(theme.getId(), newCurrentTheme.getId()) && Objects.equals(theme.isEnabled(), true))
            .forEach(theme -> {
                theme.setEnabled(false);
                theme.setUpdatedAt(ZonedDateTime.now());
                this.themeCrudService.update(theme);
            });
    }
}
