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
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PageLinks;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;
import java.util.*;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPageResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PAGE = "my-page";
    private static final String UNKNOWN_PAGE = "unknown-page";
    private static final String ANOTHER_PAGE = "another-page";
    private static final String PAGE_CONTENT = "my-page-content";

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() {
        resetAllMocks();

        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(GraviteeContext.getExecutionContext(), API);

        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis)
            .when(apiService)
            .findPublishedByUser(eq(GraviteeContext.getExecutionContext()), any(), argThat(q -> singletonList(API).equals(q.getIds())));
        doReturn(true).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);
        PageEntity page1 = new PageEntity();
        page1.setPublished(true);
        page1.setVisibility(Visibility.PUBLIC);
        page1.setContent(PAGE_CONTENT);
        doReturn(page1).when(pageService).findById(PAGE, null);
        doReturn(true).when(accessControlService).canAccessPageFromPortal(GraviteeContext.getExecutionContext(), API, page1);

        doReturn(new Page()).when(pageMapper).convert(any(), any(), any());
        doReturn(new PageLinks()).when(pageMapper).computePageLinks(any(), any());
    }

    @Test
    public void shouldNotFoundApiWhileGettingApiPage() {
        // init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(false).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);

        // test
        final Response response = target(API).path("pages").path(PAGE).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldNotFoundPageWhileGettingApiPage() {
        doThrow(new PageNotFoundException(UNKNOWN_PAGE)).when(pageService).findById(UNKNOWN_PAGE, null);

        final Response response = target(API).path("pages").path(UNKNOWN_PAGE).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("errors.page.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Page [" + UNKNOWN_PAGE + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiPage() {
        final Response response = target(API).path("pages").path(PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertNull(pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldGetApiPageWithInclude() {
        final Response response = target(API).path("pages").path(PAGE).queryParam("include", "content").request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
        assertEquals(PAGE_CONTENT, pageResponse.getContent());
        assertNotNull(pageResponse.getLinks());
    }

    @Test
    public void shouldNotGetApiPage() {
        final Builder request = target(API).path("pages").path(PAGE).request();
        doReturn(false).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        Response response = request.get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }

    @Test
    public void shouldNotHaveMetadataCleared() {
        PageEntity mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setVisibility(Visibility.PUBLIC);
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE, null);

        Response response = target(API).path("pages").path(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);

        assertFalse(mockAnotherPage.getMetadata().isEmpty());
    }

    @Test
    public void shouldNotFoundApiWhileGettingApiPageContent() {
        // init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        doReturn(false).when(accessControlService).canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API);

        // test
        final Response response = target(API).path("pages").path(PAGE).path("content").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("errors.api.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Api [" + API + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldNotFoundPageWhileGettingApiPageContent() {
        doThrow(new PageNotFoundException(UNKNOWN_PAGE)).when(pageService).findById(UNKNOWN_PAGE, null);

        final Response response = target(API).path("pages").path(UNKNOWN_PAGE).path("content").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("errors.page.notFound", error.getCode());
        assertEquals("404", error.getStatus());
        assertEquals("Page [" + UNKNOWN_PAGE + "] can not be found.", error.getMessage());
    }

    @Test
    public void shouldGetApiPageContent() {
        final Response response = target(API).path("pages").path(PAGE).path("content").request().get();
        assertEquals(OK_200, response.getStatus());

        final String pageContent = response.readEntity(String.class);
        assertEquals(PAGE_CONTENT, pageContent);
    }

    @Test
    public void shouldNotGetApiPageContent() {
        final Builder request = target(API).path("pages").path(PAGE).path("content").request();
        doReturn(false).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), eq(API), any());

        Response response = request.get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }
}
