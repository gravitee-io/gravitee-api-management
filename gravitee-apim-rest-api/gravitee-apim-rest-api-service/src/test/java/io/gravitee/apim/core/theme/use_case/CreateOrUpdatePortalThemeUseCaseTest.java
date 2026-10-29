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

import inmemory.ThemeCrudServiceInMemory;
import inmemory.ThemeImageWriteValidatorInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.theme.domain_service.ThemeDomainService;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalThemeUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String THEME_HRID = "my-theme";
    private static final String LEGACY_SVG = "data:image/svg+xml;base64,PHN2Zy8+";

    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private CreateOrUpdatePortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalThemeUseCase(
            new ValidateThemeDomainService(themeCrudService, new ThemeImageWriteValidatorInMemory()),
            new ThemeDomainService(themeCrudService, new ThemeImageWriteValidatorInMemory()),
            themeCrudService
        );
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Test
    void should_apply_metadata_changes_with_unchanged_legacy_images() {
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
        assertThat(themeCrudService.storage()).containsExactly(output.theme());
        assertThat(existing.getName()).isEqualTo("Old theme");
        assertThat(existing.getDefinitionPortalNext().getCustomCss()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "logo", "optionalLogo", "favicon", "backgroundImage" })
    void should_reject_changed_legacy_image_before_applying_without_rejecting_unchanged_fields(String field) {
        var existing = legacyTheme();
        themeCrudService.initWith(List.of(existing));

        assertThatThrownBy(() -> useCase.execute(specWithLegacyImages(field)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Invalid theme image in spec." + field + ": unsupported image media type");
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @ParameterizedTest
    @ValueSource(strings = { "another-environment", "another-theme" })
    void should_not_allow_legacy_images_from_another_scope_when_applying(String scope) {
        var existing = legacyTheme();
        if (scope.equals("another-environment")) {
            existing.setReferenceId(scope);
        } else {
            existing.setId(HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(scope).id());
        }
        themeCrudService.initWith(List.of(existing));

        assertThatThrownBy(() -> useCase.execute(specWithLegacyImages(null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("spec.logo")
            .hasMessageContaining("spec.optionalLogo")
            .hasMessageContaining("spec.favicon")
            .hasMessageContaining("spec.backgroundImage");
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @Test
    void should_reject_invalid_images_before_creating_a_theme() {
        var input = new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            THEME_HRID,
            "My Theme",
            new ThemeDefinition(),
            null,
            null,
            "data:text/html;base64,PGh0bWw+PC9odG1sPg==",
            null
        );

        assertThatThrownBy(() -> useCase.execute(input))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("spec.favicon");
        assertThat(themeCrudService.storage()).isEmpty();
    }

    @Test
    void should_reject_copying_a_legacy_image_to_another_field_when_applying() {
        var existing = legacyTheme().toBuilder().favicon(null).build();
        themeCrudService.initWith(List.of(existing));

        assertThatThrownBy(() -> useCase.execute(specWithLegacyImages(null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Invalid theme image in spec.favicon: unsupported image media type");
        assertThat(themeCrudService.storage()).containsExactly(existing);
    }

    @Test
    void creates_theme_with_deterministic_id_when_absent() {
        var definition = new ThemeDefinition();
        var output = useCase.execute(specWith(THEME_HRID, "My Theme", definition));

        var expectedId = HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(THEME_HRID).id();
        assertThat(output.theme().getId()).isEqualTo(expectedId);
        assertThat(output.theme().getName()).isEqualTo("My Theme");
        assertThat(output.theme().getType()).isEqualTo(ThemeType.PORTAL_NEXT);
        assertThat(output.theme().getReferenceType()).isEqualTo(Theme.ReferenceType.ENVIRONMENT);
        assertThat(output.theme().getReferenceId()).isEqualTo(AUDIT_INFO.environmentId());
        assertThat(output.theme().getDefinitionPortalNext()).isSameAs(definition);
        assertThat(output.theme().isEnabled()).isFalse();
        assertThat(output.errors()).isEmpty();
    }

    @Test
    void persists_assets_from_spec() {
        var input = new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            THEME_HRID,
            "My Theme",
            new ThemeDefinition(),
            "DATA:IMAGE/X-PNG;BASE64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jrFYAAAAASUVORK5CYII=",
            "https://example.test/optional-logo.png",
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jrFYAAAAASUVORK5CYII=",
            "https://example.test/background.png"
        );

        var output = useCase.execute(input);

        assertThat(output.theme().getLogo()).isEqualTo(input.logo());
        assertThat(output.theme().getOptionalLogo()).isEqualTo(input.optionalLogo());
        assertThat(output.theme().getFavicon()).isEqualTo(input.favicon());
        assertThat(output.theme().getBackgroundImage()).isEqualTo(input.backgroundImage());
    }

    @Test
    void updates_existing_theme_when_id_matches() {
        var existing = anEnvironmentTheme("Old", false, HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(THEME_HRID).id());
        themeCrudService.initWith(List.of(existing));

        var newDefinition = new ThemeDefinition();
        var output = useCase.execute(specWith(THEME_HRID, "New", newDefinition));

        assertThat(output.theme().getId()).isEqualTo(existing.getId());
        assertThat(output.theme().getName()).isEqualTo("New");
        assertThat(output.theme().getDefinitionPortalNext()).isSameAs(newDefinition);
        assertThat(output.theme().isEnabled()).isFalse();
        assertThat(themeCrudService.storage()).hasSize(1);
    }

    @Test
    void does_not_touch_other_themes_when_applying() {
        var otherEnabled = anEnvironmentTheme("Other", true, "11111111-1111-1111-1111-111111111111");
        themeCrudService.initWith(List.of(otherEnabled));

        useCase.execute(specWith(THEME_HRID, "New", new ThemeDefinition()));

        assertThat(themeCrudService.storage())
            .filteredOn(t -> t.getId().equals(otherEnabled.getId()))
            .singleElement()
            .extracting(Theme::isEnabled)
            .isEqualTo(true);
    }

    @Test
    void persists_automation_metadata_with_hrid() {
        useCase.execute(specWith(THEME_HRID, "My Theme", new ThemeDefinition()));

        assertThat(themeCrudService.storage())
            .singleElement()
            .satisfies(t -> assertThat(t.getAutomationMetadata().hrid()).isEqualTo(THEME_HRID));
    }

    @Test
    void trims_name_before_persisting() {
        useCase.execute(specWith(THEME_HRID, "  Padded  ", new ThemeDefinition()));

        assertThat(themeCrudService.storage()).singleElement().extracting(Theme::getName).isEqualTo("Padded");
    }

    @Test
    void throws_validation_error_when_name_is_blank() {
        assertThatThrownBy(() -> useCase.execute(specWith(THEME_HRID, "  ", new ThemeDefinition())))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("spec.name");
    }

    @Test
    void throws_validation_error_when_definition_is_null() {
        assertThatThrownBy(() -> useCase.execute(specWith(THEME_HRID, "Ok", null)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("spec.definitionPortalNext");
    }

    @Test
    void throws_validation_error_when_hrid_is_blank() {
        assertThatThrownBy(() -> useCase.execute(specWith("", "Ok", new ThemeDefinition())))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("spec.hrid");
    }

    private static ValidateThemeDomainService.Input specWith(String hrid, String name, ThemeDefinition definition) {
        return new ValidateThemeDomainService.Input(AUDIT_INFO, hrid, name, definition, null, null, null, null);
    }

    private static ValidateThemeDomainService.Input specWithLegacyImages(String changedField) {
        var changedSvg = "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=";
        return new ValidateThemeDomainService.Input(
            AUDIT_INFO,
            THEME_HRID,
            "Updated theme",
            ThemeDefinition.builder().customCss("body { color: blue; }").build(),
            "logo".equals(changedField) ? changedSvg : LEGACY_SVG,
            "optionalLogo".equals(changedField) ? changedSvg : LEGACY_SVG,
            "favicon".equals(changedField) ? changedSvg : LEGACY_SVG,
            "backgroundImage".equals(changedField) ? changedSvg : LEGACY_SVG
        );
    }

    private static Theme legacyTheme() {
        return anEnvironmentTheme("Old theme", false, HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(THEME_HRID).id())
            .toBuilder()
            .logo(LEGACY_SVG)
            .optionalLogo(LEGACY_SVG)
            .favicon(LEGACY_SVG)
            .backgroundImage(LEGACY_SVG)
            .build();
    }

    private static Theme anEnvironmentTheme(String name, boolean enabled, String id) {
        return Theme.builder()
            .id(id)
            .name(name)
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(AUDIT_INFO.environmentId())
            .definitionPortalNext(new ThemeDefinition())
            .enabled(enabled)
            .build();
    }
}
