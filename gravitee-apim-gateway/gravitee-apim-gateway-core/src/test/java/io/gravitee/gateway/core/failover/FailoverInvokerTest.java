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
package io.gravitee.gateway.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.invoker.V3InvokerAttributes;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.Vertx;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailoverInvokerTest {

    @Mock
    private EndpointResolver endpointResolver;

    @Mock(extraInterfaces = MutableExecutionContext.class)
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock(answer = Answers.RETURNS_SELF)
    private ReadStream<Buffer> stream;

    @Mock
    private Handler<ProxyConnection> connectionHandler;

    @Mock
    private ProxyEndpoint proxyEndpoint;

    @Mock
    private ProxyRequest proxyRequest;

    @Mock
    private Connector connector;

    @Mock
    private ProxyConnection upstreamConnection;

    @Mock
    private HttpHeaders headers;

    private Vertx vertx;
    private FailoverInvoker cut;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
        cut = new FailoverInvoker(vertx, endpointResolver, new FailoverOptions().setMaxAttempts(3).setRetryTimeout(30000L));
        lenient().when(context.request()).thenReturn(request);
        lenient().when(request.method()).thenReturn(HttpMethod.GET);
        lenient().when(request.metrics()).thenReturn(Metrics.on(0).build());
        lenient().when(endpointResolver.resolve(any())).thenReturn(proxyEndpoint);
        lenient().when(proxyEndpoint.available()).thenReturn(true);
        lenient().when(proxyEndpoint.createProxyRequest(any(Request.class), any(Function.class))).thenReturn(proxyRequest);
        lenient().when(proxyEndpoint.connector()).thenReturn(connector);
        lenient().when(proxyRequest.headers()).thenReturn(headers);
    }

    @AfterEach
    void tearDown() {
        vertx.close();
    }

    @Test
    void shouldCancelAttemptInFlightAndStopRetryingWhenCancelUpstreamIsRun() {
        // The upstream accepts the connection but never answers: the attempt stays in flight.
        doAnswer(invocation -> {
            invocation.<Handler<ProxyConnection>>getArgument(2).handle(upstreamConnection);
            return null;
        })
            .when(connector)
            .request(eq(proxyRequest), eq(context), any());

        cut.invoke(context, stream, connectionHandler);

        // The circuit breaker starts the attempt asynchronously.
        verify(connector, timeout(5000)).request(eq(proxyRequest), eq(context), any());
        verify(upstreamConnection, timeout(5000)).responseHandler(any());

        final ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
        verify(context).setAttribute(eq(V3InvokerAttributes.ATTR_INTERNAL_V3_CANCEL_UPSTREAM), published.capture());
        assertThat(published.getValue()).isInstanceOf(Runnable.class);

        ((Runnable) published.getValue()).run();

        verify(upstreamConnection).cancel();
        // The exchange is completed with a bad gateway so that the caller can end it, and no new attempt is made.
        final ArgumentCaptor<ProxyConnection> connection = ArgumentCaptor.forClass(ProxyConnection.class);
        verify(connectionHandler, timeout(5000)).handle(connection.capture());
        final ProxyConnection failoverConnection = connection.getValue();
        assertThat(failoverConnection).isNotSameAs(upstreamConnection);
        verify(connector, times(1)).request(eq(proxyRequest), eq(context), any());
    }
}
