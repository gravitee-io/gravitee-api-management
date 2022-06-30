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

import io.gravitee.gateway.standalone.AbstractWiremockGatewayTest;
import io.gravitee.gateway.standalone.junit.annotation.ApiDescriptor;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.junit.Test;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@ApiDescriptor("/io/gravitee/gateway/standalone/http2/custom-host.json")
public class Http2CustomHostTest extends AbstractWiremockGatewayTest {

    @Test
    public void http2_custom_host() throws Exception {
        wireMockRule.stubFor(get("/team/my_team").willReturn(ok()));

        // First call is calling an endpoint where trustAll is defined to true, no need for truststore => 200
        HttpResponse response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals("unknown host => 502", HttpStatus.SC_BAD_GATEWAY, response.getStatusLine().getStatusCode());

        // Second call is calling an endpoint where trustAll is defined to false, without truststore => 502
        response = execute(Request.Get("http://localhost:8082/test/my_team")).returnResponse();
        assertEquals("custom host find the api => 200", HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        wireMockRule.verify(
            getRequestedFor(urlPathEqualTo("/team/my_team")).withHeader(HttpHeaderNames.CACHE_CONTROL.toString(), equalTo("no-cache"))
        );
    }
}
