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
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Test;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/echo-lb-weight.json")
public class WeightedRoundRobinLoadBalancingTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_weighted_lb_multiple_endpoints() throws Exception {
        wireMockRule.stubFor(get(urlEqualTo("/api1")).willReturn(ok()));
        wireMockRule.stubFor(get(urlEqualTo("/api2")).willReturn(ok()));

        Request request = Request.Get("http://localhost:8082/api");

        int calls = 10;

        for (int i = 0; i < calls; i++) {
            Response response = execute(request);
            HttpResponse returnResponse = response.returnResponse();

            assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
        }

        wireMockRule.verify(3, getRequestedFor(urlPathEqualTo("/api1")));
        wireMockRule.verify(7, getRequestedFor(urlPathEqualTo("/api2")));
    }
}
