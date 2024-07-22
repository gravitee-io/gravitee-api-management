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

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ThemeCrudServiceInMemory;
import io.gravitee.apim.core.theme.exception.ThemeDefinitionInvalidException;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.exception.ThemeTypeInvalidException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.core.theme.model.UpdateTheme;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ValidateThemeDomainServiceTest {

    ThemeCrudServiceInMemory themeCrudService = new ThemeCrudServiceInMemory();
    ValidateThemeDomainService cut;

    private final String PORTAL_NEXT_THEME_ID = "portal-next";
    private final String PORTAL_THEME_ID = "portal";
    private final String ENV_ID = "env-id";
    private final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", ENV_ID);

    @BeforeEach
    void setUp() {
        themeCrudService.initWith(
            List.of(
                Theme
                    .builder()
                    .id(PORTAL_NEXT_THEME_ID)
                    .type(ThemeType.PORTAL_NEXT)
                    .referenceType(Theme.ReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .definitionPortalNext(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.builder().build())
                    .build(),
                Theme
                    .builder()
                    .id(PORTAL_THEME_ID)
                    .type(ThemeType.PORTAL)
                    .referenceType(Theme.ReferenceType.ENVIRONMENT)
                    .referenceId(ENV_ID)
                    .definitionPortal(new ThemeDefinition())
                    .build()
            )
        );

        cut = new ValidateThemeDomainService(themeCrudService);
    }

    @AfterEach
    void tearDown() {
        themeCrudService.reset();
    }

    @Nested
    class ValidateUpdateTheme {

        @Test
        void should_throw_error_if_theme_does_not_exist() {
            assertThatThrownBy(() -> cut.validateUpdateTheme(UpdateTheme.builder().id("does-not-exist").build(), EXECUTION_CONTEXT))
                .isInstanceOf(ThemeNotFoundException.class);
        }

        @Test
        void should_throw_error_if_theme_is_not_in_scope() {
            assertThatThrownBy(() ->
                    cut.validateUpdateTheme(
                        UpdateTheme.builder().id(PORTAL_THEME_ID).build(),
                        new ExecutionContext("org-id", "out-of-scope")
                    )
                )
                .isInstanceOf(ThemeNotFoundException.class);
        }

        @Test
        void should_throw_error_if_theme_type_different() {
            assertThatThrownBy(() ->
                    cut.validateUpdateTheme(
                        UpdateTheme.builder().id(PORTAL_THEME_ID).type(ThemeType.PORTAL_NEXT).build(),
                        EXECUTION_CONTEXT
                    )
                )
                .isInstanceOf(ThemeTypeInvalidException.class);
        }

        @Test
        void should_throw_error_if_portal_theme_definition_missing() {
            assertThatThrownBy(() ->
                    cut.validateUpdateTheme(UpdateTheme.builder().id(PORTAL_THEME_ID).type(ThemeType.PORTAL).build(), EXECUTION_CONTEXT)
                )
                .isInstanceOf(ThemeDefinitionInvalidException.class);
        }

        @Test
        void should_throw_error_if_portal_next_theme_definition_missing() {
            assertThatThrownBy(() ->
                    cut.validateUpdateTheme(
                        UpdateTheme.builder().id(PORTAL_NEXT_THEME_ID).type(ThemeType.PORTAL_NEXT).build(),
                        EXECUTION_CONTEXT
                    )
                )
                .isInstanceOf(ThemeDefinitionInvalidException.class);
        }

        @Test
        void should_validate_portal_theme() {
            assertThatNoException()
                .isThrownBy(() ->
                    cut.validateUpdateTheme(
                        UpdateTheme.builder().id(PORTAL_THEME_ID).type(ThemeType.PORTAL).definitionPortal(new ThemeDefinition()).build(),
                        EXECUTION_CONTEXT
                    )
                );
        }

        @Test
        void should_validate_portal_next_theme() {
            assertThatNoException()
                .isThrownBy(() ->
                    cut.validateUpdateTheme(
                        UpdateTheme
                            .builder()
                            .id(PORTAL_NEXT_THEME_ID)
                            .type(ThemeType.PORTAL_NEXT)
                            .definitionPortalNext(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.builder().build())
                            .build(),
                        EXECUTION_CONTEXT
                    )
                );
        }
    }
}
