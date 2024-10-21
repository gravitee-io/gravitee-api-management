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
package io.gravitee.plugin.endpoint.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.rxjava3.core.http.HttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HttpProxyEndpointConnectorTest {

    @Mock
    private DeploymentContext deploymentCtx;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Metrics metrics;

    @Mock
    private ProxyConnector proxyConnector;

    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private HttpProxyEndpointConnectorConfiguration configuration;
    private HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    private HttpProxyEndpointConnector cut;

    @BeforeEach
    void init() {
        lenient().when(proxyConnector.connect(ctx)).thenReturn(Completable.complete());

        lenient().when(deploymentCtx.getTemplateEngine()).thenReturn(templateEngine);

        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(ctx.getTracer()).thenReturn(new Tracer(null, new NoOpTracer()));

        requestHeaders = HttpHeaders.create();
        lenient().when(request.pathInfo()).thenReturn("");
        lenient().when(request.headers()).thenReturn(requestHeaders);
        lenient().when(request.chunks()).thenReturn(Flowable.empty());

        responseHeaders = HttpHeaders.create();
        lenient().when(response.headers()).thenReturn(responseHeaders);

        configuration = new HttpProxyEndpointConnectorConfiguration();
        sharedConfiguration = new HttpProxyEndpointConnectorSharedConfiguration();

        configuration.setTarget("http://localhost:8080/team");
        cut = new HttpProxyEndpointConnector(configuration, sharedConfiguration);
    }

    @Test
    void should_support_sync_api() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.PROXY);
    }

    @Test
    void should_support_request_response_modes() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.REQUEST_RESPONSE);
    }

    @Test
    void should_return_http_proxy_id() {
        assertThat(cut.id()).isEqualTo("http-proxy");
    }

    @Nested
    class ConnectTest {

        private HttpClientFactory spyHttpClientFactory;
        private GrpcHttpClientFactory spyGrpcHttpClientFactory;

        @Mock
        private HttpClient mockHttpClient;

        @BeforeEach
        public void init() {
            when(request.method()).thenReturn(HttpMethod.GET);
            injectSpyIntoEndpointConnector(cut);
        }

        private void injectSpyIntoEndpointConnector(HttpProxyEndpointConnector cut) {
            spyHttpClientFactory = spy((HttpClientFactory) ReflectionTestUtils.getField(cut, "httpClientFactory"));
            lenient().doReturn(mockHttpClient).when(spyHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            ReflectionTestUtils.setField(cut, "httpClientFactory", spyHttpClientFactory);
            spyGrpcHttpClientFactory = spy((GrpcHttpClientFactory) ReflectionTestUtils.getField(cut, "grpcHttpClientFactory"));
            lenient().doReturn(mockHttpClient).when(spyGrpcHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            ReflectionTestUtils.setField(cut, "grpcHttpClientFactory", spyGrpcHttpClientFactory);
        }

        @Test
        void should_use_grpc_client_factory_with_grpc() {
            // we nee to create a dedicated endpoint here as the evaluation of the configuration target
            // to detect if the URL start by grpc is done once in the constructor
            configuration.setTarget("grpc://target");
            var cut = new HttpProxyEndpointConnector(configuration, sharedConfiguration);
            injectSpyIntoEndpointConnector(cut);

            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxRequest(any())).thenThrow(new IllegalStateException());
            cut.connect(ctx).onErrorComplete(throwable -> throwable instanceof IllegalStateException).test().assertComplete();
            verify(spyGrpcHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            verify(spyHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());
        }

        @Test
        void should_use_http_client_factory_with_ws() {
            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxWebSocket(any(WebSocketConnectOptions.class))).thenThrow(new IllegalStateException());
            when(request.isWebSocket()).thenReturn(true);

            // connect will throw an exception
            cut.connect(ctx).onErrorComplete(throwable -> throwable instanceof IllegalStateException).test().assertComplete();
            verify(spyHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            verify(spyGrpcHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());
            verify(request).isWebSocket();
        }

        @Test
        void should_use_http_client_factory() {
            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxRequest(any())).thenThrow(new IllegalStateException());
            cut.connect(ctx).onErrorComplete(throwable -> throwable instanceof IllegalStateException).test().assertComplete();
            verify(spyHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            verify(spyGrpcHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());
        }
    }
}
