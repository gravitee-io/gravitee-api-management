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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertEquals;

import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http/teams.json")
public class HttpHeadersTest extends AbstractWiremockGatewayTest {

    @Test
    public void should_conserve_multi_values() throws Exception {
        String cookie1 = "JSESSIONID=ABSCDEDASDSSDSSE.oai007; path=/; Secure; HttpOnly";
        String cookie2 = "JSESSIONID=BASCDEDASDSSDSSE.oai008; path=/another; Secure; HttpOnly";

        wireMockRule.stubFor(
            get(urlPathEqualTo("/team/my_team"))
                .willReturn(ok().withHeader(HttpHeaderNames.SET_COOKIE, cookie1).withHeader(HttpHeaderNames.SET_COOKIE, cookie2))
        );

        URI target = new URIBuilder("http://localhost:8082/test/my_team").build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));

        List<String> cookieHeaders = Arrays
            .stream(response.getAllHeaders())
            .filter(header -> header.getName().equals(HttpHeaderNames.SET_COOKIE))
            .map(h -> h.getValue())
            .collect(Collectors.toList());

        assertEquals(2, cookieHeaders.size());
        assertEquals(cookie1, cookieHeaders.get(0));
        assertEquals(cookie2, cookieHeaders.get(1));
    }

    @Test
    public void should_conserve_custom_header() throws Exception {
        wireMockRule.stubFor(get(urlPathEqualTo("/team/my_team")).willReturn(ok().withHeader("custom", "foobar")));

        URI target = new URIBuilder("http://localhost:8082/test/my_team").build();

        HttpResponse response = execute(Request.Get(target)).returnResponse();

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        wireMockRule.verify(1, getRequestedFor(urlPathEqualTo("/team/my_team")));

        List<String> customHeaders = Arrays
            .stream(response.getAllHeaders())
            .filter(header -> header.getName().equals("custom"))
            .map(h -> h.getValue())
            .collect(Collectors.toList());

        assertEquals(1, customHeaders.size());
    }
}
