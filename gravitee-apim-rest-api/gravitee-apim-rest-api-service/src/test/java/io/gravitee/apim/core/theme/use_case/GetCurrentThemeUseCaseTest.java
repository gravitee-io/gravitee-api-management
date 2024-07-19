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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.InMemoryAlternative;
import inmemory.ParametersDomainServiceInMemory;
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import inmemory.ThemeServiceLegacyWrapperInMemory;
import io.gravitee.apim.core.theme.domain_service.DefaultThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.exception.ThemeTypeNotSupportedException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.query_service.ThemeQueryService;
import io.gravitee.repository.management.model.Parameter;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetCurrentThemeUseCaseTest {

    private final ExecutionContext EXECUTION_CONTEXT = GraviteeContext.getExecutionContext();

    private final ThemeDefinition EXPECTED_PORTAL_NEXT_THEME_DEFINITION = ThemeDefinition
        .builder()
        .color(
            ThemeDefinition.Color
                .builder()
                .primary("#fff")
                .secondary("#000")
                .tertiary("#111")
                .error("#222")
                .background(ThemeDefinition.Background.builder().page("#333").card("#444").build())
                .build()
        )
        .font(ThemeDefinition.Font.builder().fontFamily("Comic Sans").build())
        .customCss(".style { }")
        .build();

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ParametersDomainServiceInMemory parametersDomainService = new ParametersDomainServiceInMemory();
    ThemeServiceLegacyWrapperInMemory themeServiceLegacyWrapper = new ThemeServiceLegacyWrapperInMemory();
    ThemeQueryServiceInMemory themeQueryServiceInMemory = new ThemeQueryServiceInMemory(themeCrudService);
    DefaultThemeDomainService defaultThemeDomainService;
    GetCurrentThemeUseCase cut;

    @BeforeEach
    void setUp() {
        parametersDomainService.initWith(
            List.of(
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_PRIMARY.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getPrimary())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_SECONDARY.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getSecondary())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_TERTIARY.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getTertiary())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_ERROR.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getError())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_PAGE.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getBackground().getPage())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_COLOR_BACKGROUND_CARD.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getColor().getBackground().getCard())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_CUSTOM_CSS.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getCustomCss())
                    .build(),
                Parameter
                    .builder()
                    .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
                    .referenceType(ParameterReferenceType.ENVIRONMENT)
                    .key(Key.PORTAL_NEXT_THEME_FONT_FAMILY.key())
                    .value(EXPECTED_PORTAL_NEXT_THEME_DEFINITION.getFont().getFontFamily())
                    .build()
            )
        );

        defaultThemeDomainService =
            new DefaultThemeDomainService(parametersDomainService, new ThemeDomainService(themeCrudService), themeServiceLegacyWrapper);
        cut = new GetCurrentThemeUseCase(themeQueryServiceInMemory, defaultThemeDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream
            .of(parametersDomainService, themeCrudService, themeServiceLegacyWrapper, themeQueryServiceInMemory)
            .forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_return_existing_portal_next_theme() {
        themeCrudService.initWith(List.of(aPortalNextTheme(true)));

        var result = cut.execute(new GetCurrentThemeUseCase.Input(ThemeType.PORTAL_NEXT, EXECUTION_CONTEXT));
        assertThat(result).extracting(GetCurrentThemeUseCase.Output::result).isEqualTo(aPortalNextTheme(true));
    }

    @Test
    void should_return_existing_portal_theme() {
        themeCrudService.initWith(List.of(aPortalTheme(true)));

        var result = cut.execute(new GetCurrentThemeUseCase.Input(ThemeType.PORTAL, EXECUTION_CONTEXT));
        assertThat(result).extracting(GetCurrentThemeUseCase.Output::result).isEqualTo(aPortalTheme(true));
    }

    @Test
    void should_return_default_portal_next_theme() {
        themeCrudService.initWith(List.of(aPortalNextTheme(false)));

        var result = cut.execute(new GetCurrentThemeUseCase.Input(ThemeType.PORTAL_NEXT, EXECUTION_CONTEXT));
        assertThat(result)
            .extracting(GetCurrentThemeUseCase.Output::result)
            .satisfies(res -> {
                var defaultPortalTheme = defaultThemeDomainService.getDefaultTheme(ThemeType.PORTAL_NEXT, EXECUTION_CONTEXT);

                assertThat(res.getId()).isNotBlank();
                assertThat(res.getName()).isEqualTo(defaultPortalTheme.getName());
                assertThat(res.getDefinitionPortalNext()).isEqualTo(defaultPortalTheme.getDefinitionPortalNext());
                assertThat(res.isEnabled()).isEqualTo(true);
                assertThat(res.getCreatedAt()).isNotNull();
                assertThat(res.getUpdatedAt()).isNotNull();
            });

        assertThat(themeCrudService.storage()).hasSize(2).contains(result.result());
    }

    @Test
    void should_return_default_portal_theme() {
        themeCrudService.initWith(List.of(aPortalTheme(false)));

        var result = cut.execute(new GetCurrentThemeUseCase.Input(ThemeType.PORTAL, EXECUTION_CONTEXT));
        assertThat(result)
            .extracting(GetCurrentThemeUseCase.Output::result)
            .satisfies(res -> {
                assertThat(res.getId()).isEqualTo("portal-default-theme");
            });
    }

    private Theme aPortalTheme(boolean enabled) {
        var portalDefinition = new io.gravitee.rest.api.model.theme.portal.ThemeDefinition();
        portalDefinition.setData(List.of());

        return Theme
            .builder()
            .id("portal-theme")
            .name("a portal theme")
            .type(ThemeType.PORTAL)
            .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .definitionPortal(portalDefinition)
            .enabled(enabled)
            .build();
    }

    private Theme aPortalNextTheme(boolean enabled) {
        return Theme
            .builder()
            .id("portal-next-theme")
            .name("a portal next theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .definitionPortalNext(ThemeDefinition.builder().color(ThemeDefinition.Color.builder().primary("#fff").build()).build())
            .enabled(enabled)
            .build();
    }
}
