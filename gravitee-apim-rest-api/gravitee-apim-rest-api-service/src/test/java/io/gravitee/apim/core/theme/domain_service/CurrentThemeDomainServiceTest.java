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

import inmemory.InMemoryAlternative;
import inmemory.ParametersDomainServiceInMemory;
import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeQueryServiceInMemory;
import inmemory.ThemeServiceLegacyWrapperInMemory;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CurrentThemeDomainServiceTest {

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ThemeQueryServiceInMemory themeQueryService;
    CurrentThemeDomainService cut;

    private final String ENV_ID = "env-id";

    @BeforeEach
    void setUp() {
        themeQueryService = new ThemeQueryServiceInMemory(themeCrudService);
        cut = new CurrentThemeDomainService(themeQueryService, themeCrudService);
    }

    @AfterEach
    void tearDown() {
        Stream.of(themeCrudService, themeQueryService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class DisablePreviousEnabledTheme {

        @Test
        void should_do_nothing_if_no_themes() {
            assertThat(themeCrudService.storage()).hasSize(0);

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var newTheme = Theme
                .builder()
                .id("new-theme")
                .name("name")
                .type(ThemeType.PORTAL)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortal(portalDefinition)
                .enabled(true)
                .build();
            cut.disablePreviousEnabledTheme(newTheme);

            assertThat(themeCrudService.storage()).hasSize(0);
        }

        @Test
        void should_disable_previous_portal_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var previousPortalTheme = Theme
                .builder()
                .id("old-theme")
                .name("name")
                .type(ThemeType.PORTAL)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortal(portalDefinition)
                .enabled(true)
                .build();

            var newTheme = Theme
                .builder()
                .id("new-theme")
                .name("name")
                .type(ThemeType.PORTAL)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortal(portalDefinition)
                .enabled(true)
                .build();

            themeCrudService.initWith(Arrays.asList(previousPortalTheme, newTheme));

            cut.disablePreviousEnabledTheme(newTheme);

            var disabledPreviousTheme = previousPortalTheme.toBuilder().enabled(false).build();
            assertThat(themeCrudService.storage()).contains(disabledPreviousTheme);
        }

        @Test
        void should_disable_previous_portal_next_theme() {
            var previousPortalTheme = Theme
                .builder()
                .id("old-theme")
                .name("name")
                .type(ThemeType.PORTAL_NEXT)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortalNext(
                    io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                        .builder()
                        .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("fff").build())
                        .build()
                )
                .enabled(true)
                .build();

            var newTheme = Theme
                .builder()
                .id("new-theme")
                .name("name")
                .type(ThemeType.PORTAL_NEXT)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortalNext(
                    io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                        .builder()
                        .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("fff").build())
                        .build()
                )
                .enabled(true)
                .build();

            themeCrudService.initWith(Arrays.asList(previousPortalTheme, newTheme));

            cut.disablePreviousEnabledTheme(newTheme);

            var disabledPreviousTheme = previousPortalTheme.toBuilder().enabled(false).build();
            assertThat(themeCrudService.storage()).contains(disabledPreviousTheme);
        }

        @Test
        void should_not_disable_current_theme_with_different_type() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());

            var currentPortalTheme = Theme
                .builder()
                .id("portal-theme")
                .name("name")
                .type(ThemeType.PORTAL)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortal(portalDefinition)
                .enabled(true)
                .build();

            var newPortalNextTheme = Theme
                .builder()
                .id("new-portal-next-theme")
                .name("name")
                .type(ThemeType.PORTAL_NEXT)
                .referenceId(ENV_ID)
                .referenceType(Theme.ReferenceType.ENVIRONMENT)
                .definitionPortalNext(
                    io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                        .builder()
                        .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("fff").build())
                        .build()
                )
                .enabled(true)
                .build();

            themeCrudService.initWith(Arrays.asList(currentPortalTheme, newPortalNextTheme));

            cut.disablePreviousEnabledTheme(newPortalNextTheme);

            assertThat(themeCrudService.storage()).contains(currentPortalTheme);
        }
    }
}
