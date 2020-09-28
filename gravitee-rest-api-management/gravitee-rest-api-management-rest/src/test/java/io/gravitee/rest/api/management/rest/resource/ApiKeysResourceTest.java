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

import io.gravitee.common.http.HttpStatusCode;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiKeysResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis/my-api/keys/";
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
    }

    @Test
    public void shouldReturnApiKeyAvailability() {
        when(apiKeyService.exists(anyString())).thenReturn(true);

        Response response = envTarget("_verify")
                .queryParam("apiKey", "atLeast10CharsButLessThan64")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertEquals(false, response.readEntity(Boolean.class));
    }

    @Test
    public void shouldReturn500IfInternalError() {
        when(apiKeyService.exists(anyString())).thenThrow(new InternalError());

        Response response = envTarget("_verify")
                .queryParam("apiKey", "atLeast10CharsButLessThan64")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void shouldReturn400IfBadParameter() {

        Response response = envTarget("_verify")
                .queryParam("apiKey", "short/\\;")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldReturn400IfNoApiKeyParam() {

        Response response = envTarget("_verify")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}