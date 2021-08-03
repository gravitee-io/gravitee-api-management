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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Category;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class CategoryResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String CATEGORY_ID = "my-category-id";

    @Override
    protected String contextPath() {
        return "categories/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
                        return false;
                    }

                    @Override
                    public boolean isSecure() {
                        return false;
                    }

                    @Override
                    public String getAuthenticationScheme() {
                        return "BASIC";
                    }
                }
            );
        }
    }

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(CATEGORY_ID);
        categoryEntity.setHidden(false);
        doReturn(categoryEntity).when(categoryService).findNotHiddenById(CATEGORY_ID);

        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        Mockito.when(categoryMapper.convert(any(), any())).thenCallRealMethod();
    }

    @Test
    public void shouldGetCategory() {
        final Response response = target(CATEGORY_ID).request().get();
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(categoryService).findNotHiddenById(CATEGORY_ID);
        Mockito.verify(apiService).findPublishedByUser(null);
        Mockito.verify(categoryService).getTotalApisByCategory(any(), any());
        Mockito.verify(categoryMapper).convert(any(), any());

        final Category responseCategory = response.readEntity(Category.class);
        assertNotNull(responseCategory);
    }
}
