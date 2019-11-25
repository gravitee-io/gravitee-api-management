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

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

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

    protected String contextPath() {
        return "pages/";
    }

    private PageEntity mockAnotherPage;

    @Before
    public void init() throws IOException {
        resetAllMocks();

        PageEntity publishedPage = new PageEntity();
        publishedPage.setPublished(true);
        publishedPage.setExcludedGroups(new ArrayList<String>());
        publishedPage.setContent(PAGE_CONTENT);
        doReturn(publishedPage).when(pageService).findById(PUBLISHED_PAGE);

        PageEntity unPublishedPage = new PageEntity();
        unPublishedPage.setPublished(false);
        unPublishedPage.setExcludedGroups(new ArrayList<String>());
        unPublishedPage.setContent(PAGE_CONTENT);
        doReturn(unPublishedPage).when(pageService).findById(UNPUBLISHED_PAGE);

        mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setExcludedGroups(new ArrayList<String>());
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE);

        doThrow(new PageNotFoundException(UNKNOWN_PAGE)).when(pageService).findById(UNKNOWN_PAGE);

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
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        final Response response = target(PUBLISHED_PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertNull(pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldGetPageWithInclude() {
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        final Response response = target(PUBLISHED_PAGE).queryParam("include", "content").request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertEquals(PAGE_CONTENT, pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldNotGetPageBecauseOfGroupService() {
        doReturn(false).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target(PUBLISHED_PAGE).request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotGetUnpublishedPage() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target(UNPUBLISHED_PAGE).request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotHaveMetadataCleared() {
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

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
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        final Response response = target(PUBLISHED_PAGE).path("content").request().get();
        assertEquals(OK_200, response.getStatus());

        final String pageContent = response.readEntity(String.class);
        assertEquals(PAGE_CONTENT, pageContent);
    }

    @Test
    public void shouldNotGetPageContentBecauseOfGroupService() {
        doReturn(false).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target(PUBLISHED_PAGE).path("content").request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotGetUnpublishedPageContent() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessPortalData(any(), any());

        Response response = target(UNPUBLISHED_PAGE).path("content").request().get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }
}
