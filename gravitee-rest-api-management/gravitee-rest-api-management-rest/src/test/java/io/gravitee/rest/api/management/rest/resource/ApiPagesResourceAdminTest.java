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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
public class ApiPagesResourceAdminTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String PAGE_NAME = "p";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/pages/";
    }

    @Test
    public void shouldGetPublicApiPublishedPage() {
        reset(apiService, pageService, membershipService);
        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiService).findById(API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);

        final Response response = envTarget(PAGE_NAME).request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
        verify(pageService, never()).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }

    @Test
    public void shouldGetPrivateApiPublishedPage() {
        reset(apiService, pageService, membershipService);
        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiService).findById(API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(true);
        pageMock.setName(PAGE_NAME);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);

        final Response response = envTarget(PAGE_NAME).request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
        verify(pageService, never()).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }

    @Test
    public void shouldGetPrivateApiUnpublishedPage() {
        reset(apiService, pageService, membershipService);
        final ApiEntity apiMock = mock(ApiEntity.class);
        when(apiMock.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiMock.getName()).thenReturn(API_NAME);
        doReturn(apiMock).when(apiService).findById(API_NAME);
        final PageEntity pageMock = new PageEntity();
        pageMock.setPublished(false);
        pageMock.setName(PAGE_NAME);
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, null);

        final Response response = envTarget(PAGE_NAME).request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRoles(any(), any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, null);
        verify(pageService, never()).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }

    @Test
    public void shouldNotCreateSystemFolder() {
        NewPageEntity newPageEntity = new NewPageEntity();
        newPageEntity.setType(PageType.SYSTEM_FOLDER);
        final Response response = envTarget().request().post(Entity.json(newPageEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());

    }

    @Test
    public void shouldNotCreateMarkdownTemplate() {
        NewPageEntity newPageEntity = new NewPageEntity();
        newPageEntity.setType(PageType.MARKDOWN_TEMPLATE);
        final Response response = envTarget().request().post(Entity.json(newPageEntity));

        assertEquals(BAD_REQUEST_400, response.getStatus());

    }

    @Test
    public void shouldNotDeleteSystemFolder() {
        reset(apiService, pageService, membershipService);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("SYSTEM_FOLDER");
        doReturn(pageMock).when(pageService).findById(PAGE_NAME);

        final Response response = envTarget(PAGE_NAME).request().delete();

        assertEquals(BAD_REQUEST_400, response.getStatus());

    }

    @Test
    public void shouldNotUpdateSystemFolder() {
        reset(apiService, pageService, membershipService);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("SYSTEM_FOLDER");
        doReturn(pageMock).when(pageService).findById(PAGE_NAME);

        final Response response = envTarget(PAGE_NAME).request().put(Entity.json(new UpdatePageEntity()));

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotUpdatePatchSystemFolder() {
        reset(apiService, pageService, membershipService);

        final PageEntity pageMock = new PageEntity();
        pageMock.setType("SYSTEM_FOLDER");
        doReturn(pageMock).when(pageService).findById(PAGE_NAME);

        final Response response = envTarget(PAGE_NAME).request().method(javax.ws.rs.HttpMethod.PATCH, Entity.json(new UpdatePageEntity()));

        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateApiPage() {
        reset(pageService);
        NewPageEntity newPageEntity = new NewPageEntity();
        newPageEntity.setName("my-page-name");
        newPageEntity.setType(PageType.MARKDOWN);
        newPageEntity.setVisibility(Visibility.PUBLIC);

        PageEntity returnedPage = new PageEntity();
        returnedPage.setId("my-beautiful-page");
        doReturn(returnedPage).when(pageService).createPage(eq(API_NAME), any());
        doReturn(0).when(pageService).findMaxPortalPageOrder();

        final Response response = envTarget().request().post(Entity.json(newPageEntity));

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path("my-beautiful-page").getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }
}
