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
package io.gravitee.apim.infra.crud_service.theme;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.PlanFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.theme.exception.ThemeNotFoundException;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.apim.core.theme.model.ThemeType;
import io.gravitee.apim.infra.adapter.GraviteeJacksonMapper;
import io.gravitee.apim.infra.crud_service.plan.PlanCrudServiceImpl;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.v4.plan.PlanType;
import io.gravitee.rest.api.model.v4.plan.PlanValidationType;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.SneakyThrows;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class ThemeCrudServiceImplTest {

    ThemeRepository themeRepository;

    ThemeCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        themeRepository = mock(ThemeRepository.class);

        service = new ThemeCrudServiceImpl(themeRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_portal_theme() {
            var definitionPortal = new ThemeDefinition();
            definitionPortal.setData(List.of());
            var theme = Theme
                .builder()
                .id("portal-id")
                .type(ThemeType.PORTAL)
                .definitionPortal(definitionPortal)
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build();
            when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(theme);

            assertThat(result).isEqualTo(theme);
        }

        @Test
        @SneakyThrows
        void should_create_a_portal_next_theme() {
            var definitionPortal = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                .builder()
                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                .build();
            var theme = Theme
                .builder()
                .id("portal-id")
                .type(ThemeType.PORTAL_NEXT)
                .definitionPortalNext(definitionPortal)
                .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .updatedAt(Instant.parse("2020-02-02T20:22:02.00Z").atZone(ZoneId.systemDefault()))
                .build();
            when(themeRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var result = service.create(theme);

            assertThat(result).isEqualTo(theme);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(themeRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(Theme.builder().build()));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessage("Error during theme creation");
        }
    }

    @Nested
    class Get {

        @Test
        @SneakyThrows
        void should_get_a_portal_theme() {
            when(themeRepository.findById("portal-id"))
                .thenReturn(
                    Optional.of(
                        io.gravitee.repository.management.model.Theme
                            .builder()
                            .id("portal-id")
                            .type(io.gravitee.repository.management.model.ThemeType.PORTAL)
                            .definition("{ \"data\": [] }")
                            .build()
                    )
                );

            var definitionPortal = new ThemeDefinition();
            definitionPortal.setData(List.of());

            var theme = Theme.builder().id("portal-id").type(ThemeType.PORTAL).definitionPortal(definitionPortal).build();

            var existingTheme = service.get("portal-id");

            assertThat(existingTheme).isEqualTo(theme);
        }

        @Test
        @SneakyThrows
        void should_get_a_portal_next_theme() {
            when(themeRepository.findById("portal-id"))
                .thenReturn(
                    Optional.of(
                        io.gravitee.repository.management.model.Theme
                            .builder()
                            .id("portal-id")
                            .type(io.gravitee.repository.management.model.ThemeType.PORTAL_NEXT)
                            .definition("{ \"color\": { \"primary\": \"#fff\" } }")
                            .build()
                    )
                );

            var definitionPortal = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                .builder()
                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                .build();

            var theme = Theme.builder().id("portal-id").type(ThemeType.PORTAL_NEXT).definitionPortalNext(definitionPortal).build();

            var existingTheme = service.get("portal-id");

            assertThat(existingTheme).isEqualTo(theme);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(themeRepository.findById(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.get("portal-id"));

            // Then
            assertThat(throwable).isInstanceOf(TechnicalDomainException.class).hasMessage("Error during get");
        }

        @Test
        void should_throw_when_theme_not_found() throws TechnicalException {
            // Given
            when(themeRepository.findById(any())).thenReturn(Optional.empty());

            // When
            Throwable throwable = catchThrowable(() -> service.get("portal-id"));

            // Then
            assertThat(throwable).isInstanceOf(ThemeNotFoundException.class).hasMessage("Theme [ portal-id ] not found");
        }
    }
}
