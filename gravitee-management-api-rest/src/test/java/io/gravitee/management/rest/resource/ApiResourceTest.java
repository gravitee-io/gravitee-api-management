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

import io.gravitee.definition.model.Proxy;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.model.UpdateApiEntity;
import io.gravitee.management.rest.resource.param.LifecycleActionParam;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Optional;

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Ignore
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";

    protected String contextPath() {
        return "apis/"+API_NAME;
    }

    @Test
    public void shouldGetApi() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(mockApi).when(apiService).findById(API_NAME);

        final Response response = target().request().get();

        assertEquals(OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API_NAME, responseApi.getName());
    }

    @Test
    public void shouldNotGetApiBecauseNotFound() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.empty()).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).request().get();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotGetApiBecausePermissionDenied() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(new ApiEntity()).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).request().get();

        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldStartApi() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.START).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());

        verify(apiService).start(API_NAME, "admin");
    }

    @Test
    public void shouldNotStartApiBecauseNotFound() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.empty()).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.START).request().post(null);

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotStartApiBecausePermissionDenied() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.START).request().post(null);

        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldStopApi() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.STOP).request().post(null);

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldNotStopApiBecauseNotFound() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.empty()).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.STOP).request().post(null);

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotStopApiBecausePermissionDenied() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).queryParam("action", LifecycleActionParam.LifecycleAction.STOP).request().post(null);

        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldUpdateApi() {
        final UpdateApiEntity mockApi = new UpdateApiEntity();
        mockApi.setVersion("v1");
        mockApi.setDescription("Description of my API");
        mockApi.setProxy(new Proxy());

        doReturn(new ApiEntity()).when(apiService).update(API_NAME, mockApi);

        final Response response = target(API_NAME).request().put(Entity.json(mockApi));

        assertEquals(NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void shouldNotUpdateApiBecausePermissionDenied() {
        final UpdateApiEntity mockApi = new UpdateApiEntity();
        mockApi.setVersion("v1");
        mockApi.setDescription("Description of my API");
        mockApi.setProxy(new Proxy());

        final Response response = target(API_NAME).request().put(Entity.json(mockApi));

        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    /*
    @Test
    public void shouldDeleteApi() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).request().delete();

        assertEquals(NO_CONTENT_204, response.getStatus());

        verify(apiService).delete(API_NAME);
    }
    */

    @Test
    public void shouldNotDeleteApiBecauseNotFound() {
        doReturn(Optional.empty()).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).request().delete();

        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotDeleteApiBecausePermissionDenied() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findById(API_NAME);

        final Response response = target(API_NAME).request().delete();

        assertEquals(FORBIDDEN_403, response.getStatus());
    }
}
