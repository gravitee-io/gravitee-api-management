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

import inmemory.ThemeCrudServiceInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.theme.model.NewTheme;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.model.theme.portal.ThemeComponentDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeCssDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeCssType;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ThemeDomainServiceTest {

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ThemeDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new ThemeDomainService(themeCrudService);
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Nested
    class Create {

        @Test
        void should_create_portal_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var newTheme = NewTheme.builder().name("name").type(ThemeType.PORTAL).definitionPortal(portalDefinition).build();

            var result = cut.create(newTheme);
            assertThat(result).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull().isEqualTo(result.getUpdatedAt());
            assertThat(result.getId()).isNotBlank();
        }

        @Test
        void should_create_portal_next_theme() {
            var portalDefinition = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                .builder()
                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                .build();

            var newTheme = NewTheme.builder().name("name").type(ThemeType.PORTAL_NEXT).definitionPortalNext(portalDefinition).build();

            var result = cut.create(newTheme);
            assertThat(result).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull().isEqualTo(result.getUpdatedAt());
            assertThat(result.getId()).isNotBlank();
        }
    }

    @Nested
    class Update {

        @BeforeEach
        void setUp() {
            var portalThemeDefinition = new ThemeDefinition();

            var css = new ThemeCssDefinition();
            css.setName("border");
            css.setType(ThemeCssType.STRING);
            css.setValue("2px solid black");
            css.setDescription("Title border");

            var themeComponentDefinition = new ThemeComponentDefinition();
            themeComponentDefinition.setName("title");

            themeComponentDefinition.setCss(List.of(css));
            portalThemeDefinition.setData(List.of(themeComponentDefinition));

            themeCrudService.initWith(
                List.of(
                    Theme
                        .builder()
                        .id("portal")
                        .type(ThemeType.PORTAL)
                        .referenceId("ref-id")
                        .referenceType(Theme.ReferenceType.ENVIRONMENT)
                        .definitionPortal(portalThemeDefinition)
                        .enabled(true)
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .build(),
                    Theme
                        .builder()
                        .id("portal-next")
                        .type(ThemeType.PORTAL_NEXT)
                        .referenceId("ref-id")
                        .referenceType(Theme.ReferenceType.ENVIRONMENT)
                        .definitionPortalNext(
                            io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                                .builder()
                                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                                .build()
                        )
                        .enabled(true)
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .build()
                )
            );
        }

        @Test
        void should_update_portal_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());

            var updateTheme = UpdateTheme
                .builder()
                .id("portal")
                .name("Portal name")
                .type(ThemeType.PORTAL)
                .definitionPortal(portalDefinition)
                .enabled(false)
                .build();
            var existingTheme = themeCrudService.get("portal");

            var result = cut.update(updateTheme, existingTheme);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(updateTheme.getId());
            assertThat(result.getName()).isEqualTo(updateTheme.getName());
            assertThat(result.getUpdatedAt()).isNotNull().isAfter(existingTheme.getUpdatedAt());
            assertThat(result.getDefinitionPortal()).isEqualTo(updateTheme.getDefinitionPortal());
            assertThat(result.isEnabled()).isEqualTo(updateTheme.isEnabled());
            assertThat(result.getReferenceId()).isEqualTo(existingTheme.getReferenceId());
            assertThat(result.getReferenceType()).isEqualTo(existingTheme.getReferenceType());
            assertThat(result.getType()).isEqualTo(existingTheme.getType());
        }

        @Test
        void should_update_portal_next_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());

            var updateTheme = UpdateTheme
                .builder()
                .id("portal-next")
                .name("Portal next name")
                .type(ThemeType.PORTAL_NEXT)
                .definitionPortalNext(
                    io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                        .builder()
                        .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#000").build())
                        .build()
                )
                .enabled(true)
                .build();
            var existingTheme = themeCrudService.get("portal-next");

            var result = cut.update(updateTheme, existingTheme);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(updateTheme.getId());
            assertThat(result.getName()).isEqualTo(updateTheme.getName());
            assertThat(result.getUpdatedAt()).isNotNull().isAfter(existingTheme.getUpdatedAt());
            assertThat(result.getDefinitionPortalNext()).isEqualTo(updateTheme.getDefinitionPortalNext());
            assertThat(result.isEnabled()).isEqualTo(updateTheme.isEnabled());
            assertThat(result.getReferenceId()).isEqualTo(existingTheme.getReferenceId());
            assertThat(result.getReferenceType()).isEqualTo(existingTheme.getReferenceType());
            assertThat(result.getType()).isEqualTo(existingTheme.getType());
        }

        @Test
        void should_throw_error_if_not_found() {
            assertThatThrownBy(() -> cut.update(UpdateTheme.builder().build(), Theme.builder().build()))
                .isInstanceOf(TechnicalDomainException.class);
        }
    }
}
