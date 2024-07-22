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
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ThemeDomainService {

    private final ThemeCrudService themeCrudService;

    public Theme create(NewTheme newTheme) {
        var currentTime = ZonedDateTime.now();

        var themeToBeCreated = Theme
            .builder()
            .name(newTheme.getName())
            .referenceId(newTheme.getReferenceId())
            .referenceType(newTheme.getReferenceType())
            .definitionPortal(newTheme.getDefinitionPortal())
            .definitionPortalNext(newTheme.getDefinitionPortalNext())
            .type(newTheme.getType())
            .enabled(newTheme.isEnabled())
            .logo(newTheme.getLogo())
            .optionalLogo(newTheme.getOptionalLogo())
            .favicon(newTheme.getFavicon())
            .backgroundImage(newTheme.getBackgroundImage())
            .id(UuidString.generateRandom())
            .createdAt(currentTime)
            .updatedAt(currentTime)
            .build();

        return this.themeCrudService.create(themeToBeCreated);
    }

    public Theme update(UpdateTheme updateTheme, Theme existingTheme) {
        var existingThemeWithUpdates = existingTheme
            .toBuilder()
            .name(updateTheme.getName())
            .definitionPortal(updateTheme.getDefinitionPortal())
            .definitionPortalNext(updateTheme.getDefinitionPortalNext())
            .enabled(updateTheme.isEnabled())
            .logo(updateTheme.getLogo())
            .optionalLogo(updateTheme.getOptionalLogo())
            .favicon(updateTheme.getFavicon())
            .backgroundImage(updateTheme.getBackgroundImage())
            .updatedAt(ZonedDateTime.now())
            .build();

        return this.themeCrudService.update(existingThemeWithUpdates);
    }
}
