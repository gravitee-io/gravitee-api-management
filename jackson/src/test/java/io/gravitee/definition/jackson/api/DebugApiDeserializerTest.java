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
package io.gravitee.definition.jackson.api;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.debug.DebugApi;
import java.util.List;
import org.junit.Test;

public class DebugApiDeserializerTest extends AbstractTest {

    @Test
    public void debugApi_withRequest() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-request.json", DebugApi.class);

        assertEquals(debugApi.getRequest().getPath(), "/");
        assertEquals(debugApi.getRequest().getMethod(), "GET");
        assertEquals(debugApi.getRequest().getBody(), "request-body");
        assertEquals(debugApi.getRequest().getHeaders().size(), 4);
        assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Transaction-Id"), List.of("transaction-id"));
        assertEquals(debugApi.getRequest().getHeaders().get("content-type"), List.of("application/json"));
        assertEquals(debugApi.getRequest().getHeaders().get("X-Gravitee-Request-Id"), List.of("request-id"));
        assertEquals(debugApi.getRequest().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test
    public void debugApi_withResponse() throws Exception {
        DebugApi debugApi = load("/io/gravitee/definition/jackson/debug/debug-api-with-response.json", DebugApi.class);

        assertEquals(debugApi.getResponse().getStatusCode(), 200);
        assertEquals(debugApi.getResponse().getBody(), "response-body");
        assertEquals(debugApi.getResponse().getHeaders().size(), 2);
        assertEquals(debugApi.getResponse().getHeaders().get("transfer-encoding"), List.of("chunked"));
        assertEquals(debugApi.getResponse().getHeaders().get("accept-encoding"), List.of("deflate", "gzip", "compress"));
    }

    @Test(expected = JsonMappingException.class)
    public void debugApi_withoutRequest() throws Exception {
        load("/io/gravitee/definition/jackson/debug/debug-api-without-request.json", DebugApi.class);
    }
}
