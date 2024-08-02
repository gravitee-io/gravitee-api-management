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
package io.gravitee.rest.api.portal.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import inmemory.ThemeQueryServiceInMemory;
import io.gravitee.apim.core.theme.model.Theme;
import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.portal.rest.model.ThemeResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class ThemeResourceTest extends AbstractResourceTest {

    private static final String THEME_ID = "my-theme-id";

    @Autowired
    private ThemeQueryServiceInMemory themeQueryService;

    @Override
    protected String contextPath() {
        return "theme";
    }

    @Before
    public void init() {
        when(themeMapper.convert(any(Theme.class), anyString())).thenCallRealMethod();
        reset(themeService);
    }

    @AfterEach
    public void destroy() {
        this.themeQueryService.reset();
    }

    @Test
    public void shouldGetDefaultPortalTheme() {
        final Response response = target().queryParam("type", "PORTAL").request().get();

        ThemeResponse resultTheme = response.readEntity(ThemeResponse.class);
        assertNotNull(resultTheme);
        assertEquals(ThemeResponse.TypeEnum.PORTAL, resultTheme.getType());
    }

    @Test
    public void shouldGetDefaultPortalNextTheme() {
        final Response response = target().queryParam("type", "PORTAL_NEXT").request().get();

        ThemeResponse resultTheme = response.readEntity(ThemeResponse.class);
        assertNotNull(resultTheme);
        assertEquals(ThemeResponse.TypeEnum.PORTAL_NEXT, resultTheme.getType());
    }

    @Test
    public void should_have_error_400_if_no_query() {
        final Response response = target().request().get();
        assertEquals(400, response.getStatus());
    }
}
