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
package testcases;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.apim.gateway.tests.sdk.AbstractHttp2GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava3.core.http.HttpClient;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/teams.json")
@EnableForGatewayTestingExtensionTesting
public class Http2HeadersTestCase extends AbstractHttp2GatewayTest {

    public static final String ENDPOINT = "/team/my_team";

    @Test
    void should_conserve_multi_values(HttpClient httpClient) throws InterruptedException {
        String cookie1 = "JSESSIONID=ABSCDEDASDSSDSSE.oai007; path=/; Secure; HttpOnly";
        String cookie2 = "JSESSIONID=BASCDEDASDSSDSSE.oai008; path=/another; Secure; HttpOnly";

        wiremock.stubFor(
            get(ENDPOINT).willReturn(ok().withHeader(HttpHeaderNames.SET_COOKIE, cookie1).withHeader(HttpHeaderNames.SET_COOKIE, cookie2))
        );

        httpClient
            .rxRequest(HttpMethod.GET, "/test/my_team")
            .flatMap(HttpClientRequest::rxSend)
            .test()
            .await()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                List<String> cookieHeaders = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                assertThat(cookieHeaders).hasSize(2);
                assertThat(cookieHeaders.get(0)).isEqualTo(cookie1);
                assertThat(cookieHeaders.get(1)).isEqualTo(cookie2);
                return true;
            })
            .assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo(ENDPOINT)));
    }

    @Test
    void should_conserve_custom_header(HttpClient httpClient) throws InterruptedException {
        wiremock.stubFor(get(ENDPOINT).willReturn(ok().withHeader("custom", "foobar")));

        httpClient
            .rxRequest(HttpMethod.GET, "/test/my_team")
            .flatMap(request -> request.rxSend())
            .test()
            .await()
            .assertValue(response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().contains("custom")).isTrue();
                return true;
            })
            .assertNoErrors();

        wiremock.verify(getRequestedFor(urlPathEqualTo(ENDPOINT)));
    }
}
