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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.theme.portal.ThemeEntity;
import io.gravitee.rest.api.portal.rest.model.ThemeResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
        return "theme";
    }

    @Before
    public void init() {
        reset(themeService);
    }

    @Test
    public void shouldGetPortalTheme() {
        ThemeEntity themeEntity = new ThemeEntity();
        themeEntity.setId(THEME_ID);

        when(themeService.findEnabledPortalTheme(GraviteeContext.getExecutionContext())).thenReturn(themeEntity);
        when(themeMapper.convert(any(), any())).thenCallRealMethod();

        final Response response = target().request().get();

        ThemeResponse resultTheme = response.readEntity(ThemeResponse.class);
        assertNotNull(resultTheme);
    }
}
