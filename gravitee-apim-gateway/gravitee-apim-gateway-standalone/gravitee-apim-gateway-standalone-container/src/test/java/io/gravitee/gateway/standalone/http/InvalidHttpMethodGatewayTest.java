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
import java.lang.reflect.Field;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class InvalidHttpMethodGatewayTest extends AbstractWiremockGatewayTest {

    @Test
    public void shouldRespondWithNotImplemented() throws Exception {
        wireMockRule.stubFor(any(urlEqualTo("/team/my_team")).willReturn(aResponse().withStatus(HttpStatus.SC_NOT_IMPLEMENTED)));

        Request request = Request.Get("http://localhost:8082/test/my_team");

        // A little bit of reflection to set an unknown HTTP method since the fluent API does not allow it.
        Field requestField = request.getClass().getDeclaredField("request");
        requestField.setAccessible(true);
        Field methodField = requestField.get(request).getClass().getDeclaredField("method");
        methodField.setAccessible(true);
        methodField.set(requestField.get(request), "unkown-method");

        Response response = execute(request);
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_NOT_IMPLEMENTED, returnResponse.getStatusLine().getStatusCode());

        wireMockRule.verify(anyRequestedFor(urlPathEqualTo("/team/my_team")));
    }
}
