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

import static io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector.CLIENT_ABORTED_DURING_RESPONSE_ERROR;
import static io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector.CLIENT_ABORTED_DURING_RESPONSE_MESSAGE;
import static io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector.GATEWAY_CLIENT_CONNECTION_ERROR;
import static io.gravitee.plugin.endpoint.http.proxy.HttpProxyEndpointConnector.REQUEST_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.node.opentelemetry.tracer.noop.NoOpTracer;
import io.gravitee.plugin.endpoint.http.proxy.client.GrpcHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.connector.ProxyConnector;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.impl.NoStackTraceTimeoutException;
import io.vertx.rxjava3.core.http.HttpClient;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
    private HttpExecutionContext ctx;

    @Mock
    private HttpRequest request;

    @Mock
    private HttpResponse response;

    @Mock
    private Metrics metrics;

    @Mock
    private ProxyConnector proxyConnector;

    @Captor
    private ArgumentCaptor<ExecutionFailure> failureCaptor;

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
        // request.parameters() can't be null. See https://github.com/gravitee-io/gravitee-common/blob/master/src/main/java/io/gravitee/common/util/URIUtils.java#L74
        lenient().when(request.parameters()).thenReturn(new LinkedMultiValueMap<>());
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

    static Stream<Throwable> timeoutExceptionsProvider() {
        return Stream.of(
            new NoStackTraceTimeoutException("Vertx no stack trace timeout"),
            ReadTimeoutException.INSTANCE,
            new ConnectTimeoutException("Netty connect timeout")
        );
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

    @Test
    void should_set_status_to_zero_when_connect_is_called() {
        Map<String, ProxyConnector> mockConnectors = new ConcurrentHashMap<>();
        mockConnectors.put("http", proxyConnector);
        ReflectionTestUtils.setField(cut, "connectors", mockConnectors);

        cut.connect(ctx).test().assertComplete();

        // Verify status is set to 0
        verify(response).status(0);
    }

    @ParameterizedTest
    @MethodSource("timeoutExceptionsProvider")
    void shouldHandle_TimeoutExceptions_And_Interrupt(Throwable timeoutException) {
        Map<String, ProxyConnector> mockConnectors = new ConcurrentHashMap<>();
        mockConnectors.put("http", proxyConnector);
        ReflectionTestUtils.setField(cut, "connectors", mockConnectors);

        when(proxyConnector.connect(ctx)).thenReturn(Completable.error(timeoutException));
        when(ctx.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());

        TestObserver<Void> testObserver = cut.connect(ctx).test();

        verify(ctx).interruptWith(failureCaptor.capture());
        ExecutionFailure capturedFailure = failureCaptor.getValue();
        assertThat(capturedFailure.statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(capturedFailure.key()).isEqualTo(REQUEST_TIMEOUT);

        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    void shouldUse_HandleException_In_ErrorResumeNext() {
        Map<String, ProxyConnector> mockConnectors = new ConcurrentHashMap<>();
        mockConnectors.put("http", proxyConnector);
        ReflectionTestUtils.setField(cut, "connectors", mockConnectors);

        Throwable timeoutException = new NoStackTraceTimeoutException("timeout");
        when(proxyConnector.connect(ctx)).thenReturn(Completable.error(timeoutException));
        when(ctx.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        TestObserver<Void> testObserver = cut.connect(ctx).test();

        testObserver.assertComplete();
        verify(ctx).interruptWith(failureCaptor.capture());
        assertThat(failureCaptor.getValue().statusCode()).isEqualTo(HttpStatusCode.GATEWAY_TIMEOUT_504);
        assertThat(failureCaptor.getValue().key()).isEqualTo(REQUEST_TIMEOUT);
    }

    @Test
    void shouldHandle_IOException_And_Interrupt_With_BadGatewayKey() {
        Map<String, ProxyConnector> mockConnectors = new ConcurrentHashMap<>();
        mockConnectors.put("http", proxyConnector);
        ReflectionTestUtils.setField(cut, "connectors", mockConnectors);

        Throwable badGatewayException = new IOException("Bad Gateway");
        when(proxyConnector.connect(ctx)).thenReturn(Completable.error(badGatewayException));
        when(ctx.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        TestObserver<Void> testObserver = cut.connect(ctx).test();

        verify(ctx).interruptWith(failureCaptor.capture());
        ExecutionFailure capturedFailure = failureCaptor.getValue();
        assertThat(capturedFailure.statusCode()).isEqualTo(HttpStatusCode.BAD_GATEWAY_502);
        assertThat(capturedFailure.key()).isEqualTo(GATEWAY_CLIENT_CONNECTION_ERROR);

        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    void shouldPropagate_InterruptionFailureException() {
        Map<String, ProxyConnector> mockConnectors = new ConcurrentHashMap<>();
        mockConnectors.put("http", proxyConnector);
        ReflectionTestUtils.setField(cut, "connectors", mockConnectors);

        InterruptionFailureException interruptionFailureException = new InterruptionFailureException(new ExecutionFailure());
        when(proxyConnector.connect(ctx)).thenReturn(Completable.error(interruptionFailureException));

        cut.connect(ctx).test();

        verify(ctx, never()).interruptWith(any(ExecutionFailure.class));
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
            // we need to create a dedicated endpoint here as the evaluation of the configuration target
            // to detect if the URL start by grpc is done once in the constructor
            configuration.setTarget("grpc://target");
            var cut = new HttpProxyEndpointConnector(configuration, sharedConfiguration);
            injectSpyIntoEndpointConnector(cut);

            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxRequest(any())).thenThrow(new IllegalStateException());
            cut.connect(ctx).onErrorComplete(IllegalStateException.class::isInstance).test().assertComplete();

            ArgumentCaptor<HttpProxyEndpointConnectorSharedConfiguration> sharedConfigurationCaptor = ArgumentCaptor.forClass(
                HttpProxyEndpointConnectorSharedConfiguration.class
            );
            verify(spyGrpcHttpClientFactory).getOrBuildHttpClient(any(), any(), sharedConfigurationCaptor.capture());
            verify(spyHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());

            HttpProxyEndpointConnectorSharedConfiguration config = sharedConfigurationCaptor.getValue();

            // Check HTTP/2 default values.
            assertThat(config.getHttpOptions().getHttp2MultiplexingLimit()).isEqualTo(-1);
            assertThat(config.getHttpOptions().getHttp2ConnectionWindowSize()).isEqualTo(-1);
            assertThat(config.getHttpOptions().getHttp2StreamWindowSize()).isEqualTo(-1);
            assertThat(config.getHttpOptions().getHttp2MaxFrameSize()).isEqualTo(16384);
        }

        @Test
        void should_use_grpc_client_factory_with_grpc_and_customize_http2_settings() {
            // we nee to create a dedicated endpoint here as the evaluation of the configuration target
            // to detect if the URL start by grpc is done once in the constructor
            configuration.setTarget("grpc://target");
            sharedConfiguration.getHttpOptions().setHttp2MultiplexingLimit(13);
            sharedConfiguration.getHttpOptions().setHttp2ConnectionWindowSize(128000);
            sharedConfiguration.getHttpOptions().setHttp2StreamWindowSize(72000);
            sharedConfiguration.getHttpOptions().setHttp2MaxFrameSize(32000);

            var cut = new HttpProxyEndpointConnector(configuration, sharedConfiguration);
            injectSpyIntoEndpointConnector(cut);

            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxRequest(any())).thenThrow(new IllegalStateException());
            cut.connect(ctx).onErrorComplete(IllegalStateException.class::isInstance).test().assertComplete();

            ArgumentCaptor<HttpProxyEndpointConnectorSharedConfiguration> sharedConfigurationCaptor = ArgumentCaptor.forClass(
                HttpProxyEndpointConnectorSharedConfiguration.class
            );
            verify(spyGrpcHttpClientFactory).getOrBuildHttpClient(any(), any(), sharedConfigurationCaptor.capture());
            verify(spyHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());

            HttpProxyEndpointConnectorSharedConfiguration config = sharedConfigurationCaptor.getValue();

            // Check HTTP/2 values have been taken into account when creating the client.
            assertThat(config.getHttpOptions().getHttp2MultiplexingLimit()).isEqualTo(13);
            assertThat(config.getHttpOptions().getHttp2ConnectionWindowSize()).isEqualTo(128000);
            assertThat(config.getHttpOptions().getHttp2StreamWindowSize()).isEqualTo(72000);
            assertThat(config.getHttpOptions().getHttp2MaxFrameSize()).isEqualTo(32000);
        }

        @Test
        void should_use_http_client_factory_with_ws() {
            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxWebSocket(any(WebSocketConnectOptions.class))).thenThrow(new IllegalStateException());
            when(request.isWebSocket()).thenReturn(true);

            // connect will throw an exception
            cut
                .connect(ctx)
                .onErrorComplete(throwable -> throwable instanceof IllegalStateException)
                .test()
                .assertComplete();
            verify(spyHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            verify(spyGrpcHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());
            verify(request).isWebSocket();
        }

        @Test
        void should_use_http_client_factory() {
            // We don't want to test the request itself just that the correct factory is used
            when(mockHttpClient.rxRequest(any())).thenThrow(new IllegalStateException());
            cut
                .connect(ctx)
                .onErrorComplete(throwable -> throwable instanceof IllegalStateException)
                .test()
                .assertComplete();
            verify(spyHttpClientFactory).getOrBuildHttpClient(any(), any(), any());
            verify(spyGrpcHttpClientFactory, never()).getOrBuildHttpClient(any(), any(), any());
        }
    }
}
