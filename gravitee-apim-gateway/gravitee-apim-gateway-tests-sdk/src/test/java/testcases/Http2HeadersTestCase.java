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

import io.gravitee.apim.gateway.tests.sdk.AbstractHttp2GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.reactivex.observers.TestObserver;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi("/apis/teams.json")
public class Http2HeadersTestCase extends AbstractHttp2GatewayTest {

    public static final String ENDPOINT = "/team/my_team";

    @Test
    void should_conserve_multi_values(WebClient webClient) {
        String cookie1 = "JSESSIONID=ABSCDEDASDSSDSSE.oai007; path=/; Secure; HttpOnly";
        String cookie2 = "JSESSIONID=BASCDEDASDSSDSSE.oai008; path=/another; Secure; HttpOnly";

        wiremock.stubFor(
            get(ENDPOINT).willReturn(ok().withHeader(HttpHeaderNames.SET_COOKIE, cookie1).withHeader(HttpHeaderNames.SET_COOKIE, cookie2))
        );

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test/my_team").rxSend().test();

        awaitTerminalEvent(obs);
        obs.assertComplete();
        obs.assertValue(
            response -> {
                assertThat(response.statusCode()).isEqualTo(200);

                List<String> cookieHeaders = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                assertThat(cookieHeaders).hasSize(2);
                assertThat(cookieHeaders.get(0)).isEqualTo(cookie1);
                assertThat(cookieHeaders.get(1)).isEqualTo(cookie2);

                return true;
            }
        );
        obs.assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo(ENDPOINT)));
    }

    @Test
    void should_conserve_custom_header(WebClient webClient) {
        wiremock.stubFor(get(ENDPOINT).willReturn(ok().withHeader("custom", "foobar")));

        final TestObserver<HttpResponse<Buffer>> obs = webClient.get("/test/my_team").rxSend().test();

        awaitTerminalEvent(obs);
        obs.assertComplete();
        obs.assertValue(
            response -> {
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().contains("custom")).isTrue();

                return true;
            }
        );
        obs.assertNoErrors();
        wiremock.verify(getRequestedFor(urlPathEqualTo(ENDPOINT)));
    }
}
