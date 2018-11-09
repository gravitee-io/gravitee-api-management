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
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.standalone.junit.annotation.ApiConfiguration;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.junit.rules.ApiDeployer;
import io.gravitee.gateway.standalone.junit.rules.ApiPublisher;
import io.gravitee.gateway.standalone.servlet.TeamServlet;
import io.gravitee.gateway.standalone.wiremock.ResourceUtils;
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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/client-authentication-pem-inline-support.json")
@ApiConfiguration(
        servlet = TeamServlet.class,
        contextPath = "/team"
)
public class ClientAuthenticationPEMInlineTest extends AbstractGatewayTest {

    // PKCS12 has been generated from SSLJKSTrustStoreTest

    // Extract the private key
    // openssl pkcs12 -in keystore.p12 -passin pass:password -nodes | openssl pkcs8 -topk8 -inform PEM -outform PEM -out client-key.pem -nocrypt

    // Extract the X.509 certificate from the PCS12 store
    // openssl pkcs12 -in keystore.p12 -passin pass:password -nokeys -out client-cert.pem

    private WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .dynamicPort()
            .dynamicHttpsPort()
            .needClientAuth(true)
            .trustStorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/truststore01.jks"))
            .trustStorePassword("password")
            .keystorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/keystore01.jks"))
            .keystorePassword("password"));

    @Rule
    public final TestRule chain = RuleChain
            .outerRule(new ApiPublisher())
            .outerRule(wireMockRule)
            .around(new ApiDeployer(this));

    @Test
    public void simple_request_client_auth() throws Exception {
        stubFor(get(urlEqualTo("/team/my_team"))
                .willReturn(aResponse()
                        .withStatus(HttpStatus.SC_OK)
                        .withBody("{\"key\": \"value\"}")));

        // First call is calling an HTTPS endpoint without ssl configuration => 502
        Request request = Request.Get("http://localhost:8082/test/my_team");
        Response response = request.execute();
        HttpResponse returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_BAD_GATEWAY, returnResponse.getStatusLine().getStatusCode());

        // Second call is calling an endpoint where trustAll = false, without keystore => 200
        request = Request.Get("http://localhost:8082/test/my_team");
        response = request.execute();
        returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Third call is calling an endpoint where trustAll = true, with keystore => 200
        request = Request.Get("http://localhost:8082/test/my_team");
        response = request.execute();
        returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Third call is calling an endpoint where trustAll = false, with truststore and keystore => 200
        request = Request.Get("http://localhost:8082/test/my_team");
        response = request.execute();
        returnResponse = response.returnResponse();
        assertEquals(HttpStatus.SC_OK, returnResponse.getStatusLine().getStatusCode());

        // Check that the stub has been successfully invoked by the gateway
        verify(3, getRequestedFor(urlEqualTo("/team/my_team")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
                URL target = new URL(endpoint.getTarget());
                URL newTarget = new URL(target.getProtocol(), target.getHost(), wireMockRule.httpsPort(), target.getFile());
                endpoint.setTarget(newTarget.toString());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
