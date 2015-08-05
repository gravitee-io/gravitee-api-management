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
package io.gravitee.management.api.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.api.builder.ApiBuilder;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.LifecycleState;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApisResourceTest extends AbstractResourceTest {

    @Autowired
    private ApiRepository apiRepository;

    @Test
    public void testGetApis_findAllReturnNull() {
        Mockito.doReturn(null).when(apiRepository).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<Api> apis = response.readEntity(Set.class);

        assertTrue(apis.isEmpty());
    }

    @Test
    public void testGetApis_findAllReturnEmpty() {
        Mockito.doReturn(new HashSet<>()).when(apiRepository).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<Api> apis = response.readEntity(Set.class);

        assertTrue(apis.isEmpty());
    }

    @Test
    public void testGetApis_findAllReturnApis() {
        Set<Api> apis = new HashSet<>();
        apis.add(new ApiBuilder().name("my-api").origin("http://localhost/my-api").target("http://remote_api/context").build());
        apis.add(new ApiBuilder().name("my-api2").origin("http://localhost/my-api2").target("http://remote_api2/context").build());

        Mockito.doReturn(apis).when(apiRepository).findAll();
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Set<Api> retrievedApis = response.readEntity(Set.class);

        assertEquals(apis.size(), retrievedApis.size());
    }

    @Test
    public void testPostApi_nullApi() {
        final Response response = target().request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void testPostApi_correctApi() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/my-api").target("http://remote_api/context").build();

        Api api2 = new ApiBuilder()
                .name("my-api")
                .origin("http://localhost/my-api")
                .target("http://remote_api/context")
                .createdAt(new Date())
                .build();

        Mockito.doReturn(api2).when(apiRepository).create(Mockito.any(Api.class));

        final Response response = target().request().post(Entity.entity(api, MediaType.APPLICATION_JSON_TYPE));
        Api createdApi = response.readEntity(Api.class);

        // Check HTTP response
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.LOCATION));

        // Check Response content
        assertEquals(LifecycleState.STOPPED, createdApi.getLifecycleState());
        assertNotNull(createdApi.getCreatedAt());
        assertNotNull(createdApi.getUpdatedAt());

        assertEquals(createdApi.getCreatedAt(), createdApi.getUpdatedAt());
    }

    private WebTarget target() {
        return target("apis");
    }
}
