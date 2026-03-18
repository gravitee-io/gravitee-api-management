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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.management.model.ThemeType;
import io.gravitee.rest.api.model.theme.portalnext.ThemeDefinition;
import java.util.Collections;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ThemeDefaultDarkColorsUpgraderTest {

    private static final String ENV_ID = "env-1";
    private static final String THEME_ID = "theme-1";

    private static final String DEFINITION_WITHOUT_DARK =
        "{\"color\":{\"primary\":\"#275CF6\",\"secondary\":\"#2051B1\",\"background\":{\"page\":\"#FFFFFF\",\"card\":\"#ffffff\"}},\"font\":{\"fontFamily\":\"Roboto\"}}";

    private static final String DEFINITION_WITH_DARK =
        "{\"color\":{\"primary\":\"#275CF6\"},\"dark\":{\"color\":{\"primary\":\"#8BABF8\"}}}";

    @Mock
    EnvironmentRepository environmentRepository;

    @Mock
    ThemeRepository themeRepository;

    private ThemeDefaultDarkColorsUpgrader upgrader;

    @BeforeEach
    void setUp() {
        upgrader = new ThemeDefaultDarkColorsUpgrader(environmentRepository, themeRepository);
    }

    @Test
    @SneakyThrows
    void should_do_nothing_when_there_is_no_environment() {
        when(environmentRepository.findAll()).thenReturn(Collections.emptySet());
        assertThat(upgrader.upgrade()).isTrue();
        verifyNoInteractions(themeRepository);
    }

    @Test
    @SneakyThrows
    void should_return_false_when_technical_exception_occurs() {
        when(environmentRepository.findAll()).thenThrow(new TechnicalException("test"));
        assertThat(upgrader.upgrade()).isFalse();
    }

    @Test
    @SneakyThrows
    void should_add_default_dark_colors_when_theme_has_no_dark() {
        var env = io.gravitee.repository.management.model.Environment.builder().id(ENV_ID).build();
        var themeWithoutDark = Theme.builder()
            .id(THEME_ID)
            .name("Portal Next Theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceId(ENV_ID)
            .referenceType("ENVIRONMENT")
            .definition(DEFINITION_WITHOUT_DARK)
            .build();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        when(themeRepository.findByReferenceIdAndReferenceTypeAndType(ENV_ID, "ENVIRONMENT", ThemeType.PORTAL_NEXT)).thenReturn(
            Set.of(themeWithoutDark)
        );
        when(themeRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(upgrader.upgrade()).isTrue();

        ArgumentCaptor<Theme> themeCaptor = ArgumentCaptor.forClass(Theme.class);
        verify(themeRepository).update(themeCaptor.capture());
        Theme updated = themeCaptor.getValue();
        ThemeDefinition def = io.gravitee.apim.infra.adapter.GraviteeJacksonMapper.getInstance().readValue(
            updated.getDefinition(),
            ThemeDefinition.class
        );
        assertThat(def.getDark()).isNotNull();
        assertThat(def.getDark().getColor()).isNotNull();
        assertThat(def.getDark().getColor().getPrimary()).isEqualTo("#8BABF8");
        assertThat(def.getDark().getColor().getBackground().getPage()).isEqualTo("#1C1B1F");
    }

    @Test
    @SneakyThrows
    void should_skip_theme_when_already_has_dark_colors() {
        var env = io.gravitee.repository.management.model.Environment.builder().id(ENV_ID).build();
        var themeWithDark = Theme.builder()
            .id(THEME_ID)
            .name("Portal Next Theme")
            .type(ThemeType.PORTAL_NEXT)
            .referenceId(ENV_ID)
            .referenceType("ENVIRONMENT")
            .definition(DEFINITION_WITH_DARK)
            .build();
        when(environmentRepository.findAll()).thenReturn(Set.of(env));
        when(themeRepository.findByReferenceIdAndReferenceTypeAndType(ENV_ID, "ENVIRONMENT", ThemeType.PORTAL_NEXT)).thenReturn(
            Set.of(themeWithDark)
        );

        assertThat(upgrader.upgrade()).isTrue();

        verify(themeRepository).findByReferenceIdAndReferenceTypeAndType(ENV_ID, "ENVIRONMENT", ThemeType.PORTAL_NEXT);
        verify(themeRepository, never()).update(any());
    }
}
