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
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.domain_service.CurrentThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.exception.ThemeDefinitionInvalidException;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class CreateThemeUseCaseTest {

    private final ThemeCrudServiceInMemory themeCrudServiceInMemory = new ThemeCrudServiceInMemory();
    private ThemeQueryServiceInMemory themeQueryServiceInMemory;
    private CreateThemeUseCase cut;

    @BeforeEach
    void setUp() {
        themeQueryServiceInMemory = new ThemeQueryServiceInMemory(themeCrudServiceInMemory);
        var currentThemeDomainService = new CurrentThemeDomainService(themeQueryServiceInMemory, themeCrudServiceInMemory);
        var themeDomainService = new ThemeDomainService(themeCrudServiceInMemory);
        cut = new CreateThemeUseCase(currentThemeDomainService, themeDomainService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(themeCrudServiceInMemory, themeQueryServiceInMemory).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_not_allow_null_portal_definition() {
        assertThatThrownBy(() ->
                cut.execute(
                    CreateThemeUseCase.Input
                        .builder()
                        .newTheme(NewTheme.builder().type(ThemeType.PORTAL).definitionPortal(null).build())
                        .build()
                )
            )
            .isInstanceOf(ThemeDefinitionInvalidException.class);
    }

    @Test
    void should_not_allow_null_portal_next_definition() {
        assertThatThrownBy(() ->
                cut.execute(
                    CreateThemeUseCase.Input
                        .builder()
                        .newTheme(NewTheme.builder().type(ThemeType.PORTAL_NEXT).definitionPortalNext(null).build())
                        .build()
                )
            )
            .isInstanceOf(ThemeDefinitionInvalidException.class);
    }

    @Test
    void should_create_portal_theme() {
        var portalDefinition = new ThemeDefinition();
        portalDefinition.setData(List.of());
        var result = cut.execute(
            CreateThemeUseCase.Input
                .builder()
                .newTheme(
                    NewTheme
                        .builder()
                        .name("portal theme")
                        .definitionPortal(portalDefinition)
                        .logo("logo")
                        .type(ThemeType.PORTAL)
                        .enabled(false)
                        .backgroundImage("background")
                        .favicon("favicon")
                        .optionalLogo("optional")
                        .build()
                )
                .build()
        );

        assertThat(result.created())
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "portal theme")
            .hasFieldOrPropertyWithValue("type", ThemeType.PORTAL)
            .hasFieldOrPropertyWithValue("enabled", false)
            .extracting("id")
            .isNotNull();
    }

    @Test
    void should_create_portal_next_theme_and_disable_current_theme() {
        var currentPortalNextTheme = Theme.builder().id("current-theme").type(ThemeType.PORTAL_NEXT).enabled(true).build();

        themeCrudServiceInMemory.initWith(List.of(currentPortalNextTheme));

        var portalDefinition = new io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition();
        portalDefinition.setPrimary("#fff");
        var result = cut.execute(
            CreateThemeUseCase.Input
                .builder()
                .newTheme(
                    NewTheme
                        .builder()
                        .name("new portal next theme")
                        .definitionPortalNext(portalDefinition)
                        .logo("logo")
                        .type(ThemeType.PORTAL_NEXT)
                        .enabled(true)
                        .backgroundImage("background")
                        .favicon("favicon")
                        .optionalLogo("optional")
                        .build()
                )
                .build()
        );

        assertThat(result.created())
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "new portal next theme")
            .hasFieldOrPropertyWithValue("type", ThemeType.PORTAL_NEXT)
            .hasFieldOrPropertyWithValue("enabled", true)
            .extracting("id")
            .isNotNull();
        var disabledPortalNextTheme = currentPortalNextTheme.toBuilder().enabled(false).build();

        assertThat(themeCrudServiceInMemory.storage()).contains(disabledPortalNextTheme);
    }

    @Test
    void should_set_enabled_to_false_if_not_specified() {
        var portalDefinition = new io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition();
        portalDefinition.setPrimary("#fff");
        var result = cut.execute(
            CreateThemeUseCase.Input
                .builder()
                .newTheme(
                    NewTheme
                        .builder()
                        .name("new portal next theme")
                        .definitionPortalNext(portalDefinition)
                        .logo("logo")
                        .type(ThemeType.PORTAL_NEXT)
                        .backgroundImage("background")
                        .favicon("favicon")
                        .optionalLogo("optional")
                        .build()
                )
                .build()
        );

        assertThat(result.created())
            .isNotNull()
            .hasFieldOrPropertyWithValue("name", "new portal next theme")
            .hasFieldOrPropertyWithValue("type", ThemeType.PORTAL_NEXT)
            .hasFieldOrPropertyWithValue("enabled", false)
            .extracting("id")
            .isNotNull();
    }
}
