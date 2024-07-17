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
import io.gravitee.apim.core.parameters.domain_service.ParametersDomainService;
import io.gravitee.apim.core.theme.exception.ThemeTypeNotSupportedException;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class DefaultThemeDomainService {

    private final ParametersDomainService parametersDomainService;
    private final ThemeDomainService themeDomainService;
    private final ThemeServiceLegacyWrapper themeServiceLegacyWrapper;

    private static final List<Key> PORTAL_NEXT_THEME_KEYS = List.of(
        Key.PORTAL_NEXT_THEME_COLOR_PRIMARY,
        Key.PORTAL_NEXT_THEME_COLOR_SECONDARY,
        Key.PORTAL_NEXT_THEME_COLOR_TERTIARY,
        Key.PORTAL_NEXT_THEME_COLOR_ERROR,
        Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_PAGE,
        Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_CARD,
        Key.PORTAL_NEXT_THEME_CUSTOM_CSS,
        Key.PORTAL_NEXT_THEME_FONT_FAMILY
    );

    public Theme getDefaultTheme(ThemeType themeType, ExecutionContext executionContext) {
        if (Objects.requireNonNull(themeType) == ThemeType.PORTAL_NEXT) {
            return this.getPortalNextDefaultTheme(executionContext);
        }

        throw new ThemeTypeNotSupportedException(themeType.name());
    }

    public Theme createAndEnableDefaultTheme(ThemeType themeType, ExecutionContext executionContext) {
        if (ThemeType.PORTAL_NEXT.equals(themeType)) {
            var defaultTheme = this.getDefaultTheme(themeType, executionContext);
            return this.themeDomainService.create(
                    NewTheme
                        .builder()
                        .referenceId(defaultTheme.getReferenceId())
                        .referenceType(defaultTheme.getReferenceType())
                        .name(defaultTheme.getName())
                        .definitionPortalNext(defaultTheme.getDefinitionPortalNext())
                        .type(ThemeType.PORTAL_NEXT)
                        .enabled(true)
                        .build()
                );
        } else if (ThemeType.PORTAL.equals(themeType)) {
            return this.themeServiceLegacyWrapper.getCurrentOrCreateDefaultPortalTheme(executionContext);
        }

        throw new ThemeTypeNotSupportedException(themeType.name());
    }

    private Theme getPortalNextDefaultTheme(ExecutionContext executionContext) {
        Map<Key, String> parameters = parametersDomainService.getSystemParameters(PORTAL_NEXT_THEME_KEYS);

        return Theme
            .builder()
            .type(ThemeType.PORTAL_NEXT)
            .name("Default Portal Next Theme")
            .referenceId(executionContext.getEnvironmentId())
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .definitionPortalNext(
                ThemeDefinition
                    .builder()
                    .color(
                        ThemeDefinition.Color
                            .builder()
                            .primary(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY))
                            .secondary(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_SECONDARY))
                            .tertiary(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_TERTIARY))
                            .error(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_ERROR))
                            .background(
                                ThemeDefinition.Background
                                    .builder()
                                    .card(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_CARD))
                                    .page(parameters.get(Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_PAGE))
                                    .build()
                            )
                            .build()
                    )
                    .customCss(parameters.get(Key.PORTAL_NEXT_THEME_CUSTOM_CSS))
                    .font(ThemeDefinition.Font.builder().fontFamily(parameters.get(Key.PORTAL_NEXT_THEME_FONT_FAMILY)).build())
                    .build()
            )
            .build();
    }
}
