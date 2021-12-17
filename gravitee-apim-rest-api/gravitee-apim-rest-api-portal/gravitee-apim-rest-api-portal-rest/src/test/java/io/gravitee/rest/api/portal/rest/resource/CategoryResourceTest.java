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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.InlinePictureEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Category;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CategoryNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
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

    @Override
    protected String contextPath() {
        return "categories/";
    }

    private InlinePictureEntity mockImage;
    private byte[] apiLogoContent;

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(CATEGORY_ID);
        categoryEntity.setHidden(false);
        doReturn(categoryEntity).when(categoryService).findNotHiddenById(CATEGORY_ID, GraviteeContext.getCurrentEnvironment());

        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        Mockito.when(categoryMapper.convert(any(), any())).thenCallRealMethod();

        mockImage = new InlinePictureEntity();
        apiLogoContent = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("media/logo.svg").toURI()));
        mockImage.setContent(apiLogoContent);
        mockImage.setType("image/svg");
        doReturn(mockImage).when(categoryService).getPicture(GraviteeContext.getCurrentEnvironment(), CATEGORY_ID);
    }

    @Test
    public void shouldGetCategory() {
        final Response response = target(CATEGORY_ID).request().get();
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(categoryService).findNotHiddenById(CATEGORY_ID, GraviteeContext.getCurrentEnvironment());
        Mockito.verify(apiService).findPublishedByUser(USER_NAME);
        Mockito.verify(categoryService).getTotalApisByCategory(any(), any());
        Mockito.verify(categoryMapper).convert(any(), any());

        final Category responseCategory = response.readEntity(Category.class);
        assertNotNull(responseCategory);
    }

    @Test
    public void shouldNotGetCategory() {
        doThrow(new CategoryNotFoundException(UNKNOWN_CATEGORY))
            .when(categoryService)
            .findNotHiddenById(UNKNOWN_CATEGORY, GraviteeContext.getCurrentEnvironment());

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
        assertEquals("Category [" + UNKNOWN_CATEGORY + "] can not be found.", error.getMessage());
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
