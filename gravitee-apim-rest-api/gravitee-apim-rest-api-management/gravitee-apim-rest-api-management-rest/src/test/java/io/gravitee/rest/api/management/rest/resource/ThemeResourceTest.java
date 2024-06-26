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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.*;
import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.theme.portal.ThemeDefinition;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.model.theme.portal.UpdateThemeEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ThemeNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ThemeResourceTest extends AbstractResourceTest {

    private static final String THEME_ID = "my-theme-id";

    @Override
    protected String contextPath() {
        return "configuration/themes/";
    }

    @Before
    public void init() {
        reset(themeService);
    }

    @Test
    public void shouldNotFindThemeById() {
        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenThrow(ThemeNotFoundException.class);

        final Response response = envTarget(THEME_ID).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldFindThemeById() {
        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().get();

        assertEquals(OK_200, response.getStatus());
        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(theme, responseTheme);
    }

    @Test
    public void shouldDeleteTheme() {
        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());
        verify(themeService).delete(GraviteeContext.getExecutionContext(), THEME_ID);
    }

    @Test
    public void shouldUpdateTheme() {
        ThemeEntity oldTheme = new ThemeEntity();
        oldTheme.setId(THEME_ID);

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(oldTheme);

        UpdateThemeEntity updateTheme = new UpdateThemeEntity();
        updateTheme.setId(THEME_ID);
        updateTheme.setName("my-theme");
        updateTheme.setDefinition(new ThemeDefinition());

        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateTheme)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().put(json(updateTheme));

        assertEquals(OK_200, response.getStatus());
        verify(themeService).updatePortalTheme(eq(GraviteeContext.getExecutionContext()), eq(updateTheme));

        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(theme, responseTheme);
    }

    @Test
    public void shouldNotFindPortalNextThemeById() {
        var theme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().get();

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotDeletePortalNextTheme() {
        var theme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(theme);
        final Response response = envTarget(THEME_ID).request().delete();

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotUpdatePortalNextTheme() {
        var oldTheme = io.gravitee.rest.api.model.theme.portalnext.ThemeEntity.builder().id(THEME_ID).build();

        when(themeService.findById(GraviteeContext.getExecutionContext(), THEME_ID)).thenReturn(oldTheme);
        UpdateThemeEntity updateTheme = new UpdateThemeEntity();
        updateTheme.setId(THEME_ID);
        updateTheme.setName("my-theme");
        updateTheme.setDefinition(new ThemeDefinition());

        ThemeEntity theme = new ThemeEntity();
        theme.setId(THEME_ID);

        when(themeService.updatePortalTheme(GraviteeContext.getExecutionContext(), updateTheme)).thenReturn(theme);

        final Response response = envTarget(THEME_ID).request().put(json(updateTheme));

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }
}
