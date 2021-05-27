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
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/echo.json")
public class RoundRobinLoadBalancingSingleTest extends AbstractWiremockGatewayTest {

    @Test
    public void call_round_robin_lb_single_endpoint() throws Exception {
        wireMockRule.stubFor(get("/api2").willReturn(ok()));

        Request request = Request.Get("http://localhost:8082/api");

        // Set the first endpoint with down status
        api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next().setStatus(Endpoint.Status.DOWN);

        int calls = 20;

        for (int i = 0; i < calls; i++) {
            HttpResponse response = execute(request).returnResponse();

            assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        }

        wireMockRule.verify(0, getRequestedFor(urlPathEqualTo("/api1")));
        wireMockRule.verify(calls, getRequestedFor(urlPathEqualTo("/api2")));
    }
}
