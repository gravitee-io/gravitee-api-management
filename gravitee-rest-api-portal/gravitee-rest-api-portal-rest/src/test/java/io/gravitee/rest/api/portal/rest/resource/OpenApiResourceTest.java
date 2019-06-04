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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.http.HttpHeader;
import org.junit.Test;

import io.gravitee.common.http.MediaType;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OpenApiResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "";
    }

    @Test
    public void shouldGetOpenApiSpecification() throws IOException {
        
        final Response response = root().path("openapi").request().get();
        assertEquals(OK_200, response.getStatus());

        MultivaluedMap<String, Object> headers = response.getHeaders();
        String contentType = (String) headers.getFirst(HttpHeader.CONTENT_TYPE.asString());

        assertEquals(MediaType.TEXT_PLAIN, contentType);

        String result = response.readEntity(String.class);
        assertEquals("DUMMY OPENAPI", result);
        
    }
    
    //APIPortal: add a test if spec file does not exist
}
