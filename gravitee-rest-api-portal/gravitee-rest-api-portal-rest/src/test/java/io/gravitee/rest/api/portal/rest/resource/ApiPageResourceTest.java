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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.common.http.HttpStatusCode.UNAUTHORIZED_401;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.service.exceptions.PageNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiPageResourceTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PAGE = "my-page";
    private static final String UNKNOWN_PAGE = "unknown-page";
    private static final String ANOTHER_PAGE = "another-page";


    protected String contextPath() {
        return "apis/";
    }

    @Before
    public void init() throws IOException {
        resetAllMocks();
        
        ApiEntity mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(API);

        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());

        PageEntity page1 = new PageEntity();
        page1.setPublished(true);
        page1.setExcludedGroups(new ArrayList<String>());
        doReturn(page1).when(pageService).findById(PAGE);
    }
    
    @Test
    public void shouldNotFoundApiWhileGettingApiPage() {
        //init
        ApiEntity userApi = new ApiEntity();
        userApi.setId("1");
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(userApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(any());
        
        //test
        final Response response = target(API).path("pages").path(PAGE).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertNotNull(error);
        assertEquals("404", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.ApiNotFoundException", error.getTitle());
        assertEquals("Api ["+API+"] can not be found.", error.getDetail());
    }
    
    @Test
    public void shouldNotFoundPageWhileGettingApiPage() {
        doThrow(new PageNotFoundException(UNKNOWN_PAGE)).when(pageService).findById(UNKNOWN_PAGE);

        final Response response = target(API).path("pages").path(UNKNOWN_PAGE).request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("404", error.getCode());
        assertEquals("io.gravitee.rest.api.service.exceptions.PageNotFoundException", error.getTitle());
        assertEquals("Page [" + UNKNOWN_PAGE + "] can not be found.", error.getDetail());
    }
    
    @Test
    public void shouldGetApiPage() {
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        doReturn(new Page()).when(pageMapper).convert(any());

        final Response response = target(API).path("pages").path(PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        final Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);
    }  
    
    @Test
    public void shouldNotGetApiPage() {
        final Builder request = target(API).path("pages").path(PAGE).request();
        // case 1
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        Response response = request.get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());

        // case 2
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(false).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        response = request.get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());

        
        // case 3
        doReturn(false).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(false).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        
        
        response = request.get();
        assertEquals(UNAUTHORIZED_401, response.getStatus());
    }
    
    @Test
    public void shouldNotHaveMetadataCleared() {
        PageEntity mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setExcludedGroups(new ArrayList<String>());
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE);
        
        doReturn(new Page()).when(pageMapper).convert(any());
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        doReturn(true).when(pageService).isDisplayable(any(), any(Boolean.class).booleanValue(), any());        


        Response response = target(API).path("pages").path(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, response.getStatus());

        Page pageResponse = response.readEntity(Page.class);
        assertNotNull(pageResponse);

        assertFalse(mockAnotherPage.getMetadata().isEmpty());
    }
}
