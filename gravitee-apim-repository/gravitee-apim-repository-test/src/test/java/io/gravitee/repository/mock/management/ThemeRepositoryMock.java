/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.mock.management;

import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.model.Theme;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Date;
import java.util.Set;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ThemeRepositoryMock extends AbstractRepositoryMock<ThemeRepository> {

    public ThemeRepositoryMock() {
        super(ThemeRepository.class);
    }

    @Override
    protected void prepare(ThemeRepository themeRepository) throws Exception {
        final Theme theme = mockTheme(
            "dark",
            "ENVIRONMENT",
            "DEFAULT",
            "Theme dark",
            true,
            "{\"def\": \"value\"}",
            new Date(1000000000000L),
            new Date(1111111111111L),
            "logo",
            "backgroundImage",
            "optionalLogo",
            "favicon"
        );

        final Theme theme2 = mockTheme(
            "light",
            "ENVIRONMENT",
            "DEFAULT",
            "Light",
            true,
            "{\"def\": \"value\"}",
            new Date(1000000000000L),
            new Date(1111111111111L),
            "logo",
            "backgroundImage",
            "optionalLogo",
            "favicon"
        );

        final Theme theme2Updated = mockTheme(
            "light",
            "PLATFORM",
            "TEST",
            "Awesome",
            true,
            "{\"def\": \"test\"}",
            new Date(1010101010101L),
            new Date(1030141710801L),
            "updateLogo",
            "updateBackground",
            null,
            "updateFavicon"
        );

        final Theme theme3 = mockTheme(
            "simple",
            "ENVIRONMENT",
            "TEST",
            "Theme simple",
            false,
            "{\"def\": \"value\"}",
            new Date(1000002222222L),
            new Date(1111111111111L),
            "logo",
            "backgroundImage",
            "optionalLogo",
            "favicon"
        );

        final Set<Theme> themes = newSet(theme, theme2, theme3);
        final Set<Theme> themesAfterDelete = newSet(theme, theme2);
        final Set<Theme> themesAfterAdd = newSet(theme, theme2, theme3, mock(Theme.class));

        when(themeRepository.findAll()).thenReturn(themes, themesAfterAdd, themes, themesAfterDelete, themes);

        when(themeRepository.create(any(Theme.class))).thenReturn(theme);

        when(themeRepository.findById("new-theme")).thenReturn(of(theme));
        when(themeRepository.findById("light")).thenReturn(of(theme2), of(theme2Updated));
        when(themeRepository.findByReferenceIdAndReferenceType("DEFAULT", "ENVIRONMENT")).thenReturn(newSet(theme, theme2));
        when(themeRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());
    }

    private Theme mockTheme(
        final String id,
        final String referenceType,
        final String referenceId,
        final String name,
        final boolean isEnabled,
        final String definition,
        final Date createdAt,
        final Date updatedAt,
        final String logo,
        final String backgroundImage,
        final String optionalLogo,
        final String favicon
    ) {
        final Theme theme = mock(Theme.class);
        when(theme.getId()).thenReturn(id);
        when(theme.getReferenceId()).thenReturn(referenceId);
        when(theme.getReferenceType()).thenReturn(referenceType);
        when(theme.getName()).thenReturn(name);
        when(theme.isEnabled()).thenReturn(isEnabled);
        when(theme.getDefinition()).thenReturn(definition);
        when(theme.getCreatedAt()).thenReturn(createdAt);
        when(theme.getUpdatedAt()).thenReturn(updatedAt);
        when(theme.getLogo()).thenReturn(logo);
        when(theme.getBackgroundImage()).thenReturn(backgroundImage);
        when(theme.getOptionalLogo()).thenReturn(optionalLogo);
        when(theme.getFavicon()).thenReturn(favicon);
        return theme;
    }
}
