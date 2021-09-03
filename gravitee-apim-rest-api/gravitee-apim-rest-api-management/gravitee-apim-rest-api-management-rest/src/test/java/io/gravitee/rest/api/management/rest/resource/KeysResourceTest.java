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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class KeysResourceTest extends AbstractResourceTest {

    private final String API_KEY = "my-api-key-123546";
    private final String APPLICATION_ID = "application-id";
    private final String API_ID = "api-id";

    @Override
    protected String contextPath() {
        return "keys/";
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_omitted() {
        Response response = envTarget("_canCreate").queryParam("application", APPLICATION_ID).queryParam("api", API_ID).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_is_invalid_format() {
        Response response = envTarget("_canCreate")
            .queryParam("key", "short")
            .queryParam("application", APPLICATION_ID)
            .queryParam("api", API_ID)
            .request()
            .get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_return_http_400_if_application_query_param_omitted() {
        Response response = envTarget("_canCreate").queryParam("key", API_KEY).queryParam("api", API_ID).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_return_http_400_if_api_query_param_omitted() {
        Response response = envTarget("_canCreate").queryParam("key", API_KEY).queryParam("application", APPLICATION_ID).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_call_service_and_return_http_200_containing_true() {
        when(apiKeyService.canCreate(API_KEY, API_ID, APPLICATION_ID)).thenReturn(true);

        Response response = envTarget("_canCreate")
            .queryParam("key", API_KEY)
            .queryParam("application", APPLICATION_ID)
            .queryParam("api", API_ID)
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertTrue(response.readEntity(Boolean.class));
    }

    @Test
    public void get_canCreate_should_call_service_and_return_http_200_containing_false() {
        when(apiKeyService.canCreate(API_KEY, API_ID, APPLICATION_ID)).thenReturn(false);

        Response response = envTarget("_canCreate")
            .queryParam("key", API_KEY)
            .queryParam("application", APPLICATION_ID)
            .queryParam("api", API_ID)
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertFalse(response.readEntity(Boolean.class));
    }

    @Test
    public void get_canCreate_should_return_http_500_on_exception() {
        when(apiKeyService.canCreate(API_KEY, API_ID, APPLICATION_ID)).thenThrow(TechnicalManagementException.class);

        Response response = envTarget("_canCreate")
            .queryParam("key", API_KEY)
            .queryParam("application", APPLICATION_ID)
            .queryParam("api", API_ID)
            .request()
            .get();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }
}
