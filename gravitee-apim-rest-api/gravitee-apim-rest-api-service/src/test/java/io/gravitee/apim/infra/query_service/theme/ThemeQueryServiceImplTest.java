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
package io.gravitee.apim.infra.query_service.theme;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.theme.model.ThemeSearchCriteria;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.api.search.ThemeCriteria;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ThemeQueryServiceImplTest {

    private final String PORTAL_THEME_ID = "portal-id";
    private final String PORTAL_NEXT_THEME_ID = "portal-next-id";
    private final String ENV_ID = "env-id";

    @Mock
    ThemeRepository themeRepository;

    ThemeQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ThemeQueryServiceImpl(themeRepository);
    }

    @Nested
    class Search {

        @Test
        void should_return_empty_page() throws Throwable {
            var criteria = ThemeCriteria.builder().build();
            when(themeRepository.search(eq(criteria), any())).thenAnswer(invocation -> new Page<Theme>(List.of(), 0, 0, 0));
            var result = service.searchByCriteria(ThemeSearchCriteria.builder().build(), new PageableImpl(1, 1));

            Assertions.assertThat(result).isNotNull().hasFieldOrPropertyWithValue("content", List.of());
        }

        @Test
        void should_return_page_of_mixed_themes() throws Throwable {
            var criteria = ThemeCriteria.builder().build();
            when(themeRepository.search(eq(criteria), any()))
                .thenAnswer(invocation -> new Page<>(List.of(aPortalRepoTheme(), aPortalNextRepoTheme()), 1, 2, 2));
            var result = service.searchByCriteria(ThemeSearchCriteria.builder().build(), new PageableImpl(1, 1));

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            var portalNextDefinition = io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition
                .builder()
                .color(io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition.Color.builder().primary("#fff").build())
                .build();
            var portalNextTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_NEXT_THEME_ID)
                .name(PORTAL_NEXT_THEME_ID)
                .definitionPortalNext(portalNextDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL_NEXT)
                .build();

            Assertions
                .assertThat(result)
                .isNotNull()
                .hasFieldOrPropertyWithValue("totalElements", 2L)
                .hasFieldOrPropertyWithValue("pageNumber", 1)
                .hasFieldOrPropertyWithValue("pageElements", 2L);
            Assertions.assertThat(result.getContent()).hasSize(2).contains(portalTheme, portalNextTheme);
        }

        @Test
        void should_allow_null_type() throws Throwable {
            var criteria = ThemeCriteria.builder().type(null).build();
            when(themeRepository.search(eq(criteria), any())).thenAnswer(invocation -> new Page<>(List.of(aPortalRepoTheme()), 0, 0, 0));
            var result = service.searchByCriteria(ThemeSearchCriteria.builder().type(null).build(), new PageableImpl(1, 1));

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            Assertions.assertThat(result).isNotNull().hasFieldOrPropertyWithValue("content", List.of(portalTheme));
        }

        @Test
        void should_allow_specified_type() throws Throwable {
            var criteria = ThemeCriteria.builder().type(ThemeType.PORTAL).build();
            when(themeRepository.search(eq(criteria), any())).thenAnswer(invocation -> new Page<>(List.of(aPortalRepoTheme()), 0, 0, 0));
            var result = service.searchByCriteria(
                ThemeSearchCriteria.builder().type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL).build(),
                new PageableImpl(1, 1)
            );

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            Assertions.assertThat(result).isNotNull().hasFieldOrPropertyWithValue("content", List.of(portalTheme));
        }

        @Test
        void should_allow_null_enabled() throws Throwable {
            var criteria = ThemeCriteria.builder().enabled(null).build();
            when(themeRepository.search(eq(criteria), any())).thenAnswer(invocation -> new Page<>(List.of(aPortalRepoTheme()), 0, 0, 0));
            var result = service.searchByCriteria(ThemeSearchCriteria.builder().enabled(null).build(), new PageableImpl(1, 1));

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            Assertions.assertThat(result).isNotNull().hasFieldOrPropertyWithValue("content", List.of(portalTheme));
        }

        @Test
        void should_allow_specified_enabled() throws Throwable {
            var criteria = ThemeCriteria.builder().enabled(true).build();
            when(themeRepository.search(eq(criteria), any())).thenAnswer(invocation -> new Page<>(List.of(aPortalRepoTheme()), 0, 0, 0));
            var result = service.searchByCriteria(ThemeSearchCriteria.builder().enabled(true).build(), new PageableImpl(1, 1));

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            Assertions.assertThat(result).isNotNull().hasFieldOrPropertyWithValue("content", List.of(portalTheme));
        }
    }

    @Nested
    class FindByThemeTypeAndEnvironmentId {

        @Test
        void should_return_empty_results() throws Throwable {
            when(themeRepository.findByReferenceIdAndReferenceTypeAndType(eq(ENV_ID), eq("ENVIRONMENT"), eq(ThemeType.PORTAL)))
                .thenAnswer(invocation -> new HashSet<Theme>());

            var result = service.findByThemeTypeAndEnvironmentId(io.gravitee.apim.core.theme.model.ThemeType.PORTAL, ENV_ID);

            Assertions.assertThat(result).isNotNull().isEmpty();
        }

        @Test
        void should_return_portal_theme() throws Throwable {
            when(themeRepository.findByReferenceIdAndReferenceTypeAndType(eq(ENV_ID), eq("ENVIRONMENT"), eq(ThemeType.PORTAL)))
                .thenAnswer(invocation -> Set.of(aPortalRepoTheme()));

            var result = service.findByThemeTypeAndEnvironmentId(io.gravitee.apim.core.theme.model.ThemeType.PORTAL, ENV_ID);

            var portalDefinition = new ThemeDefinition();
            portalDefinition.setData(List.of());
            var portalTheme = io.gravitee.apim.core.theme.model.Theme
                .builder()
                .id(PORTAL_THEME_ID)
                .name(PORTAL_THEME_ID)
                .definitionPortal(portalDefinition)
                .type(io.gravitee.apim.core.theme.model.ThemeType.PORTAL)
                .build();

            Assertions.assertThat(result).isNotNull().hasSize(1).contains(portalTheme);
        }
    }

    private Theme aPortalRepoTheme() {
        var theme = new Theme();
        theme.setId(PORTAL_THEME_ID);
        theme.setName(PORTAL_THEME_ID);
        theme.setType(ThemeType.PORTAL);
        theme.setDefinition("{ \"data\": [] }");
        return theme;
    }

    private Theme aPortalNextRepoTheme() {
        var theme = new Theme();
        theme.setId(PORTAL_NEXT_THEME_ID);
        theme.setName(PORTAL_NEXT_THEME_ID);
        theme.setType(ThemeType.PORTAL_NEXT);
        theme.setDefinition("{ \"color\": { \"primary\": \"#fff\" } }");
        return theme;
    }
}
