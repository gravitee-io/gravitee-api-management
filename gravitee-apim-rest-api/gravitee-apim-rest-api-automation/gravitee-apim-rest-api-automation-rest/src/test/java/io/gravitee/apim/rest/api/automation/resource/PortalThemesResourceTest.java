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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.use_case.CreateOrUpdatePortalThemeUseCase;
import io.gravitee.apim.core.theme.use_case.ValidatePortalThemeUseCase;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.apim.rest.api.automation.model.PortalThemeState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PortalThemesResourceTest extends AbstractResourceTest {

    private static final String THEME_HRID = "default-theme";
    private static final AuditInfo AUDIT_INFO = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();
    private static final String THEME_ID = HRIDToUUID.portalTheme().context(AUDIT_INFO).hrid(THEME_HRID).id();

    @Inject
    private CreateOrUpdatePortalThemeUseCase createOrUpdatePortalThemeUseCase;

    @Inject
    private ValidatePortalThemeUseCase validatePortalThemeUseCase;

    @AfterEach
    void tearDown() {
        reset(createOrUpdatePortalThemeUseCase, validatePortalThemeUseCase);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/portal-themes";
    }

    @Nested
    class DryRun {

        @Test
        void should_return_populated_state_when_validation_passes() {
            when(validatePortalThemeUseCase.execute(any())).thenReturn(new CreateOrUpdatePortalThemeUseCase.Output(aTheme(), List.of()));

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("theme.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalThemeUseCase);
                verify(validatePortalThemeUseCase).execute(any());

                var state = response.readEntity(PortalThemeState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getHrid()).isEqualTo(THEME_HRID);
                    soft.assertThat(state.getName()).isEqualTo("Default Theme");
                    soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                    soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                    soft.assertThat(state.getId()).isEqualTo(THEME_ID);
                    soft.assertThat(state.getErrors()).isNull();
                    soft.assertThat(state.getDefinition().getCustomCss()).isEqualTo(".portal { padding: 0; }");
                    soft.assertThat(state.getDefinition().getColor().getPrimary()).isEqualTo("#123456");
                });
            }
        }

        @Test
        void should_return_state_with_errors_when_validation_fails() {
            when(validatePortalThemeUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalThemeUseCase.Output(aTheme(), List.of(Validator.Error.severe("spec.name must not be blank")))
            );

            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("theme.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
                verifyNoInteractions(createOrUpdatePortalThemeUseCase);

                var state = response.readEntity(PortalThemeState.class);
                assertThat(state.getErrors()).isNotNull();
                assertThat(state.getErrors().getSevere()).hasSize(1);
                assertThat(state.getErrors().getSevere().get(0)).contains("spec.name");
            }
        }

        @Test
        void should_return_400_when_hrid_is_missing() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("theme-missing-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                verifyNoInteractions(validatePortalThemeUseCase);
                verifyNoInteractions(createOrUpdatePortalThemeUseCase);
            }
        }
    }

    @Nested
    class Run {

        @Test
        void should_create_or_update_theme() {
            when(createOrUpdatePortalThemeUseCase.execute(any())).thenReturn(
                new CreateOrUpdatePortalThemeUseCase.Output(aTheme(), List.of())
            );

            try (var response = rootTarget().request().accept(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(readJSON("theme.json")))) {
                assertThat(response.getStatus()).isEqualTo(200);
                verify(createOrUpdatePortalThemeUseCase).execute(any());

                var state = response.readEntity(PortalThemeState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getId()).isEqualTo(THEME_ID);
                    soft.assertThat(state.getHrid()).isEqualTo(THEME_HRID);
                });
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
            .definitionPortalNext(
                ThemeDefinition.builder()
                    .customCss(".portal { padding: 0; }")
                    .color(ThemeDefinition.Color.builder().primary("#123456").build())
                    .build()
            )
            .build();
    }
}
