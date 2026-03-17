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
package io.gravitee.gateway.core.invoker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.reporter.api.http.Metrics;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EndpointInvokerTest {

    @Mock
    private EndpointResolver endpointResolver;

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private ReadStream<Buffer> stream;

    @Mock
    private Handler<ProxyConnection> connectionHandler;

    @Mock
    private ProxyEndpoint proxyEndpoint;

    @Mock
    private ProxyRequest proxyRequest;

    @Mock
    private HttpHeaders proxyRequestHeaders;

    @Mock
    private Connector connector;

    @Mock
    private Metrics requestMetrics;

    private EndpointInvoker cut;

    @BeforeEach
    void setUp() {
        cut = new EndpointInvoker(endpointResolver);
        lenient().when(context.request()).thenReturn(request);
        lenient().when(request.method()).thenReturn(HttpMethod.PUT);
        lenient().when(request.metrics()).thenReturn(requestMetrics);
    }

    @Test
    void shouldAddTransferEncodingChunkedForHttp2RequestWithoutContentLength() {
        when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        when(proxyEndpoint.available()).thenReturn(true);
        when(proxyEndpoint.createProxyRequest(eq(request), any(Function.class))).thenReturn(proxyRequest);
        when(proxyEndpoint.connector()).thenReturn(connector);
        when(proxyRequest.headers()).thenReturn(proxyRequestHeaders);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);
        when(proxyRequestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn(null);
        when(proxyRequestHeaders.get(HttpHeaderNames.TRANSFER_ENCODING)).thenReturn(null);

        cut.invoke(context, stream, connectionHandler);

        verify(proxyRequestHeaders).set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    }

    @Test
    void shouldNotAddTransferEncodingForHttp2RequestWithContentLength() {
        when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        when(proxyEndpoint.available()).thenReturn(true);
        when(proxyEndpoint.createProxyRequest(eq(request), any(Function.class))).thenReturn(proxyRequest);
        when(proxyEndpoint.connector()).thenReturn(connector);
        when(proxyRequest.headers()).thenReturn(proxyRequestHeaders);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);
        when(proxyRequestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn("42");

        cut.invoke(context, stream, connectionHandler);

        verify(proxyRequestHeaders, never()).set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    }

    @Test
    void shouldNotAddTransferEncodingForHttp1Request() {
        when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        when(proxyEndpoint.available()).thenReturn(true);
        when(proxyEndpoint.createProxyRequest(eq(request), any(Function.class))).thenReturn(proxyRequest);
        when(proxyEndpoint.connector()).thenReturn(connector);
        lenient().when(proxyRequest.headers()).thenReturn(proxyRequestHeaders);
        when(request.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut.invoke(context, stream, connectionHandler);

        verify(proxyRequestHeaders, never()).set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    }

    @Test
    void shouldNotAddTransferEncodingWhenAlreadyChunked() {
        when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        when(proxyEndpoint.available()).thenReturn(true);
        when(proxyEndpoint.createProxyRequest(eq(request), any(Function.class))).thenReturn(proxyRequest);
        when(proxyEndpoint.connector()).thenReturn(connector);
        when(proxyRequest.headers()).thenReturn(proxyRequestHeaders);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);
        when(proxyRequestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn(null);
        when(proxyRequestHeaders.get(HttpHeaderNames.TRANSFER_ENCODING)).thenReturn("chunked");

        cut.invoke(context, stream, connectionHandler);

        verify(proxyRequestHeaders, never()).set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    }

    @Test
    void shouldNotAddTransferEncodingForGrpcRequest() {
        when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        when(proxyEndpoint.available()).thenReturn(true);
        when(proxyEndpoint.createProxyRequest(eq(request), any(Function.class))).thenReturn(proxyRequest);
        when(proxyEndpoint.connector()).thenReturn(connector);
        when(proxyRequest.headers()).thenReturn(proxyRequestHeaders);
        when(request.version()).thenReturn(HttpVersion.HTTP_2);
        when(proxyRequestHeaders.get(HttpHeaderNames.CONTENT_LENGTH)).thenReturn(null);
        when(proxyRequestHeaders.get(HttpHeaderNames.TRANSFER_ENCODING)).thenReturn(null);
        when(proxyRequestHeaders.get(HttpHeaderNames.CONTENT_TYPE)).thenReturn("application/grpc");

        cut.invoke(context, stream, connectionHandler);

        verify(proxyRequestHeaders, never()).set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
    }
}
