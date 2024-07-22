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

import inmemory.*;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.exception.ThemeDefinitionInvalidException;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.exception.ThemeTypeInvalidException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.model.theme.portal.ThemeComponentDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeCssDefinition;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateThemeUseCaseTest {

    private final String PORTAL_NEXT_THEME_ID = "portal-next";
    private final String PORTAL_THEME_ID = "portal";
    private final String ENV_ID = "env-id";
    private final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", ENV_ID);
    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ThemeQueryServiceInMemory themeQueryServiceInMemory = new ThemeQueryServiceInMemory(themeCrudService);
    UpdateThemeUseCase cut;

    @BeforeEach
    void setUp() {
        themeCrudService.initWith(List.of(aPortalTheme(false), aPortalNextTheme(false)));
        cut =
            new UpdateThemeUseCase(
                new ValidateThemeDomainService(themeCrudService),
                new ThemeDomainService(themeCrudService),
                new CurrentThemeDomainService(themeQueryServiceInMemory, themeCrudService),
                themeCrudService
            );
    }

    @AfterEach
    void tearDown() {
        Stream.of(themeCrudService, themeQueryServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_throw_error_if_theme_does_not_exist() {
        assertThatThrownBy(() ->
                cut.execute(
                    UpdateThemeUseCase.Input
                        .builder()
                        .updateTheme(UpdateTheme.builder().id("does-not-exist").build())
                        .executionContext(EXECUTION_CONTEXT)
                        .build()
                )
            )
            .isInstanceOf(ThemeNotFoundException.class);
    }

    @Test
    void should_throw_error_if_theme_is_not_in_scope() {
        assertThatThrownBy(() ->
                cut.execute(
                    UpdateThemeUseCase.Input
                        .builder()
                        .updateTheme(UpdateTheme.builder().id(PORTAL_THEME_ID).build())
                        .executionContext(new ExecutionContext("org-id", "out-of-scope"))
                        .build()
                )
            )
            .isInstanceOf(ThemeNotFoundException.class);
    }

    @Test
    void should_throw_error_if_theme_type_different() {
        assertThatThrownBy(() ->
                cut.execute(
                    UpdateThemeUseCase.Input
                        .builder()
                        .updateTheme(UpdateTheme.builder().id(PORTAL_THEME_ID).type(ThemeType.PORTAL_NEXT).build())
                        .executionContext(EXECUTION_CONTEXT)
                        .build()
                )
            )
            .isInstanceOf(ThemeTypeInvalidException.class);
    }

    @Test
    void should_throw_error_if_portal_theme_definition_missing() {
        assertThatThrownBy(() ->
                cut.execute(
                    UpdateThemeUseCase.Input
                        .builder()
                        .updateTheme(UpdateTheme.builder().id(PORTAL_THEME_ID).type(ThemeType.PORTAL).build())
                        .executionContext(EXECUTION_CONTEXT)
                        .build()
                )
            )
            .isInstanceOf(ThemeDefinitionInvalidException.class);
    }

    @Test
    void should_throw_error_if_portal_next_theme_definition_missing() {
        assertThatThrownBy(() ->
                cut.execute(
                    UpdateThemeUseCase.Input
                        .builder()
                        .updateTheme(UpdateTheme.builder().id(PORTAL_NEXT_THEME_ID).type(ThemeType.PORTAL_NEXT).build())
                        .executionContext(EXECUTION_CONTEXT)
                        .build()
                )
            )
            .isInstanceOf(ThemeDefinitionInvalidException.class);
    }

    @Test
    public void should_update_portal_theme() {
        var cssDefinition = new ThemeCssDefinition();
        cssDefinition.setName("background");
        cssDefinition.setValue("#000");

        var componentDefinition = new ThemeComponentDefinition();
        componentDefinition.setCss(List.of(cssDefinition));
        componentDefinition.setName("title");

        var newPortalDefinition = new io.gravitee.rest.api.model.theme.portal.ThemeDefinition();
        newPortalDefinition.setData(List.of(componentDefinition));

        var updateTheme = UpdateTheme
            .builder()
            .id(PORTAL_THEME_ID)
            .type(ThemeType.PORTAL)
            .name("my new name")
            .enabled(true)
            .logo("my new logo")
            .backgroundImage("my new background image")
            .favicon("my new favicon")
            .optionalLogo("my new optional logo")
            .definitionPortal(newPortalDefinition)
            .build();

        var result = cut.execute(UpdateThemeUseCase.Input.builder().updateTheme(updateTheme).executionContext(EXECUTION_CONTEXT).build());

        assertThat(result)
            .isNotNull()
            .extracting(UpdateThemeUseCase.Output::result)
            .satisfies(res -> {
                updateAppliedCorrectly(res, updateTheme);
            });
    }

    @Test
    public void should_update_portal_next_theme() {
        var updateTheme = UpdateTheme
            .builder()
            .id(PORTAL_NEXT_THEME_ID)
            .type(ThemeType.PORTAL_NEXT)
            .name("my new name")
            .enabled(true)
            .logo("my new logo")
            .backgroundImage("my new background image")
            .favicon("my new favicon")
            .optionalLogo("my new optional logo")
            .definitionPortalNext(
                ThemeDefinition.builder().color(ThemeDefinition.Color.builder().primary("#999").build()).customCss("something cool").build()
            )
            .build();

        var result = cut.execute(UpdateThemeUseCase.Input.builder().updateTheme(updateTheme).executionContext(EXECUTION_CONTEXT).build());

        assertThat(result)
            .isNotNull()
            .extracting(UpdateThemeUseCase.Output::result)
            .satisfies(res -> {
                updateAppliedCorrectly(res, updateTheme);
            });
    }

    @Test
    public void should_change_enabled_theme() {
        themeCrudService.reset();

        var currentThemeId = "current-theme-id";

        themeCrudService.initWith(
            List.of(
                aPortalNextTheme(false),
                Theme
                    .builder()
                    .id(currentThemeId)
                    .type(ThemeType.PORTAL_NEXT)
                    .referenceId(ENV_ID)
                    .referenceType(Theme.ReferenceType.ENVIRONMENT)
                    .enabled(true)
                    .build()
            )
        );

        var updateTheme = UpdateTheme
            .builder()
            .id(PORTAL_NEXT_THEME_ID)
            .type(ThemeType.PORTAL_NEXT)
            .name("my new name")
            .enabled(true)
            .logo("my new logo")
            .backgroundImage("my new background image")
            .favicon("my new favicon")
            .optionalLogo("my new optional logo")
            .definitionPortalNext(
                ThemeDefinition.builder().color(ThemeDefinition.Color.builder().primary("#999").build()).customCss("something cool").build()
            )
            .build();

        var result = cut.execute(UpdateThemeUseCase.Input.builder().updateTheme(updateTheme).executionContext(EXECUTION_CONTEXT).build());

        assertThat(result)
            .isNotNull()
            .extracting(UpdateThemeUseCase.Output::result)
            .satisfies(res -> {
                updateAppliedCorrectly(res, updateTheme);
            });

        assertThat(themeCrudService.get(currentThemeId)).isNotNull().hasFieldOrPropertyWithValue("enabled", false);
    }

    private void updateAppliedCorrectly(Theme result, UpdateTheme updateTheme) {
        assertThat(result)
            .isNotNull()
            .satisfies(res -> {
                assertThat(res.getId()).isEqualTo(updateTheme.getId());
                assertThat(res.getType()).isEqualTo(updateTheme.getType());
                assertThat(res.getName()).isEqualTo(updateTheme.getName());
                assertThat(res.isEnabled()).isEqualTo(updateTheme.isEnabled());
                assertThat(res.getLogo()).isEqualTo(updateTheme.getLogo());
                assertThat(res.getBackgroundImage()).isEqualTo(updateTheme.getBackgroundImage());
                assertThat(res.getFavicon()).isEqualTo(updateTheme.getFavicon());
                assertThat(res.getOptionalLogo()).isEqualTo(updateTheme.getOptionalLogo());
                assertThat(res.getDefinitionPortal()).isEqualTo(updateTheme.getDefinitionPortal());
                assertThat(res.getDefinitionPortalNext()).isEqualTo(updateTheme.getDefinitionPortalNext());
            });
    }

    private Theme aPortalTheme(boolean enabled) {
        var portalDefinition = new io.gravitee.rest.api.model.theme.portal.ThemeDefinition();
        portalDefinition.setData(List.of());

        return Theme
            .builder()
            .id(PORTAL_THEME_ID)
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
            .id(PORTAL_NEXT_THEME_ID)
            .name("a portal next theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceId(EXECUTION_CONTEXT.getEnvironmentId())
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .definitionPortalNext(ThemeDefinition.builder().color(ThemeDefinition.Color.builder().primary("#fff").build()).build())
            .enabled(enabled)
            .build();
    }
}
