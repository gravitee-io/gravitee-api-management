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
import static org.junit.Assert.assertNull;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Ignore("Disabled for now because there are some race condition between https and http tests when running all tests.")
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class Http2UnexpectedHttpHeadersGatewayTest extends Http2WiremockGatewayTest {

    @Test
    public void shouldNotReceiveConnectionHeader() throws Exception {
        wireMockRule.stubFor(
            get("/team/my_team").willReturn(ok().withHeader(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE))
        );

        io.vertx.core.http.HttpClient httpClient = Vertx
            .vertx()
            .createHttpClient(
                new HttpClientOptions().setSsl(true).setTrustAll(true).setUseAlpn(true).setProtocolVersion(HttpVersion.HTTP_2)
            );

        httpClient
            .request(HttpMethod.GET, "https://localhost:8082/test/my_team")
            .onComplete(
                new Handler<AsyncResult<HttpClientRequest>>() {
                    @Override
                    public void handle(AsyncResult<HttpClientRequest> event) {
                        Assert.assertTrue(event.succeeded());

                        event
                            .result()
                            .send()
                            .onComplete(
                                new Handler<AsyncResult<HttpClientResponse>>() {
                                    @Override
                                    public void handle(AsyncResult<HttpClientResponse> event) {
                                        Assert.assertTrue(event.succeeded());

                                        HttpClientResponse httpClientResponse = event.result();

                                        assertEquals(HttpStatusCode.OK_200, httpClientResponse.statusCode());
                                        assertNull(httpClientResponse.getHeader(HttpHeaderNames.CONNECTION));
                                        wireMockRule.verify(getRequestedFor(urlPathEqualTo("/team/my_team")));
                                    }
                                }
                            );
                    }
                }
            );
    }
}
