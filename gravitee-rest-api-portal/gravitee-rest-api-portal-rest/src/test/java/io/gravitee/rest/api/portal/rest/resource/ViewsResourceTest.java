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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.DataResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ViewsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "views";
    }
    
    @Before
    public void init() {
        resetAllMocks();
        
        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findByUser(any(), any());
        doReturn(mockApis).when(apiService).search(any());
        
        ViewEntity view1 = new ViewEntity();
        view1.setId("1");
        view1.setHidden(false);
        view1.setOrder(2);
        
        ViewEntity view2 = new ViewEntity();
        view2.setId("2");
        view2.setHidden(false);
        view2.setOrder(3);
        
        ViewEntity view3 = new ViewEntity();
        view3.setId("3");
        view3.setHidden(true);
        view3.setOrder(1);
        
        List<ViewEntity> mockViews = Arrays.asList(view1, view2, view3);
        doReturn(mockViews).when(viewService).findAll();

        Function<ViewEntity, ViewEntity> identity = (v) -> v;
        doReturn(identity).when(viewEnhancer).enhance(any());
        
        Mockito.when(viewMapper.convert(any(), any())).thenCallRealMethod();
        
    }
    
    @Test
    public void shouldGetNotHiddenViews() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(apiService).findByUser(any(), any());
        DataResponse viewsResponse = response.readEntity(DataResponse.class);
        assertEquals(2, viewsResponse.getData().size());
        
    }
    
    @Test
    public void shouldGetNoView() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("400", error.getCode());
        assertEquals("javax.ws.rs.BadRequestException", error.getTitle());
        assertEquals("page is not valid", error.getDetail());
    }
    
    @Test
    public void shouldGetNoPublishedApiAndNoLink() {

        doReturn(new ArrayList<>()).when(viewService).findAll();
        
        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        DataResponse viewsResponse = response.readEntity(DataResponse.class);
        assertEquals(0, viewsResponse.getData().size());
        
        Links links = viewsResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        viewsResponse = anotherResponse.readEntity(DataResponse.class);
        assertEquals(0, viewsResponse.getData().size());
        
        links = viewsResponse.getLinks();
        assertNull(links);

    }
}
