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
package io.gravitee.gateway.standalone;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/teams.json")
public class NotFoundGatewayTest extends AbstractGatewayTest {

    @Test
    public void shouldReturnNotFound_noApi() throws IOException {
        HttpResponse response = Request.Get("http://localhost:8082/unknow").execute().returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatusLine().getStatusCode());
    }

    @Test
    public void shouldReturnNotFound_apiNotStarted() throws Exception {
        HttpResponse response = Request.Get("http://localhost:8082/not_started_api").execute().returnResponse();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatusLine().getStatusCode());
    }
}
