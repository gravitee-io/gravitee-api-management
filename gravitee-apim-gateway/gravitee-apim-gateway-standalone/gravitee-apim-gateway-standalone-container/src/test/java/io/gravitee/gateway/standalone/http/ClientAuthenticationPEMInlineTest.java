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

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.wiremock.ResourceUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertEquals;

/**
 *
 * PKCS12 has been generated from SSLJKSTrustStoreTest
 *
 * Extract the private key
 * > openssl pkcs12 -in keystore.p12 -passin pass:password -nodes | openssl pkcs8 -topk8 -inform PEM -outform PEM -out client-key.pem -nocrypt
 *
 * Extract the X.509 certificate from the PCS12 store
 * > openssl pkcs12 -in keystore.p12 -passin pass:password -nokeys -out client-cert.pem
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/client-authentication-pem-inline-support.json")
public class ClientAuthenticationPEMInlineTest extends AbstractWiremockGatewayTest {

    @Override
    protected WireMockRule getWiremockRule() {
        return new WireMockRule(wireMockConfig()
                .dynamicPort()
                .dynamicHttpsPort()
                .needClientAuth(true)
                .trustStorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/truststore01.jks"))
                .trustStorePassword("password")
                .keystorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/keystore01.jks"))
                .keystorePassword("password"));
    }

    @Test
    public void simple_request_client_auth() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        // First call is calling an HTTPS endpoint without ssl configuration => 502
        HttpResponse response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals("without ssl configuration => 502",
                HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode());

        // Second call is calling an endpoint where trustAll = false, without keystore => 502
        response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals("trustAll = false, without keystore => 200",
                HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode());

        // Third call is calling an endpoint where trustAll = true, with keystore => 200
        response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals("trustAll = true, with keystore => 200",
                HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Fourth call is calling an endpoint where trustAll = false, with truststore and keystore => 200
        response = Request.Get("http://localhost:8082/test/my_team").execute().returnResponse();
        assertEquals("trustAll = false, with truststore and keystore => 200",
                HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // Check that the stub has been successfully invoked by the gateway
        wireMockRule.verify(2, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
                endpoint.setTarget(exchangePort(endpoint.getTarget(), wireMockRule.httpsPort()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
