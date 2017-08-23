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
package io.gravitee.management.rest.resource;

import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.Visibility;
import org.junit.Test;

import javax.ws.rs.core.Response;

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
        return "apis/"+API_NAME+"/pages/"+PAGE_NAME;
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
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, false);

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRole(any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, false);
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
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, false);

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRole(any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, false);
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
        doReturn(pageMock).when(pageService).findById(PAGE_NAME, false);

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());
        final PageEntity responsePage = response.readEntity(PageEntity.class);
        assertNotNull(responsePage);
        assertEquals(PAGE_NAME, responsePage.getName());
        verify(membershipService, never()).getRole(any(), any(), any());
        verify(apiService, times(1)).findById(API_NAME);
        verify(pageService, times(1)).findById(PAGE_NAME, false);
        verify(pageService, never()).isDisplayable(apiMock, pageMock.isPublished(), USER_NAME);
    }
}
