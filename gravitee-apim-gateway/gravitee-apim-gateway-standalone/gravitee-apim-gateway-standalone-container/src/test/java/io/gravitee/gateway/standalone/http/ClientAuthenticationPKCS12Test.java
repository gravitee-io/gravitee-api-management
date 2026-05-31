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
package io.gravitee.gateway.standalone.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.gravitee.connector.http.endpoint.HttpEndpoint;
import io.gravitee.connector.http.endpoint.pkcs12.PKCS12KeyStore;
import io.gravitee.connector.http.endpoint.pkcs12.PKCS12TrustStore;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.gravitee.gateway.standalone.wiremock.ResourceUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.jupiter.api.Test;

/**
 *
 * JKS has been generated from SSLJKSTrustStoreTest
 *
 * Generate a self-signed keystore
 * > keytool -importkeystore -srckeystore keystore01.jks -srcstorepass password -destkeystore keystore.p12 -deststoretype PKCS12 -deststorepass password
 *
 * Generate the client truststore
 * > keytool -importkeystore -srckeystore truststore01.jks -srcstorepass password -destkeystore truststore.p12 -deststoretype PKCS12 -deststorepass password
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/client-authentication-pkcs12-support.json")
public class ClientAuthenticationPKCS12Test extends AbstractWiremockGatewayTest {

    ObjectMapper mapper = new GraviteeMapper();

    @Override
    protected WireMockExtension getWiremockRule() {
        return WireMockExtension.newInstance()
            .options(
                wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .needClientAuth(true)
                    .trustStorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/truststore01.jks"))
                    .trustStorePassword("password")
                    .keystorePath(ResourceUtils.toPath("io/gravitee/gateway/standalone/keystore01.jks"))
                    .keystorePassword("password")
            )
            .build();
    }

    @Test
    public void simple_request_client_auth() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        // First call is calling an HTTPS endpoint without ssl configuration => 502
        HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode(), "without ssl configuration => 502");

        // Second call is calling an endpoint where trustAll = false, without truststore => 502
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(
            HttpStatus.SC_BAD_GATEWAY,
            response.getStatusLine().getStatusCode(),
            "trustAll = false, with keystore and without truststore => 200"
        );

        // Third call is calling an endpoint where trustAll = false, with truststore and keystore => 200
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode(), "trustAll = false, with truststore and keystore => 200");

        // Fourth call is calling an endpoint where trustAll = true, with keystore and without truststore => 200
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals(
            HttpStatus.SC_OK,
            response.getStatusLine().getStatusCode(),
            "trustAll = true,with keystore and  without truststore => 200"
        );

        // Check that the stub has been successfully invoked by the gateway
        wireMockRule.verify(2, getRequestedFor(urlPathEqualTo("/team/my_team")));
    }

    @Override
    public void before(Api api) {
        super.before(api);

        try {
            for (Endpoint endpoint : api.getProxy().getGroups().iterator().next().getEndpoints()) {
                endpoint.setTarget(exchangePort(endpoint.getTarget(), wireMockRule.getHttpsPort()));

                HttpEndpoint httpEndpoint = mapper.readValue(endpoint.getConfiguration(), HttpEndpoint.class);
                if (httpEndpoint.getHttpClientSslOptions() != null && httpEndpoint.getHttpClientSslOptions().getKeyStore() != null) {
                    PKCS12KeyStore keyStore = (PKCS12KeyStore) httpEndpoint.getHttpClientSslOptions().getKeyStore();
                    keyStore.setPath(ResourceUtils.toPath("io/gravitee/gateway/standalone/keystore.p12"));
                }
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
