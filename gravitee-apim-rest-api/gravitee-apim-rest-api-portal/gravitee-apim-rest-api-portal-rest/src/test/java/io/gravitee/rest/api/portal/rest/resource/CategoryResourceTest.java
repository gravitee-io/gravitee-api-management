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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.eclipse.jetty.http.HttpHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CategoryResourceTest extends AbstractResourceTest {

    private static final String CATEGORY_ID = "my-category-id";
    private static final String UNKNOWN_CATEGORY = "unknown";
    private InlinePictureEntity mockImage;
    private byte[] apiLogoContent;

    @Override
    protected String contextPath() {
        return "categories/";
    }

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(CATEGORY_ID);
        categoryEntity.setHidden(false);
        when(categoryService.findNotHiddenById(CATEGORY_ID, GraviteeContext.getCurrentEnvironment())).thenReturn(categoryEntity);

        when(apiCategoryService.countApisPublishedGroupedByCategoriesForUser(USER_NAME))
            .thenReturn(cat -> CATEGORY_ID.equals(cat) ? 1L : 0L);

        when(categoryMapper.convert(any(), any())).thenCallRealMethod();

        mockImage = new InlinePictureEntity();
        apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        when(categoryService.getPicture(GraviteeContext.getCurrentEnvironment(), CATEGORY_ID)).thenReturn(mockImage);
    }

    @Test
    public void shouldGetCategory() {
        final Response response = target(CATEGORY_ID).request().get();
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(categoryService).findNotHiddenById(CATEGORY_ID, GraviteeContext.getCurrentEnvironment());
        Mockito.verify(apiCategoryService).countApisPublishedGroupedByCategoriesForUser(USER_NAME);
        Mockito.verify(categoryMapper).convert(any(), any());

        final Category responseCategory = response.readEntity(Category.class);
        assertNotNull(responseCategory);
        assertThat(responseCategory.getTotalApis()).isOne();
    }

    @Test
    public void shouldNotGetCategory() {
        when(categoryService.findNotHiddenById(UNKNOWN_CATEGORY, GraviteeContext.getCurrentEnvironment()))
            .thenThrow(new CategoryNotFoundException(UNKNOWN_CATEGORY));

        final Response response = target(UNKNOWN_CATEGORY).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.category.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Category [" + UNKNOWN_CATEGORY + "] cannot be found.", error.getMessage());
    }

    @Test
    public void shouldGetCategoryPicture() throws IOException {
        final Response response = target(CATEGORY_ID).path("picture").request().get();
        assertEquals(OK_200, response.getStatus());

        MultivaluedMap<String, Object> headers = response.getHeaders();
        String contentType = (String) headers.getFirst(HttpHeader.CONTENT_TYPE.asString());
        String etag = (String) headers.getFirst("ETag");

        assertEquals(mockImage.getType(), contentType);

        File result = response.readEntity(File.class);
        byte[] fileContent = Files.readAllBytes(Paths.get(result.getAbsolutePath()));
        assertTrue(Arrays.equals(fileContent, apiLogoContent));

        String expectedTag = '"' + Integer.toString(new String(fileContent).hashCode()) + '"';
        assertEquals(expectedTag, etag);

        // test Cache
        final Response cachedResponse = target(CATEGORY_ID)
            .path("picture")
            .request()
            .header(HttpHeader.IF_NONE_MATCH.asString(), etag)
            .get();
        assertEquals(NOT_MODIFIED_304, cachedResponse.getStatus());
    }
}
