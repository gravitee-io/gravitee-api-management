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
package io.gravitee.apim.core.theme.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.exception.ThemeDefinitionInvalidException;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.util.Objects;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class CreateThemeUseCase {

    private final CurrentThemeDomainService currentThemeDomainService;
    private final ThemeDomainService themeDomainService;

    public Output execute(Input input) {
        var newTheme = input.newTheme();

        // validate that theme definition is correct for theme type
        if (ThemeType.PORTAL.equals(newTheme.getType()) && Objects.isNull(newTheme.getDefinitionPortal())) {
            throw new ThemeDefinitionInvalidException(ThemeType.PORTAL, null);
        }

        if (ThemeType.PORTAL_NEXT.equals(newTheme.getType()) && Objects.isNull(newTheme.getDefinitionPortalNext())) {
            throw new ThemeDefinitionInvalidException(ThemeType.PORTAL_NEXT, null);
        }

        var createdTheme = this.themeDomainService.create(newTheme);

        if (Boolean.TRUE.equals(createdTheme.isEnabled())) {
            this.currentThemeDomainService.disablePreviousEnabledTheme(createdTheme);
        }

        return new Output(createdTheme);
    }

    @Builder
    public record Input(NewTheme newTheme) {}

    public record Output(Theme created) {}
}
