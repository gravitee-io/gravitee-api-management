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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.theme.NewThemeEntity;
import io.gravitee.rest.api.model.theme.ThemeEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ThemesResourceTest extends AbstractResourceTest {

    private static final String THEME_ID = "my-theme-id";

    @Override
    protected String contextPath() {
        return "configuration/themes";
    }

    @Before
    public void init() {
        reset(themeService);
    }

    @Test
    public void shouldCreateTheme() {
        NewThemeEntity newTheme = new NewThemeEntity();
        newTheme.setName("my-theme");

        ThemeEntity createdTheme = new ThemeEntity();
        createdTheme.setId(THEME_ID);
        createdTheme.setName("my-theme");

        when(themeService.create(eq(GraviteeContext.getExecutionContext()), eq(newTheme))).thenReturn(createdTheme);

        final Response response = envTarget().request().post(json(newTheme));

        assertEquals(OK_200, response.getStatus());
        verify(themeService).create(eq(GraviteeContext.getExecutionContext()), eq(newTheme));

        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(createdTheme, responseTheme);
    }

    @Test
    public void shouldGetCurrentTheme() {
        ThemeEntity activeTheme = new ThemeEntity();
        activeTheme.setId(THEME_ID);
        activeTheme.setName("my-theme");

        when(themeService.findOrCreateDefault(eq(GraviteeContext.getExecutionContext()))).thenReturn(activeTheme);

        final Response response = envTarget("/current").request().get();

        assertEquals(OK_200, response.getStatus());

        final ThemeEntity responseTheme = response.readEntity(ThemeEntity.class);
        assertNotNull(responseTheme);
        assertEquals(activeTheme, responseTheme);
    }
}
