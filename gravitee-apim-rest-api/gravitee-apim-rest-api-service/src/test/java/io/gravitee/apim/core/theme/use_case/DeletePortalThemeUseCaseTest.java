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

import inmemory.PortalCrudServiceInMemory;
import inmemory.ThemeCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.portal.model.Portal;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.theme.exception.PortalThemeInUseException;
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
class DeletePortalThemeUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("organization-id")
        .environmentId("environment-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();
    private static final String THEME_ID = "theme-1";

    private final ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    private final PortalCrudServiceInMemory portalCrudService = new PortalCrudServiceInMemory();
    private DeletePortalThemeUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePortalThemeUseCase(themeCrudService, portalCrudService);
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
        portalCrudService.reset();
    }

    @Test
    void deletes_theme_when_not_referenced() {
        themeCrudService.initWith(List.of(anEnvironmentTheme(THEME_ID)));

        useCase.execute(new DeletePortalThemeUseCase.Input(AUDIT_INFO, THEME_ID));

        assertThat(themeCrudService.storage()).isEmpty();
    }

    @Test
    void rejects_delete_when_portal_active_theme_id_matches() {
        themeCrudService.initWith(List.of(anEnvironmentTheme(THEME_ID)));
        var portal = Portal.of(
            PortalId.of("11111111-1111-1111-1111-111111111111"),
            AUDIT_INFO.environmentId(),
            AUDIT_INFO.organizationId(),
            "Portal 1"
        ).withActiveThemeId(THEME_ID);
        portalCrudService.initWith(List.of(portal));

        assertThatThrownBy(() -> useCase.execute(new DeletePortalThemeUseCase.Input(AUDIT_INFO, THEME_ID)))
            .isInstanceOf(PortalThemeInUseException.class)
            .hasMessageContaining("still referenced");

        assertThat(themeCrudService.storage()).hasSize(1);
    }

    @Test
    void allows_delete_when_portals_reference_other_themes() {
        themeCrudService.initWith(List.of(anEnvironmentTheme(THEME_ID), anEnvironmentTheme("other")));
        var portal = Portal.of(
            PortalId.of("11111111-1111-1111-1111-111111111111"),
            AUDIT_INFO.environmentId(),
            AUDIT_INFO.organizationId(),
            "Portal 1"
        ).withActiveThemeId("other");
        portalCrudService.initWith(List.of(portal));

        useCase.execute(new DeletePortalThemeUseCase.Input(AUDIT_INFO, THEME_ID));

        assertThat(themeCrudService.storage()).extracting(Theme::getId).containsExactly("other");
    }

    @Test
    void throws_when_theme_missing() {
        assertThatThrownBy(() -> useCase.execute(new DeletePortalThemeUseCase.Input(AUDIT_INFO, "unknown"))).isInstanceOf(
            ThemeNotFoundException.class
        );
    }

    @Test
    void throws_when_theme_belongs_to_another_environment() {
        themeCrudService.initWith(List.of(anEnvironmentTheme(THEME_ID, "other-env")));

        assertThatThrownBy(() -> useCase.execute(new DeletePortalThemeUseCase.Input(AUDIT_INFO, THEME_ID))).isInstanceOf(
            ThemeNotFoundException.class
        );
    }

    private static Theme anEnvironmentTheme(String id) {
        return anEnvironmentTheme(id, AUDIT_INFO.environmentId());
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
