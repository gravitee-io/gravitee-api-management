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
package io.gravitee.gateway.standalone.http2;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class Http2HeadersTest extends AbstractWiremockGatewayTest {

    io.vertx.core.http.HttpClient httpClient = Vertx
        .vertx()
        .createHttpClient(new HttpClientOptions().setSsl(true).setTrustAll(true).setUseAlpn(true).setProtocolVersion(HttpVersion.HTTP_2));

    @Test
    public void should_conserve_multi_values() {
        String cookie1 = "JSESSIONID=ABSCDEDASDSSDSSE.oai007; path=/; Secure; HttpOnly";
        String cookie2 = "JSESSIONID=BASCDEDASDSSDSSE.oai008; path=/another; Secure; HttpOnly";

        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team"))
                .willReturn(ok().withHeader(HttpHeaderNames.SET_COOKIE, cookie1).withHeader(HttpHeaderNames.SET_COOKIE, cookie2))
        );

        httpClient
            .request(HttpMethod.GET, "https://localhost:8082/test/my_team")
            .onComplete(
                event -> {
                    Assert.assertTrue(event.succeeded());
                    event
                        .result()
                        .send()
                        .onComplete(
                            responseEvent -> {
                                Assert.assertTrue(responseEvent.succeeded());

                                HttpClientResponse response = responseEvent.result();

                                assertEquals(HttpStatus.SC_OK, response.statusCode());
                                wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));

                                List<String> cookieHeaders = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                                assertEquals(2, cookieHeaders.size());
                                assertEquals(cookie1, cookieHeaders.get(0));
                                assertEquals(cookie2, cookieHeaders.get(1));
                            }
                        );
                }
            );
    }

    @Test
    public void should_conserve_custom_header() {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok().withHeader("custom", "foobar")));

        httpClient
            .request(HttpMethod.GET, "https://localhost:8082/test/my_team")
            .onComplete(
                event -> {
                    Assert.assertTrue(event.succeeded());
                    event
                        .result()
                        .send()
                        .onComplete(
                            responseEvent -> {
                                Assert.assertTrue(responseEvent.succeeded());

                                HttpClientResponse response = responseEvent.result();

                                assertEquals(HttpStatus.SC_OK, response.statusCode());
                                wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));

                                List<String> customHeaders = response.headers().getAll("custom");
                                assertEquals(1, customHeaders.size());
                            }
                        );
                }
            );
    }
}
