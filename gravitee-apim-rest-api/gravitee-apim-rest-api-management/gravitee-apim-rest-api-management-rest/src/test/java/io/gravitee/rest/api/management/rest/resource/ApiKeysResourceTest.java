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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysResourceTest extends AbstractResourceTest {

    private static final String API_KEY = "atLeast10CharsButLessThan64";

    @Override
    protected String contextPath() {
        return "apis/my-api/keys/";
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
    }

    @Test
    public void shouldReturnApiKeyAvailabilityTrue_whenNoKeyFound() {
        when(apiKeyService.findByKey(API_KEY)).thenReturn(Collections.emptyList());

        Response response = envTarget("_verify").queryParam("apiKey", API_KEY).request().post(null);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertTrue(response.readEntity(Boolean.class));
    }

    @Test
    public void shouldReturnApiKeyAvailabilityFalse_whenAtLeast1KeyFound() {
        when(apiKeyService.findByKey(API_KEY)).thenReturn(List.of(new ApiKeyEntity()));

        Response response = envTarget("_verify").queryParam("apiKey", API_KEY).request().post(null);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertFalse(response.readEntity(Boolean.class));
    }

    @Test
    public void shouldReturn500IfInternalError() {
        when(apiKeyService.findByKey(API_KEY)).thenThrow(new InternalError());

        Response response = envTarget("_verify").queryParam("apiKey", API_KEY).request().post(null);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldReturn400IfBadParameter() {
        Response response = envTarget("_verify").queryParam("apiKey", "short/\\;").request().post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400IfNoApiKeyParam() {
        Response response = envTarget("_verify").request().post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
