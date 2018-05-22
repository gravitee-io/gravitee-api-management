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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.cors.CorsHandler;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/cors.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class CorsTest extends AbstractGatewayTest {

    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort());

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .outerRule(wireMockRule)
            .around(new ApiDeployer(this));

    @Test
    public void preflight_request() throws Exception {
        Request request = Request.Options("http://localhost:8082/test/my_team")
                .addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .addHeader(HttpHeaders.ORIGIN, "http://localhost");

        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void preflight_request_unauthorized() throws Exception {
        Request request = Request.Options("http://localhost:8082/test/my_team")
                .addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                .addHeader(HttpHeaders.ORIGIN, "http://bad_host");

        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_BAD_REQUEST, returnResponse.getStatusLine().getStatusCode());
    }

    @Test
    public void simple_request() throws Exception {
        stubFor(get(urlEqualTo("/team/my_team"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody("{\"key\": \"value\"}")));

        org.apache.http.client.fluent.Request request = org.apache.http.client.fluent.Request.Get("http://localhost:8082/test/my_team");
        org.apache.http.client.fluent.Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();

        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());
        assertEquals(CorsHandler.ALLOW_ORIGIN_PUBLIC_WILDCARD, returnResponse.getFirstHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());
        assertEquals("x-forwarded-host", returnResponse.getFirstHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).getValue());

        // Check that the stub has never been invoked by the gateway
        verify(1, getRequestedFor(urlEqualTo("/team/my_team")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            Endpoint edpt = api.getProxy().getGroups().iterator().next().getEndpoints().iterator().next();
            URL target = new URL(edpt.getTarget());
            URL newTarget = new URL(target.getProtocol(), target.getHost(), wireMockRule.port(), target.getFile());
            edpt.setTarget(newTarget.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
