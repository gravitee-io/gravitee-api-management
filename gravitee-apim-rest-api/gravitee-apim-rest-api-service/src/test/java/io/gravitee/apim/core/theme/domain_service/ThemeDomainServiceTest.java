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
import inmemory.ThemeImageWriteValidatorInMemory;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class ThemeDomainServiceTest {

    private static final String LEGACY_SVG = "data:image/svg+xml;base64,PHN2Zy8+";
    private static final String PNG_IMAGE =
        "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jrFYAAAAASUVORK5CYII=";

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ThemeDomainService cut;

    @BeforeEach
    void setUp() {
        cut = new ThemeDomainService(themeCrudService, new ThemeImageWriteValidatorInMemory());
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Nested
    class Create {

        @ParameterizedTest
        @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
        void should_reject_an_invalid_image_without_creating_the_theme(String field) {
            var theme = NewTheme.builder().name("Theme").build();
            var invalid = "data:text/html;base64,PGh0bWw+PC9odG1sPg==";
            switch (field) {
                case "logo" -> theme.setLogo(invalid);
                case "optionalLogo" -> theme.setOptionalLogo(invalid);
                case "favicon" -> theme.setFavicon(invalid);
                case "backgroundImage" -> theme.setBackgroundImage(invalid);
                default -> throw new IllegalArgumentException("Unexpected image field: " + field);
            }

            assertThatThrownBy(() -> cut.create(theme))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining(field);
            assertThat(themeCrudService.storage()).isEmpty();
        }

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
            var portalDefinition = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.builder()
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
                    Theme.builder()
                        .id("portal")
                        .type(ThemeType.PORTAL)
                        .referenceId("ref-id")
                        .referenceType(Theme.ReferenceType.ENVIRONMENT)
                        .definitionPortal(portalThemeDefinition)
                        .enabled(true)
                        .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .updatedAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                        .build(),
                    Theme.builder()
                        .id("portal-next")
                        .type(ThemeType.PORTAL_NEXT)
                        .referenceId("ref-id")
                        .referenceType(Theme.ReferenceType.ENVIRONMENT)
                        .definitionPortalNext(
                            io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.builder()
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

        @ParameterizedTest
        @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
        void should_reject_an_invalid_image_without_updating_the_theme(String field) {
            var existing = themeCrudService.get("portal");
            var update = UpdateTheme.builder().id("portal").name("Changed").build();
            var invalid = "data:text/html;base64,PGh0bWw+PC9odG1sPg==";
            switch (field) {
                case "logo" -> update.setLogo(invalid);
                case "optionalLogo" -> update.setOptionalLogo(invalid);
                case "favicon" -> update.setFavicon(invalid);
                case "backgroundImage" -> update.setBackgroundImage(invalid);
                default -> throw new IllegalArgumentException("Unexpected image field: " + field);
            }

            assertThatThrownBy(() -> cut.update(update, existing))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining(field);
            assertThat(themeCrudService.get("portal")).isEqualTo(existing);
        }

        @ParameterizedTest
        @ValueSource(strings = { "portal", "portal-next" })
        void should_preserve_unchanged_legacy_images_when_updating_other_theme_properties(String themeId) {
            var existing = themeCrudService.get(themeId);
            existing.setLogo(LEGACY_SVG);
            existing.setOptionalLogo(LEGACY_SVG);
            existing.setFavicon(LEGACY_SVG);
            existing.setBackgroundImage(LEGACY_SVG);
            var original = existing.toBuilder().build();
            var update = updateFrom(existing);
            update.setName("Changed");
            update.setEnabled(false);

            var result = cut.update(update, existing);

            assertThat(result.getName()).isEqualTo("Changed");
            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getLogo()).isEqualTo(LEGACY_SVG);
            assertThat(result.getOptionalLogo()).isEqualTo(LEGACY_SVG);
            assertThat(result.getFavicon()).isEqualTo(LEGACY_SVG);
            assertThat(result.getBackgroundImage()).isEqualTo(LEGACY_SVG);
            assertThat(themeCrudService.get(themeId)).isEqualTo(result);
            assertThat(existing).isEqualTo(original);
        }

        @Test
        void should_validate_a_replacement_image_while_preserving_other_legacy_images() {
            var existing = themeCrudService.get("portal");
            existing.setLogo(LEGACY_SVG);
            existing.setFavicon(LEGACY_SVG);
            var update = updateFrom(existing);
            update.setLogo(PNG_IMAGE);

            var result = cut.update(update, existing);

            assertThat(result.getLogo()).isEqualTo(PNG_IMAGE);
            assertThat(result.getFavicon()).isEqualTo(LEGACY_SVG);
            assertThat(existing.getLogo()).isEqualTo(LEGACY_SVG);
        }

        @ParameterizedTest
        @ValueSource(strings = { "data:image/svg+xml;base64,PHN2ZyAvPg==", "data:image/png;base64,PGh0bWw+PC9odG1sPg==" })
        void should_reject_a_changed_invalid_image_and_preserve_the_original_theme(String replacement) {
            var existing = themeCrudService.get("portal");
            existing.setLogo(LEGACY_SVG);
            existing.setFavicon(LEGACY_SVG);
            var original = existing.toBuilder().build();
            var update = updateFrom(existing);
            update.setName("Changed");
            update.setEnabled(false);
            update.setLogo(replacement);

            assertThatThrownBy(() -> cut.update(update, existing))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("logo");

            assertThat(existing).isEqualTo(original);
            assertThat(themeCrudService.get("portal")).isEqualTo(existing);
        }

        @Test
        void should_reject_copying_a_legacy_image_to_another_field() {
            var existing = themeCrudService.get("portal");
            existing.setLogo(LEGACY_SVG);
            var update = updateFrom(existing);
            update.setFavicon(existing.getLogo());

            assertThatThrownBy(() -> cut.update(update, existing))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("favicon");
            assertThat(themeCrudService.get("portal").getFavicon()).isNull();
        }

        @Test
        void should_allow_clearing_legacy_images() {
            var existing = themeCrudService.get("portal");
            existing.setLogo(LEGACY_SVG);
            existing.setOptionalLogo(LEGACY_SVG);
            existing.setFavicon(LEGACY_SVG);
            existing.setBackgroundImage(LEGACY_SVG);
            var update = UpdateTheme.builder().id("portal").name("Changed").optionalLogo("").build();

            var result = cut.update(update, existing);

            assertThat(result.getLogo()).isNull();
            assertThat(result.getOptionalLogo()).isEmpty();
            assertThat(result.getFavicon()).isNull();
            assertThat(result.getBackgroundImage()).isNull();
        }

        private UpdateTheme updateFrom(Theme theme) {
            return UpdateTheme.builder()
                .id(theme.getId())
                .name(theme.getName())
                .type(theme.getType())
                .definitionPortal(theme.getDefinitionPortal())
                .definitionPortalNext(theme.getDefinitionPortalNext())
                .enabled(theme.isEnabled())
                .logo(theme.getLogo())
                .optionalLogo(theme.getOptionalLogo())
                .favicon(theme.getFavicon())
                .backgroundImage(theme.getBackgroundImage())
                .build();
        }

        @Test
        void should_update_portal_theme() {
            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());

            var updateTheme = UpdateTheme.builder()
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

            var updateTheme = UpdateTheme.builder()
                .id("portal-next")
                .name("Portal next name")
                .type(ThemeType.PORTAL_NEXT)
                .definitionPortalNext(
                    io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.builder()
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
            assertThatThrownBy(() -> cut.update(UpdateTheme.builder().build(), Theme.builder().build())).isInstanceOf(
                TechnicalDomainException.class
            );
        }
    }
}
