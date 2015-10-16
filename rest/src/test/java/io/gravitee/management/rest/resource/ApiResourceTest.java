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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Test;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.management.model.ApiEntity;
import io.gravitee.management.service.PermissionType;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ApiResourceTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";

    protected String contextPath() {
        return "/apis/";
    }

    @Test
    public void shouldGetApi() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(mockApi)).when(apiService).findByName(API_NAME);

        final Response response = target(API_NAME).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        final ApiEntity responseApi = response.readEntity(ApiEntity.class);
        assertNotNull(responseApi);
        assertEquals(API_NAME, responseApi.getName());
    }

    @Test
    public void shouldNotGetApiBecauseNotFound() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.empty()).when(apiService).findByName(API_NAME);

        final Response response = target(API_NAME).request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotGetApiBecausePermissionDenied() {
        final ApiEntity mockApi = new ApiEntity();
        mockApi.setName(API_NAME);

        doReturn(Optional.of(new ApiEntity())).when(apiService).findByName(API_NAME);
        doThrow(ForbiddenAccessException.class).when(permissionService).hasPermission(USER_NAME, API_NAME, PermissionType.VIEW_API);

        final Response response = target(API_NAME).request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }
}
