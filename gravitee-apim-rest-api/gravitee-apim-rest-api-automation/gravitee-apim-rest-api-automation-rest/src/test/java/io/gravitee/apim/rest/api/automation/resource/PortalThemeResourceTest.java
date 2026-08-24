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
package io.gravitee.apim.rest.api.automation.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.use_case.DeletePortalThemeUseCase;
import io.gravitee.apim.core.theme.use_case.GetPortalThemeUseCase;
import io.gravitee.apim.rest.api.automation.model.PortalThemeState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalThemeResourceTest extends AbstractResourceTest {

    private static final String THEME_HRID = "default-theme";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final String THEME_ID = HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(THEME_HRID).id();

    @Inject
    private GetPortalThemeUseCase getPortalThemeUseCase;

    @Inject
    private DeletePortalThemeUseCase deletePortalThemeUseCase;

    @AfterEach
    void tearDown() {
        reset(getPortalThemeUseCase, deletePortalThemeUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portal-themes";
    }

    @Nested
    class Get {

        @Test
        void should_return_the_theme() {
            when(getPortalThemeUseCase.execute(any())).thenReturn(new GetPortalThemeUseCase.Output(aTheme()));

            try (var response = rootTarget(THEME_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(getPortalThemeUseCase).execute(any());

                var state = response.readEntity(PortalThemeState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(THEME_HRID);
                    soft.assertThat(state.getName()).isEqualTo("Default Theme");
                    soft.assertThat(state.getId()).isEqualTo(THEME_ID);
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                });
            }
        }

        @Test
        void should_return_404_when_theme_is_missing() {
            when(getPortalThemeUseCase.execute(any())).thenThrow(new ThemeNotFoundException(THEME_ID));

            try (var response = rootTarget(THEME_HRID).request().accept(MediaType.APPLICATION_JSON_TYPE).get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_the_theme() {
            try (var response = rootTarget(THEME_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
                verify(deletePortalThemeUseCase).execute(any());
            }
        }

        @Test
        void should_return_404_when_theme_is_missing() {
            doThrow(new ThemeNotFoundException(THEME_ID)).when(deletePortalThemeUseCase).execute(any());

            try (var response = rootTarget(THEME_HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }

    private static Theme aTheme() {
        return Theme.builder()
            .id(THEME_ID)
            .name("Default Theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceType(Theme.ReferenceType.ENVIRONMENT)
            .referenceId(ENVIRONMENT)
            .enabled(true)
            .build();
    }
}
