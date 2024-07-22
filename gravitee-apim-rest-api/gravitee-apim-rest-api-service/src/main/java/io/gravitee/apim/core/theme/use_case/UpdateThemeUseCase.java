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
import io.gravitee.apim.core.theme.crud_service.ThemeCrudService;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class UpdateThemeUseCase {

    private final ValidateThemeDomainService validateThemeDomainService;
    private final ThemeDomainService themeDomainService;
    private final CurrentThemeDomainService currentThemeDomainService;
    private final ThemeCrudService themeCrudService;

    public Output execute(Input input) {
        UpdateTheme updateTheme = input.updateTheme();

        this.validateThemeDomainService.validateUpdateTheme(input.updateTheme(), input.executionContext());

        var existingTheme = this.themeCrudService.get(input.updateTheme().getId());

        var updatedTheme = this.themeDomainService.update(updateTheme, existingTheme);

        if (!existingTheme.isEnabled() && updatedTheme.isEnabled()) {
            this.currentThemeDomainService.disablePreviousEnabledTheme(updatedTheme);
        }

        return new Output(updatedTheme);
    }

    @Builder
    public record Input(UpdateTheme updateTheme, ExecutionContext executionContext) {}

    public record Output(Theme result) {}
}
