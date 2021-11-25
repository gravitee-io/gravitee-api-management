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

import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import java.util.Iterator;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/echo.json")
public class RoundRobinLoadBalancingMultipleTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_round_robin_lb_multiple_endpoints() throws Exception {
        wireMockRule.stubFor(get("/api1").willReturn(ok()));
        wireMockRule.stubFor(get("/api2").willReturn(ok()));
        wireMockRule.stubFor(get("/api3").willReturn(ok()));

        Request request = Request.Get("http://localhost:8082/api");

        int calls = 20;

        for (int i = 0; i < calls; i++) {
            HttpResponse response = request.execute().returnResponse();

            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            wireMockRule.verify((i / 2) + 1, getRequestedFor(urlEqualTo("/api" + (i % 2 + 1))));
        }

        wireMockRule.verify(calls / 2, getRequestedFor(urlPathEqualTo("/api1")));
        wireMockRule.verify(calls / 2, getRequestedFor(urlPathEqualTo("/api2")));
        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/api3")));
    }

    @Test
    public void call_round_robin_lb_multiple_endpoints_only_secondary() throws Exception {
        wireMockRule.stubFor(get("/api1").willReturn(ok()));
        wireMockRule.stubFor(get("/api2").willReturn(ok()));
        wireMockRule.stubFor(get("/api3").willReturn(ok()));

        Iterator<Endpoint> endpointsIte = api.getProxy().getGroups().iterator().next().getEndpoints().iterator();

        // Set the first endpoint with down status
        endpointsIte.next().setStatus(Endpoint.Status.DOWN);

        // Set the second endpoint with down status
        endpointsIte.next().setStatus(Endpoint.Status.DOWN);

        Request request = Request.Get("http://localhost:8082/api");

        int calls = 20;

        for (int i = 0; i < calls; i++) {
            HttpResponse response = execute(request).returnResponse();

            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        }

        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/api1")));
        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/api2")));
        wireMockRule.verify(calls, getRequestedFor(urlPathEqualTo("/api3")));
    }
}
