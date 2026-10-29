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

import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeImageWriteValidatorInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalThemeUseCaseTest {

    private static final String LEGACY_SVG = "data:image/svg+xml;base64,PHN2Zy8+";

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private ValidatePortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidatePortalThemeUseCase(new ValidateThemeDomainService(themeCrudService, new ThemeImageWriteValidatorInMemory()));
    }

    @Test
    void should_preview_metadata_changes_with_unchanged_legacy_images_without_persisting() {
        var existing = legacyTheme();
        themeCrudService.initWith(List.of(existing));
        var input = specWithLegacyImages(null);

        var output = useCase.execute(input);

        assertThat(output.errors()).isEmpty();
        assertThat(output.theme().getName()).isEqualTo(input.name());
        assertThat(output.theme().getDefinitionPortalNext()).isEqualTo(input.definitionPortalNext());
        assertThat(output.theme())
            .extracting(Theme::getLogo, Theme::getOptionalLogo, Theme::getFavicon, Theme::getBackgroundImage)
            .containsExactly(LEGACY_SVG, LEGACY_SVG, LEGACY_SVG, LEGACY_SVG);
        assertThat(themeCrudService.storage()).containsExactly(existing);
        assertThat(existing.getName()).isEqualTo("Old theme");
        assertThat(existing.getDefinitionPortalNext().getCustomCss()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
    void should_reject_changed_legacy_image_in_preview_without_rejecting_unchanged_fields(String field) {
        var existing = legacyTheme();
        themeCrudService.initWith(List.of(existing));

        var output = useCase.execute(specWithLegacyImages(field));

        assertThat(output.errors())
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).isEqualTo("Invalid theme image in spec." + field + ": unsupported image media type");
            });
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @ParameterizedTest
    @ValueSource(strings = { "another-environment", "another-theme" })
    void should_not_allow_legacy_images_from_another_scope_in_preview(String scope) {
        var existing = legacyTheme();
        if (scope.equals("another-environment")) {
            existing.setReferenceId(scope);
        } else {
            existing.setId(HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(scope).id());
        }
        themeCrudService.initWith(List.of(existing));

        var output = useCase.execute(specWithLegacyImages(null));

        assertThat(output.errors())
            .hasSize(4)
            .allMatch(error -> error.isSevere());
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @Test
    void should_reject_copying_a_legacy_image_to_another_field_in_preview() {
        var existing = legacyTheme().toBuilder().favicon(null).build();
        themeCrudService.initWith(List.of(existing));

        var output = useCase.execute(specWithLegacyImages(null));

        assertThat(output.errors())
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).isEqualTo("Invalid theme image in spec.favicon: unsupported image media type");
            });
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void should_report_only_the_hrid_error_when_the_hrid_is_missing_or_blank(String hrid) {
        var output = useCase.execute(specWith(hrid, "My Theme", new ThemeDefinition()));

        assertThat(output.errors())
            .singleElement()
            .satisfies(error -> {
                assertThat(error.isSevere()).isTrue();
                assertThat(error.getMessage()).isEqualTo("spec.hrid must not be blank");
            });
        assertThat(output.theme().getId()).isNull();
        assertThat(themeCrudService.storage()).isEmpty();
    }

    @Test
    void should_return_severe_errors_for_invalid_images_without_throwing() {
        var input = new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            "my-theme",
            "My Theme",
            new ThemeDefinition(),
            "data:text/html;base64,PGh0bWw+PC9odG1sPg==",
            null,
            "data:image/png;base64,not-base64",
            null
        );

        var output = useCase.execute(input);

        assertThat(output.errors())
            .hasSize(2)
            .allMatch(error -> error.isSevere());
        assertThat(output.errors()).anyMatch(error -> error.getMessage().contains("spec.logo"));
        assertThat(output.errors()).anyMatch(error -> error.getMessage().contains("spec.favicon"));
        assertThat(output.theme().getName()).isEqualTo(input.name());
    }

    @Test
    void should_preserve_valid_data_uri_source_in_the_preview() {
        var image = "DATA:IMAGE/X-PNG;BASE64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jrFYAAAAASUVORK5CYII=";
        var input = new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            "my-theme",
            "My Theme",
            new ThemeDefinition(),
            image,
            null,
            null,
            null
        );

        var output = useCase.execute(input);

        assertThat(output.errors()).isEmpty();
        assertThat(output.theme().getLogo()).isEqualTo(image);
    }

    @Test
    void returns_no_errors_and_previews_theme_for_well_formed_input() {
        var output = useCase.execute(specWith("my-theme", "My Theme", new ThemeDefinition()));

        assertThat(output.errors()).isEmpty();
        assertThat(output.theme().getName()).isEqualTo("My Theme");
        assertThat(output.theme().getId()).isNotBlank();
        assertThat(output.theme().isEnabled()).isFalse();
    }

    @Test
    void surfaces_severe_errors_for_blank_name() {
        var output = useCase.execute(specWith("my-theme", "  ", new ThemeDefinition()));

        assertThat(output.errors()).anyMatch(e -> e.isSevere() && e.getMessage().contains("spec.name"));
    }

    @Test
    void surfaces_severe_error_when_definition_null() {
        var output = useCase.execute(specWith("my-theme", "Ok", null));

        assertThat(output.errors()).anyMatch(e -> e.isSevere() && e.getMessage().contains("spec.definitionPortalNext"));
    }

    private static ValidateThemeDomainService.Input specWith(String hrid, String name, ThemeDefinition definition) {
        return new ValidateThemeDomainService.Input(AUDIT_INFO, hrid, name, definition, null, null, null, null);
    }

    private static ValidateThemeDomainService.Input specWithLegacyImages(String changedField) {
        var changedSvg = "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=";
        return new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            "my-theme",
            "Updated theme",
            ThemeDefinition.builder().customCss("body { color: blue; }").build(),
            "logo".equals(changedField) ? changedSvg : LEGACY_SVG,
            "optionalLogo".equals(changedField) ? changedSvg : LEGACY_SVG,
            "favicon".equals(changedField) ? changedSvg : LEGACY_SVG,
            "backgroundImage".equals(changedField) ? changedSvg : LEGACY_SVG
        );
    }

    private static Theme legacyTheme() {
        return Theme.builder()
            .id(HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid("my-theme").id())
            .name("Old theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(AUDIT_INFO.environmentId())
            .definitionPortalNext(new ThemeDefinition())
            .logo(LEGACY_SVG)
            .optionalLogo(LEGACY_SVG)
            .favicon(LEGACY_SVG)
            .backgroundImage(LEGACY_SVG)
            .build();
    }
}
