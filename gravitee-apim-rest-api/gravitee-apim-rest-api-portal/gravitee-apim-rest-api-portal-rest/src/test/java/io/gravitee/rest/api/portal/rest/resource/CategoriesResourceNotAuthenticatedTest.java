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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.CategoriesResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
public class CategoriesResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "categories";
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
    public void init() {
        resetAllMocks();

        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        CategoryEntity category1 = new CategoryEntity();
        category1.setId("1");
        category1.setHidden(false);
        category1.setOrder(2);

        CategoryEntity category2 = new CategoryEntity();
        category2.setId("2");
        category2.setHidden(false);
        category2.setOrder(3);

        CategoryEntity category3 = new CategoryEntity();
        category3.setId("3");
        category3.setHidden(true);
        category3.setOrder(1);

        List<CategoryEntity> mockCategories = Arrays.asList(category1, category2, category3);
        doReturn(mockCategories).when(categoryService).findAll();
        doReturn(1L).when(categoryService).getTotalApisByCategory(any(), any());

        doReturn(false).when(ratingService).isEnabled();

        Mockito.when(categoryMapper.convert(any(), any())).thenCallRealMethod();
    }

    @Test
    public void shouldGetNotHiddenCategories() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(apiService).findPublishedByUser(any());
        CategoriesResponse categoriesResponse = response.readEntity(CategoriesResponse.class);
        assertEquals(2, categoriesResponse.getData().size());
    }
}
