/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.standalone.http2;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.connector.http.endpoint.HttpEndpoint;
import io.gravitee.connector.http.endpoint.pkcs12.PKCS12TrustStore;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.wiremock.ResourceUtils;
import java.net.URL;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;

/**
 * Based on keystore from {@link io.gravitee.gateway.standalone.http.SSLPKCS12TrustStoreTest}
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http2/ssl-pkcs12-support.json")
public class Http2SSLPKCS12TrustStoreTest extends AbstractWiremockGatewayTest {

    ObjectMapper mapper = new GraviteeMapper();

    @Override
    protected WireMockExtension getWiremockRule() {
        return WireMockExtension.newInstance()
            .options(
                wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .keystorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/keystore01.jks"))
                    .keystorePassword("password")
            )
            .build();
    }

    @Test
    public void http2_request_ssl_trustAll() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        // First call is calling an endpoint where trustAll is defined to true, no need for truststore => 200
        HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(
            HttpStatus.SC_OK,
            response.getStatusLine().getStatusCode(),
            "trustAll is defined to true, no need for truststore => 200"
        );

        // Second call is calling an endpoint where trustAll is defined to false, without truststore => 502
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(
            HttpStatus.SC_BAD_GATEWAY,
            response.getStatusLine().getStatusCode(),
            "trustAll is defined to false, without truststore => 502"
        );

        // Third call is calling an endpoint where trustAll is defined to false, with truststore => 200
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode(), "trustAll is defined to false, with truststore => 200");

        // Check that the stub has been successfully invoked by the gateway
        wireMockRule.verify(2, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
                URL target = new URL(endpoint.getTarget());
                URL newTarget = new URL(target.getProtocol(), target.getHost(), wireMockRule.getHttpsPort(), target.getFile());
                endpoint.setTarget(newTarget.toString());

                HttpEndpoint httpEndpoint = mapper.readValue(endpoint.getConfiguration(), HttpEndpoint.class);
                if (httpEndpoint.getHttpClientSslOptions() != null && httpEndpoint.getHttpClientSslOptions().getTrustStore() != null) {
                    PKCS12TrustStore trustStore = (PKCS12TrustStore) httpEndpoint.getHttpClientSslOptions().getTrustStore();
                    trustStore.setPath(ResourceUtils.toPath("io/gravitee/gateway/standalone/truststore.p12"));
                }
                endpoint.setConfiguration(mapper.writeValueAsString(httpEndpoint));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
