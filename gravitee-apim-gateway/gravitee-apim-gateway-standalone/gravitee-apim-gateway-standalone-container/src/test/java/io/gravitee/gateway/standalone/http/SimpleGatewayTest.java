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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class SimpleGatewayTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_get_started_api_noBody() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(
            Request
                .Get("http://localhost:8082/test/my_team")
                .addHeader("test", "test01")
                .addHeader("test", "test02")
                .addHeader("single", "single")
        )
            .returnResponse();

        assertEquals(HttpStatusCode.OK_200, response.getStatusLine().getStatusCode());
        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_started_api_withBody() throws Exception {
        final String request = "This is a dummy request payload";

        wireMockRule.stubFor(post("/team/my_team").willReturn(ok()));

        final HttpResponse response = execute(
            Request.Post("http://localhost:8082/test/my_team").bodyString(request, ContentType.TEXT_PLAIN)
        )
            .returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Test
    public void call_get_started_api_withStatusMessage() throws Exception {
        final String request = "This is a dummy request payload";

        wireMockRule.stubFor(post("/team/my_team").willReturn(ok().withStatusMessage("dummy-message")));

        final HttpResponse response = execute(
            Request.Post("http://localhost:8082/test/my_team").bodyString(request, ContentType.TEXT_PLAIN)
        )
            .returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertEquals("dummy-message", response.getStatusLine().getReasonPhrase());

        wireMockRule.verify(postRequestedFor(urlPathEqualTo("/team/my_team")));
    }
}
