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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.ApiService;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiService apiService;

    protected String contextPath() {
        return "apis";
    }

    @Test
    @Ignore
    public void testGetApis_findAllReturnNull() {
        Mockito.doReturn(null).when(apiService).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<ApiEntity> apis = response.readEntity(Set.class);

        assertTrue(apis.isEmpty());
    }

    @Test
    @Ignore
    public void testGetApis_findAllReturnEmpty() {
        Mockito.doReturn(new HashSet<>()).when(apiService).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<ApiEntity> apis = response.readEntity(Set.class);

        assertTrue(apis.isEmpty());
    }

    @Test
    @Ignore
    public void testGetApis_findAllReturnApis() {
        Set<ApiEntity> apis = new HashSet<>();

        /*
        apis.add(new ApiBuilder().name("my-api").origin("http://localhost/my-api").target("http://remote_api/context").build());
        apis.add(new ApiBuilder().name("my-api2").origin("http://localhost/my-api2").target("http://remote_api2/context").build());
        */

        Mockito.doReturn(apis).when(apiService).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<ApiEntity> retrievedApis = response.readEntity(Set.class);

        assertEquals(apis.size(), retrievedApis.size());
    }

    @Test
    @Ignore
    public void testPostApi_nullApi() {
        final Response response = target().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    @Ignore
    public void testPostApi_correctApi() {
                       /*
 ApiEntity api = new ApiBuilder().name("my-api").build();//.origin("http://localhost/my-api").target("http://remote_api/context").build();

        ApiEntity api2 = new ApiBuilder()
                .name("my-api")
                .origin("http://localhost/my-api")
                .target("http://remote_api/context")
                .createdAt(new Date())
                .build();

        Mockito.doReturn(api2).when(apiService).createForUser(Mockito.any(NewApiEntity.class), Mockito.anyString());

        final Response response = target().request().post(Entity.entity(api, MediaType.APPLICATION_JSON_TYPE));
        ApiEntity createdApi = response.readEntity(ApiEntity.class);

        // Check HTTP response
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.LOCATION));

        // Check Response content
        assertNotNull(createdApi.getCreatedAt());
        assertNotNull(createdApi.getUpdatedAt());

        assertEquals(createdApi.getCreatedAt(), createdApi.getUpdatedAt());                */

    }
}
