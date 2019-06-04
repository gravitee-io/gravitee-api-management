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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Links;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
    }
    
    @Before
    public void init() {
        ApiEntity publishedApi1 = new ApiEntity();
        publishedApi1.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi1.setName("A");
        publishedApi1.setId("A");
        
        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("B");
        unpublishedApi.setId("B");
        
        ApiEntity publishedApi2 = new ApiEntity();
        publishedApi2.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi2.setName("C");
        publishedApi2.setId("C");
        
        
        ApiEntity publishedApi3 = new ApiEntity();
        publishedApi3.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi3.setName("D");
        publishedApi3.setId("D");
        
        ApiEntity publishedApi4 = new ApiEntity();
        publishedApi4.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi4.setName("E");
        publishedApi4.setId("E");
        
        ApiEntity publishedApi5 = new ApiEntity();
        publishedApi5.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi5.setName("F");
        publishedApi5.setId("F");
        
        
        reset(apiService);
        reset(ratingService);
        reset(apiMapper);
        
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(publishedApi1, publishedApi2, publishedApi3, publishedApi4, publishedApi5));
        doReturn(mockApis).when(apiService).findByUser(any(), any());
        doReturn(mockApis).when(apiService).search(any());
        
        doReturn(false).when(ratingService).isEnabled();

        doReturn(new Api().id("A").name("A")).when(apiMapper).convert(publishedApi1);
        doReturn(new Api().id("B").name("B")).when(apiMapper).convert(unpublishedApi);
        doReturn(new Api().id("C").name("C")).when(apiMapper).convert(publishedApi2);
        doReturn(new Api().id("D").name("D")).when(apiMapper).convert(publishedApi3);
        doReturn(new Api().id("E").name("E")).when(apiMapper).convert(publishedApi4);
        doReturn(new Api().id("F").name("F")).when(apiMapper).convert(publishedApi5);
        
    }
    
    @Test
    public void shouldGetPublishedApi() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(apiService).findByUser(any(), any());
        
        ArgumentCaptor<String> ac = ArgumentCaptor.forClass(String.class);
        Mockito.verify(apiMapper, Mockito.times(5)).computeApiLinks(ac.capture());
        
        String expectedBasePath = target().getUri().toString();
        List<String> bastPathList = ac.getAllValues();
        assertTrue(bastPathList.contains(expectedBasePath+"/A"));
        assertTrue(bastPathList.contains(expectedBasePath+"/C"));
        assertTrue(bastPathList.contains(expectedBasePath+"/D"));
        assertTrue(bastPathList.contains(expectedBasePath+"/E"));
        assertTrue(bastPathList.contains(expectedBasePath+"/F"));
        
        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(5, apiResponse.getData().size());
        assertEquals("A", apiResponse.getData().get(0).getName());
        assertEquals("C", apiResponse.getData().get(1).getName());
        assertEquals("D", apiResponse.getData().get(2).getName());
        assertEquals("E", apiResponse.getData().get(3).getName());
        assertEquals("F", apiResponse.getData().get(4).getName());
        
        
    }
    
    @Test
    public void shouldGetPublishedApiWithPaginatedLink() {
        final Response response = target().queryParam("page", 3).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(1, apiResponse.getData().size());
        assertEquals("D", apiResponse.getData().get(0).getName());
    
        Links links = apiResponse.getLinks();
        assertNotNull(links);
        
    }
    
    @Test
    public void shouldGetNoApi() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        Error errorResponse = response.readEntity(Error.class);
        assertEquals("400", errorResponse.getCode());
        assertEquals("javax.ws.rs.BadRequestException", errorResponse.getTitle());
        assertEquals("page is not valid", errorResponse.getDetail());
    }
    
    @Test
    public void shouldGetNoPublishedApiAndNoLink() {

        doReturn(new HashSet<>()).when(apiService).findByUser(any(), any());
        
        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(0, apiResponse.getData().size());
        
        Links links = apiResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        apiResponse = anotherResponse.readEntity(ApisResponse.class);
        assertEquals(0, apiResponse.getData().size());
        
        links = apiResponse.getLinks();
        assertNull(links);

    }
}
