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
package testcases;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.utils.ResourceUtils;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 *
 * In this testcase, we rely on the fact {@link AbstractGatewayTest#updateEndpoints(Api)} will update all endpoints with URL starting with "https" to automatically replace the HTTPS port configured on the wiremock server.
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
@GatewayTest
@DeployApi("/apis/client-authentication-pem-inline-support.json")
public class ClientAuthenticationPEMInlineTestCase extends AbstractGatewayTest {

    public static final String ENDPOINT = "/team/my_team";
    public static final String API_ENTRYPOINT = "/test/my_team";

    @Override
    protected void configureWireMock(WireMockConfiguration configuration) {
        configuration
            .needClientAuth(true)
            .trustStorePath(ResourceUtils.toPath("certs/truststore01.jks"))
            .trustStorePassword("password")
            .keystorePath(ResourceUtils.toPath("certs/keystore01.jks"))
            .keystorePassword("password");
    }

    @Test
    @DisplayName("Should test the different endpoints thanks to round robin and have the correct status depending on their configuration")
    void simple_request_client_auth(WebClient client) throws Exception {
        wiremock.stubFor(get(ENDPOINT).willReturn(ok()));

        // First call is calling an HTTPS endpoint without ssl configuration => 502
        TestObserver<HttpResponse<Buffer>> obs = client.get(API_ENTRYPOINT).rxSend().test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
                    return true;
                }
            )
            .assertNoErrors();

        // Second call is calling an endpoint where trustAll = false, without keystore => 502
        obs = client.get(API_ENTRYPOINT).rxSend().test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
                    return true;
                }
            )
            .assertNoErrors();

        // Third call is calling an endpoint where trustAll = true, with keystore => 200
        obs = client.get(API_ENTRYPOINT).rxSend().test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                    return true;
                }
            )
            .assertNoErrors();

        // Fourth call is calling an endpoint where trustAll = false, with truststore and keystore => 200
        obs = client.get(API_ENTRYPOINT).rxSend().test();

        awaitTerminalEvent(obs);
        obs
            .assertComplete()
            .assertValue(
                response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.OK_200);
                    return true;
                }
            )
            .assertNoErrors();

        // Check that the stub has been successfully invoked by the gateway
        wiremock.verify(2, getRequestedFor(urlPathEqualTo(ENDPOINT)));
    }
}
