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
package io.gravite.gateway.http.connector.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.HttpClientOptions;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.gravitee.gateway.http.connector.http.HttpProxyConnection;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpProxyConnectionTest {

    @Mock
    private ProxyRequest proxyRequest;

    @Mock
    private HttpEndpoint endpoint;

    @Mock
    private Handler<ProxyResponse> responseHandler;

    @Mock
    private Handler<AbstractHttpProxyConnection> connectionHandler;

    @Mock
    private HttpClient httpClient;

    private HttpProxyConnection httpProxyConnection;

    @Before
    public void setupMockedProxyConnection() {
        when(proxyRequest.headers()).thenReturn(new HttpHeaders());
        when(proxyRequest.method()).thenReturn(HttpMethod.CONNECT);
        when(proxyRequest.metrics()).thenReturn(Metrics.on(123L).build());
        when(endpoint.getHttpClientOptions()).thenReturn(new HttpClientOptions());
        httpProxyConnection = new HttpProxyConnection(endpoint, proxyRequest);
        httpProxyConnection.responseHandler(responseHandler);
    }

    @Test
    public void connection_handler_is_triggered_with_connection_on_connection_success() {
        when(httpClient.request(any())).thenReturn(Future.succeededFuture(mock(HttpClientRequest.class)));

        httpProxyConnection.connect(httpClient, 80, "test.hostname", "https://test", connectionHandler, mock(Handler.class));

        verify(connectionHandler, times(1)).handle(httpProxyConnection);
    }

    @Test
    public void connection_handler_is_triggered_with_null_on_connection_failure() {
        when(httpClient.request(any())).thenReturn(Future.failedFuture("test failed connection"));

        httpProxyConnection.connect(httpClient, 80, "test.hostname", "https://test", connectionHandler, mock(Handler.class));

        verify(connectionHandler, times(1)).handle(null);
    }
}
