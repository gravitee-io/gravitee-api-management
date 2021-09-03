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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class KeyResourceTest extends AbstractResourceTest {

    private final String API_KEY_ID = "api-key-id";

    @Override
    protected String contextPath() {
        return "keys/";
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
    }

    @Test
    public void delete_with_id_should_call_revoke_service_and_return_http_204() {
        Response response = envTarget(API_KEY_ID).request().delete();

        verify(apiKeyService, times(1)).revoke(API_KEY_ID, true);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void delete_with_id_should_return_http_500_on_exception() {
        doThrow(TechnicalManagementException.class).when(apiKeyService).revoke(any(String.class), any(Boolean.class));

        Response response = envTarget(API_KEY_ID).request().delete();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void put_with_id_should_return_http_400_if_entity_id_does_not_match() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("another-api-key-id");

        Response response = envTarget(API_KEY_ID).request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void put_with_id_should_call_service_update_and_return_http_200() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(API_KEY_ID);

        Response response = envTarget(API_KEY_ID).request().put(Entity.json(apiKey));

        verify(apiKeyService, times(1)).update(apiKey);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void put_with_id_should_return_http_500_on_exception() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(API_KEY_ID);

        when(apiKeyService.update(any())).thenThrow(TechnicalManagementException.class);

        Response response = envTarget(API_KEY_ID).request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
