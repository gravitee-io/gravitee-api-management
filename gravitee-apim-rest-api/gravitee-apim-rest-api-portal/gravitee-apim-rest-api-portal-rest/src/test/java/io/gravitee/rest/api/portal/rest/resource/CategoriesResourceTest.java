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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.CategoriesResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.*;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoriesResourceTest extends AbstractResourceTest {

    private List<CategoryEntity> existingCategories;

    @Override
    protected String contextPath() {
        return "categories";
    }

    @Before
    public void init() {
        resetAllMocks();

        Set<Api> mockApis = new HashSet<>();
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

        doReturn(Map.of(category1.getId(), 1L, category2.getId(), 0L, category3.getId(), 2L))
            .when(apiService)
            .countPublishedByUserGroupedByCategories(any());

        existingCategories = Arrays.asList(category1, category2, category3);

        doReturn(existingCategories).when(categoryService).findAll(GraviteeContext.getCurrentEnvironment());

        Mockito.when(categoryMapper.convert(any(), any())).thenCallRealMethod();
    }

    @Test
    public void shouldGetNotHiddenCategories() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(apiService).countPublishedByUserGroupedByCategories(any());
        CategoriesResponse categoriesResponse = response.readEntity(CategoriesResponse.class);
        assertEquals(1, categoriesResponse.getData().size());
    }

    @Test
    public void shouldGetNoCategory() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        doReturn(new ArrayList<>()).when(categoryService).findAll(GraviteeContext.getCurrentEnvironment());

        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        CategoriesResponse categoriesResponse = response.readEntity(CategoriesResponse.class);
        assertEquals(0, categoriesResponse.getData().size());

        Links links = categoriesResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        categoriesResponse = anotherResponse.readEntity(CategoriesResponse.class);
        assertEquals(0, categoriesResponse.getData().size());

        links = categoriesResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldGetNothingIfAllCategoriesEmpty() {
        // 0 APIs returned for user in any categories
        doReturn(Map.of()).when(apiService).countPublishedByUserGroupedByCategories(any());

        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(apiService).countPublishedByUserGroupedByCategories(any());

        CategoriesResponse categoriesResponse = response.readEntity(CategoriesResponse.class);

        assertEquals(0, categoriesResponse.getData().size());
    }

    @Test
    public void shouldGetOnlyNonEmptyCategories() {
        final Response response = target().request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        Mockito.verify(apiService).countPublishedByUserGroupedByCategories(any());

        CategoriesResponse categoriesResponse = response.readEntity(CategoriesResponse.class);
        // only C1 is returned
        assertEquals(1, categoriesResponse.getData().size());
    }
}
