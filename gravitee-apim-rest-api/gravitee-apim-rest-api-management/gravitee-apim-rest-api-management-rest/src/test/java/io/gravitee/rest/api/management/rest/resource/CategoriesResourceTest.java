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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.UpdateCategoryEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Eric LELEU (eric dot leleu at graviteesource dot com)
 */
public class CategoriesResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "configuration/categories/";
    }

    @Inject
    private CategoryService categoryService;

    @Before
    public void init() {
        CategoryEntity cat1 = new CategoryEntity();
        cat1.setId("cat1-id");
        cat1.setName("cat1-name");
        cat1.setHidden(false);
        cat1.setOrder(2);

        CategoryEntity cat2 = new CategoryEntity();
        cat2.setId("cat2-id");
        cat2.setName("cat2-name");
        cat2.setHidden(false);
        cat2.setOrder(1);

        doReturn(List.of(cat1, cat2)).when(categoryService).findAll(GraviteeContext.getCurrentEnvironment());
    }

    @Test
    public void shouldListAllCategories_withoutApiCount() throws IOException {
        final Response response = envTarget().request().get();

        assertEquals(OK_200, response.getStatus());
        final List<CategoryEntity> categories = response.readEntity(new GenericType<>() {});
        assertNotNull(categories);
        assertEquals(2, categories.size());
        assertEquals("cat2-id", categories.get(0).getId());
        assertEquals(0, categories.get(0).getTotalApis());
        assertEquals("cat1-id", categories.get(1).getId());
        assertEquals(0, categories.get(1).getTotalApis());
    }

    @Test
    public void shouldListAllCategories_withApiCount() throws IOException {
        doReturn(2L).when(categoryService).getTotalApisByCategory(any(), any());

        final Response response = envTarget().queryParam("include", "total-apis").request().get();

        assertEquals(OK_200, response.getStatus());
        final List<CategoryEntity> categories = response.readEntity(new GenericType<>() {});
        assertNotNull(categories);
        assertEquals(2, categories.size());
        assertEquals("cat2-id", categories.get(0).getId());
        assertEquals(2, categories.get(0).getTotalApis());
        assertEquals("cat1-id", categories.get(1).getId());
        assertEquals(2, categories.get(1).getTotalApis());
    }
}
