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
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.theme.domain_service.ValidateThemeDomainService;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ValidatePortalThemeUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private ValidatePortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ValidatePortalThemeUseCase(new ValidateThemeDomainService(new ThemeCrudServiceInMemory()));
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
}
