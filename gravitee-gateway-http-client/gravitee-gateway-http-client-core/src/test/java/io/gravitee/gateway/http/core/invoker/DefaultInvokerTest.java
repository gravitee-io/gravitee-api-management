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
package io.gravitee.gateway.http.core.invoker;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.http.core.invoker.loadbalancer.RoundRobinLoadBalancer;
import io.gravitee.reporter.api.http.Metrics;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Collections.singleton;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultInvokerTest {

    @InjectMocks
    private RoundRobinLoadBalancer invoker;

    @Mock
    private ExecutionContext executionContext;
    @Mock
    private Request serverRequest;
    @Mock
    private ReadStream<Buffer> stream;
    @Mock
    private Handler<ProxyConnection> connectionHandler;
    @Mock
    private EndpointManager endpointManager;
    @Mock
    private Endpoint endpoint;
    @Mock
    private HttpHeaders httpHeaders;
    @Mock
    private Connector connector;
    @Mock
    private ProxyConnection proxyConnection;
    @Mock
    private Api api;
    @Mock
    private Proxy proxy;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldInvokeEndpointWithEncodedTargetURI() {
        final String targetUri = "http://host:8080/test toto  tété/titi";
        final String expectedTargetUri = "http://host:8080/test%20toto%20%20t%C3%A9t%C3%A9/titi";
        when(executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT)).thenReturn(targetUri);
        when(endpoint.available()).thenReturn(true);
        when(endpoint.target()).thenReturn("/test");
        when(endpoint.headers()).thenReturn(httpHeaders);
        when(endpoint.available()).thenReturn(true);
        when(endpoint.connector()).thenReturn(connector);
        when(connector.request(any(ProxyRequest.class))).thenReturn(proxyConnection);
        when(endpointManager.endpoints()).thenReturn(singleton(endpoint));
        final Metrics metrics = Metrics.on(System.currentTimeMillis()).build();
        when(serverRequest.metrics()).thenReturn(metrics);
        when(serverRequest.headers()).thenReturn(httpHeaders);
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getLoggingMode()).thenReturn(LoggingMode.NONE);
        when(stream.bodyHandler(any(Handler.class))).thenReturn(stream);

        invoker.invoke(executionContext, serverRequest, stream, connectionHandler);

        Assert.assertEquals(expectedTargetUri, metrics.getEndpoint());
    }
}