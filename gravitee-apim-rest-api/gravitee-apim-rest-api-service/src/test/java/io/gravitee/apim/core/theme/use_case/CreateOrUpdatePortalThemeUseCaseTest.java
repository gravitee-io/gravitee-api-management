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

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateOrUpdatePortalThemeUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String THEME_HRID = "my-theme";

    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private CreateOrUpdatePortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateOrUpdatePortalThemeUseCase(
            new ValidateThemeDomainService(themeCrudService),
            new ThemeDomainService(themeCrudService),
            themeCrudService
        );
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
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
            "data:image/png;base64,LOGO",
            "data:image/png;base64,OPT",
            "data:image/png;base64,FAV",
            "data:image/png;base64,BG"
        );

        var output = useCase.execute(input);

        assertThat(output.theme().getLogo()).isEqualTo("data:image/png;base64,LOGO");
        assertThat(output.theme().getOptionalLogo()).isEqualTo("data:image/png;base64,OPT");
        assertThat(output.theme().getFavicon()).isEqualTo("data:image/png;base64,FAV");
        assertThat(output.theme().getBackgroundImage()).isEqualTo("data:image/png;base64,BG");
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
