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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.*;
import io.gravitee.apim.core.theme.exception.ThemeTypeNotSupportedException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.infra.domain_service.theme.ThemePortalNextAssetsDomainServiceImpl;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class DefaultThemeDomainServiceTest {

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

    private final Theme EXPECTED_DEFAULT_PORTAL_NEXT_THEME = Theme
        .builder()
        .type(ThemeType.PORTAL_NEXT)
        .referenceType(Theme.ReferenceType.ENVIRONMENT)
        .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
        .name("Default Portal Next Theme")
        .definitionPortalNext(EXPECTED_PORTAL_NEXT_THEME_DEFINITION)
        .logo("logo.png")
        .favicon("favicon.png")
        .build();

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ParametersDomainServiceInMemory parametersDomainService = new ParametersDomainServiceInMemory();
    ThemeServiceLegacyWrapperInMemory themeServiceLegacyWrapper = new ThemeServiceLegacyWrapperInMemory();
    ThemePortalNextAssetsDomainServiceInMemory themePortalNextAssetsDomainService = new ThemePortalNextAssetsDomainServiceInMemory();
    DefaultThemeDomainService cut;

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

        cut =
            new DefaultThemeDomainService(
                parametersDomainService,
                new ThemeDomainService(themeCrudService),
                themeServiceLegacyWrapper,
                themePortalNextAssetsDomainService
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(themeCrudService, parametersDomainService, themeServiceLegacyWrapper).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetDefaultTheme {

        @Test
        void should_return_portal_next_theme() {
            var result = cut.getDefaultTheme(ThemeType.PORTAL_NEXT, EXECUTION_CONTEXT);
            assertThat(result).isEqualTo(EXPECTED_DEFAULT_PORTAL_NEXT_THEME);
        }

        @Test
        void should_throw_error_for_portal_theme() {
            assertThatThrownBy(() -> cut.getDefaultTheme(ThemeType.PORTAL, EXECUTION_CONTEXT))
                .isInstanceOf(ThemeTypeNotSupportedException.class);
        }
    }

    @Nested
    class CreateDefaultTheme {

        @Test
        void should_create_enabled_portal_next_theme() {
            var result = cut.createAndEnableDefaultTheme(ThemeType.PORTAL_NEXT, EXECUTION_CONTEXT);

            assertThat(result)
                .hasFieldOrPropertyWithValue("referenceId", EXPECTED_DEFAULT_PORTAL_NEXT_THEME.getReferenceId())
                .hasFieldOrPropertyWithValue("referenceType", EXPECTED_DEFAULT_PORTAL_NEXT_THEME.getReferenceType())
                .hasFieldOrPropertyWithValue("name", EXPECTED_DEFAULT_PORTAL_NEXT_THEME.getName())
                .hasFieldOrPropertyWithValue("definitionPortalNext", EXPECTED_DEFAULT_PORTAL_NEXT_THEME.getDefinitionPortalNext())
                .hasFieldOrPropertyWithValue("type", EXPECTED_DEFAULT_PORTAL_NEXT_THEME.getType())
                .hasFieldOrPropertyWithValue("enabled", true)
                .satisfies(res -> {
                    assertThat(res.getId()).isNotBlank();
                    assertThat(res.getCreatedAt()).isNotNull();
                    assertThat(res.getUpdatedAt()).isNotNull();
                })
                .isEqualTo(themeCrudService.get(result.getId()));
        }

        @Test
        void should_create_portal_theme() {
            var result = cut.createAndEnableDefaultTheme(ThemeType.PORTAL, EXECUTION_CONTEXT);

            assertThat(result)
                .hasFieldOrPropertyWithValue("referenceId", EXECUTION_CONTEXT.getEnvironmentId())
                .hasFieldOrPropertyWithValue("referenceType", Theme.ReferenceType.ENVIRONMENT)
                .hasFieldOrPropertyWithValue("type", ThemeType.PORTAL)
                .hasFieldOrPropertyWithValue("enabled", true);
        }
    }
}
