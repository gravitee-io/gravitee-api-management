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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.portal.rest.model.PagesResponse;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPagesResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String FORBIDDEN_API = "my-forbidden-api";

    protected String contextPath() {
        return "apis/";
    }

    private ApiEntity mockApi;
    private ApiEntity forbiddenApi;

    @Before
    public void init() throws IOException {
        reset(apiService);
        reset(pageService);
        reset(pageMapper);
        
        mockApi = new ApiEntity();
        mockApi.setId(API);
        mockApi.setVisibility(Visibility.PUBLIC);
        doReturn(mockApi).when(apiService).findById(API);
        
        doReturn(Arrays.asList(new PageEntity())).when(pageService).search(any());
        
        forbiddenApi = new ApiEntity();
        forbiddenApi.setVisibility(Visibility.PRIVATE);
        doReturn(forbiddenApi).when(apiService).findById(FORBIDDEN_API);
        
        doReturn(new Page()).when(pageMapper).convert(any());

    }

    
    @Test
    public void shouldGetForbiddenAccess() {
        final Response response = target(FORBIDDEN_API).path("pages").request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        
        final Error error = response.readEntity(Error.class);
        
        assertNotNull(error);
        assertEquals("403", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ForbiddenAccessException", error.getTitle());
        assertEquals("You do not have sufficient rights to access this resource", error.getDetail());
    }

    @Test
    public void shouldGetApiPages() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        final Response response = target(API).path("pages").request().get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);

        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(1, pages.size());
    }
    
    @Test
    public void shouldGetNoApiPage() {
        final Builder request = target(API).path("pages").request();
        
        // case 1
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        Response response = request.get();
        assertEquals(OK_200, response.getStatus());

        PagesResponse pagesResponse = response.readEntity(PagesResponse.class);
        List<Page> pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
        
        // case 2
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(false).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        response = request.get();
        assertEquals(OK_200, response.getStatus());

        pagesResponse = response.readEntity(PagesResponse.class);
        pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
        
        // case 3
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(false).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        response = request.get();
        assertEquals(OK_200, response.getStatus());

        pagesResponse = response.readEntity(PagesResponse.class);
        pages = pagesResponse.getData();
        assertNotNull(pages);
        assertEquals(0, pages.size());
    }
}
