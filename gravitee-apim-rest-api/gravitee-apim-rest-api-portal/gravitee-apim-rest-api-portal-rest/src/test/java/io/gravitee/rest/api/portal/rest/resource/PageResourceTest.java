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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PageResourceTest extends AbstractResourceTest {

    private static final String PUBLISHED_PAGE = "my-page-published";
    private static final String UNPUBLISHED_PAGE = "my-page-unpublished";
    private static final String ANOTHER_PAGE = "another-page";
    private static final String UNKNOWN_PAGE = "unknown-page";
    private static final String PAGE_CONTENT = "my-page-content";

    @Override
    protected String contextPath() {
        return "pages/";
    }

    private PageEntity mockAnotherPage;

    @Before
    public void init() {
        resetAllMocks();

        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        publishedPage.setVisibility(Visibility.PUBLIC);
        publishedPage.setContent(PAGE_CONTENT);
        doReturn(publishedPage).when(pageService).findById(PUBLISHED_PAGE, null);

        PageEntity unPublishedPage = new PageEntity();
        unPublishedPage.setPublished(false);
        publishedPage.setVisibility(Visibility.PUBLIC);
        unPublishedPage.setContent(PAGE_CONTENT);
        doReturn(unPublishedPage).when(pageService).findById(UNPUBLISHED_PAGE, null);

        mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        publishedPage.setVisibility(Visibility.PUBLIC);
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE, null);

        doThrow(new PageNotFoundException(UNKNOWN_PAGE)).when(pageService).findById(UNKNOWN_PAGE, null);

        doReturn(new Page()).when(pageMapper).convert(any(), any(), any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
        doReturn(true).when(accessControlService).canAccessApiFromPortal(anyString());
        doReturn(true).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getCurrentEnvironment()), any());
    }

    @Test
    public void shouldNotGetPage() {
        final Response response = target(UNKNOWN_PAGE).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.page.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Page [" + UNKNOWN_PAGE + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetPage() {
        final Response response = target(PUBLISHED_PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertNull(pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldGetPageWithInclude() {
        final Response response = target(PUBLISHED_PAGE).queryParam("include", "content").request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertEquals(PAGE_CONTENT, pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldNotGetPageBecauseOfGroupService() {
        doReturn(false).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getCurrentEnvironment()), any());

        Response response = target(PUBLISHED_PAGE).request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotHaveMetadataCleared() {
        Response response = target(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);

        assertFalse(mockAnotherPage.getMetadata().isEmpty());
    }

    @Test
    public void shouldNotGetPageContent() {
        final Response response = target(UNKNOWN_PAGE).path("content").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.page.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Page [" + UNKNOWN_PAGE + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetPageContent() {
        final Response response = target(PUBLISHED_PAGE).path("content").request().get();
        assertEquals(OK_200, response.getStatus());

        final String pageContent = response.readEntity(String.class);
        assertEquals(PAGE_CONTENT, pageContent);
    }

    @Test
    public void shouldNotGetPageContentBecauseOfGroupService() {
        doReturn(false).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getCurrentEnvironment()), any());

        Response response = target(PUBLISHED_PAGE).path("content").request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }
}
