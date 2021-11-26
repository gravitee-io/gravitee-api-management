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
package io.gravitee.gateway.standalone.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/no-redirect.json")
public class UnfollowRedirectTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldNotFollowRedirect() throws Exception {
        wireMockRule.stubFor(get("/redirect").willReturn(permanentRedirect("http://localhost:" + wireMockRule.port() + "/final")));

        HttpClient client = HttpClientBuilder.create().disableRedirectHandling().build();

        Request request = Request.Get("http://localhost:8082/api/redirect");
        Response response = Executor.newInstance(client).execute(request);
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_MOVED_PERMANENTLY, returnResponse.getStatusLine().getStatusCode());

        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/redirect")));
        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/final")));
    }
}
