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
import static org.junit.Assert.assertTrue;

import io.gravitee.definition.jackson.AbstractTest;
import io.gravitee.definition.model.HttpResponse;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpResponseDeserializerTest extends AbstractTest {

    @Test
    public void definition_defaultHttpResponse() throws Exception {
        HttpResponse response = load("/io/gravitee/definition/jackson/httpresponse-simpleheaders.json", HttpResponse.class);

        assertEquals(200, response.getStatusCode());
        assertEquals("response-body", response.getBody());
        assertEquals(4, response.getHeaders().size());
        assertEquals(1, response.getHeaders().get("transfer-encoding").size());
        assertEquals("chunked", response.getHeaders().get("transfer-encoding").get(0));
    }

    @Test
    public void definition_defaultHttpResponseMultiValueHeaders() throws Exception {
        HttpResponse response = load("/io/gravitee/definition/jackson/httpresponse-multivalueheaders.json", HttpResponse.class);

        assertEquals(200, response.getStatusCode());
        assertEquals("response-body", response.getBody());
        assertEquals(5, response.getHeaders().size());
        assertEquals(1, response.getHeaders().get("transfer-encoding").size());
        assertEquals("chunked", response.getHeaders().get("transfer-encoding").get(0));
        assertEquals(3, response.getHeaders().get("accept-encoding").size());
        assertTrue(response.getHeaders().get("accept-encoding").contains("deflate"));
        assertTrue(response.getHeaders().get("accept-encoding").contains("gzip"));
        assertTrue(response.getHeaders().get("accept-encoding").contains("compress"));
    }
}
