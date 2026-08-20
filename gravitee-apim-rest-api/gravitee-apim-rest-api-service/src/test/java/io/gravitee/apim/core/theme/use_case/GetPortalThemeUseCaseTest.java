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
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalThemeUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private GetPortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPortalThemeUseCase(themeCrudService);
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Test
    void returns_theme_when_it_belongs_to_environment() {
        var theme = anEnvironmentTheme("theme-1", AUDIT_INFO.environmentId());
        themeCrudService.initWith(List.of(theme));

        var output = useCase.execute(new GetPortalThemeUseCase.Input(AUDIT_INFO, "theme-1"));

        assertThat(output.theme()).isEqualTo(theme);
    }

    @Test
    void throws_when_theme_is_in_a_different_environment() {
        themeCrudService.initWith(List.of(anEnvironmentTheme("theme-1", "other-env")));

        assertThatThrownBy(() -> useCase.execute(new GetPortalThemeUseCase.Input(AUDIT_INFO, "theme-1"))).isInstanceOf(
            ThemeNotFoundException.class
        );
    }

    @Test
    void throws_when_theme_does_not_exist() {
        assertThatThrownBy(() -> useCase.execute(new GetPortalThemeUseCase.Input(AUDIT_INFO, "unknown"))).isInstanceOf(
            ThemeNotFoundException.class
        );
    }

    private static Theme anEnvironmentTheme(String id, String envId) {
        return Theme.builder()
            .id(id)
            .name("Theme " + id)
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(envId)
            .enabled(true)
            .build();
    }
}
