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
package io.gravitee.gateway.handlers.api.processor.cors;

import static io.gravitee.common.http.HttpHeaders.ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK;
import static io.gravitee.common.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static io.gravitee.common.http.HttpHeaders.ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK;
import static io.gravitee.common.http.HttpHeaders.ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Cors;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.context.SimpleExecutionContext;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.reporter.api.http.Metrics;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorsPreflightRequestProcessorTest {

    private CorsPreflightRequestProcessor processor;
    private Cors cors;
    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        cors = new Cors();
        cors.setEnabled(true);
        cors.setAccessControlAllowOrigin(Set.of("*"));
        cors.setAccessControlAllowMethods(Set.of("GET"));
        cors.setAccessControlAllowHeaders(Set.of());
        cors.setRunPolicies(false);

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        requestHeaders = HttpHeaders.create();
        responseHeaders = HttpHeaders.create();

        when(request.method()).thenReturn(HttpMethod.OPTIONS);
        when(request.headers()).thenReturn(requestHeaders);
        when(request.metrics()).thenReturn(mock(Metrics.class));
        when(response.headers()).thenReturn(responseHeaders);

        context = new SimpleExecutionContext(request, response);

        processor = new CorsPreflightRequestProcessor(cors);
        processor.handler(c -> {});
        processor.exitHandler(v -> {});
    }

    @Test
    void shouldSetAllowPrivateNetworkHeaderWhenEnabledAndRequestHasPnaHeader() {
        cors.setAllowPrivateNetwork(true);
        requestHeaders.set(ORIGIN, "origin");
        requestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        requestHeaders.set(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");

        processor.handle(context);

        assertThat(responseHeaders.getFirst(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isEqualTo("true");
    }

    @Test
    void shouldNotSetAllowPrivateNetworkHeaderWhenEnabledButRequestMissingPnaHeader() {
        cors.setAllowPrivateNetwork(true);
        requestHeaders.set(ORIGIN, "origin");
        requestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");

        processor.handle(context);

        assertThat(responseHeaders.getFirst(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isNull();
    }

    @Test
    void shouldNotSetAllowPrivateNetworkHeaderWhenDisabledAndRequestHasPnaHeader() {
        cors.setAllowPrivateNetwork(false);
        requestHeaders.set(ORIGIN, "origin");
        requestHeaders.set(ACCESS_CONTROL_REQUEST_METHOD, "GET");
        requestHeaders.set(ACCESS_CONTROL_REQUEST_PRIVATE_NETWORK, "true");

        processor.handle(context);

        assertThat(responseHeaders.getFirst(ACCESS_CONTROL_ALLOW_PRIVATE_NETWORK)).isNull();
    }
}
