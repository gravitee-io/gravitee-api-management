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
package io.gravitee.gateway.jupiter.reactor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.withSettings;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.processor.provider.ProcessorProviderChain;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.HttpRequestTimeoutConfiguration;
import io.gravitee.gateway.http.vertx.TimeoutServerResponse;
import io.gravitee.gateway.jupiter.core.context.MutableRequestExecutionContext;
import io.gravitee.gateway.jupiter.core.processor.ProcessorChain;
import io.gravitee.gateway.jupiter.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.jupiter.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.jupiter.reactor.processor.PlatformProcessorChainFactory;
import io.gravitee.gateway.reactor.handler.HttpAcceptorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.report.ReporterService;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultHttpRequestDispatcherTest {

    protected static final String HOST = "gravitee.io";
    protected static final String PATH = "/path";
    protected static final String MOCK_ERROR_MESSAGE = "Mock error";
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private HttpAcceptorResolver httpAcceptorResolver;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private RequestProcessorChainFactory requestProcessorChainFactory;

    @Mock
    private ResponseProcessorChainFactory responseProcessorChainFactory;

    @Mock
    private PlatformProcessorChainFactory platformProcessorChainFactory;

    @Mock
    private NotFoundProcessorChainFactory notFoundProcessorChainFactory;

    @Mock
    private HttpServerRequest rxRequest;

    @Mock
    private HttpServerResponse rxResponse;

    @Mock
    private io.vertx.core.http.HttpServerRequest request;

    @Mock
    private io.vertx.core.http.HttpServerResponse response;

    @Mock
    private HttpAcceptorHandler handlerEntrypoint;

    @Mock
    private Environment environment;

    @Mock
    private ReporterService reporterService;

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private HttpRequestTimeoutConfiguration httpRequestTimeoutConfiguration;

    private DefaultHttpRequestDispatcher cut;

    @BeforeEach
    public void init() {
        // Mock vertx request behavior.
        lenient().when(rxRequest.host()).thenReturn(HOST);
        lenient().when(rxRequest.path()).thenReturn(PATH);
        lenient().when(rxRequest.version()).thenReturn(HttpVersion.HTTP_2);
        lenient().when(rxRequest.method()).thenReturn(HttpMethod.GET);
        lenient().when(rxRequest.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxRequest.toFlowable()).thenReturn(Flowable.empty());
        lenient().when(rxRequest.response()).thenReturn(rxResponse);
        lenient().when(rxRequest.getDelegate()).thenReturn(request);

        lenient().when(request.host()).thenReturn(HOST);
        lenient().when(request.path()).thenReturn(PATH);
        lenient().when(request.method()).thenReturn(HttpMethod.GET);
        lenient().when(request.headers()).thenReturn(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        lenient().when(request.response()).thenReturn(response);

        // Mock vertx response behavior.
        lenient().when(rxResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxResponse.trailers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxResponse.getDelegate()).thenReturn(response);

        lenient().when(response.headers()).thenReturn(io.vertx.core.MultiMap.caseInsensitiveMultiMap());
        lenient().when(response.trailers()).thenReturn(io.vertx.core.MultiMap.caseInsensitiveMultiMap());

        lenient().when(requestProcessorChainFactory.create()).thenReturn(new ProcessorProviderChain<>(List.of()));
        lenient().when(responseProcessorChainFactory.create()).thenReturn(new ProcessorProviderChain<>(List.of()));
        lenient().when(platformProcessorChainFactory.preProcessorChain()).thenReturn(new ProcessorChain("pre", List.of()));
        lenient().when(platformProcessorChainFactory.postProcessorChain()).thenReturn(new ProcessorChain("post", List.of()));
        lenient().when(httpRequestTimeoutConfiguration.getHttpRequestTimeout()).thenReturn(0L);
        lenient().when(httpRequestTimeoutConfiguration.getHttpRequestTimeoutGraceDelay()).thenReturn(10L);

        cut =
            new DefaultHttpRequestDispatcher(
                gatewayConfiguration,
                httpAcceptorResolver,
                idGenerator,
                globalComponentProvider,
                new RequestProcessorChainFactory(),
                responseProcessorChainFactory,
                platformProcessorChainFactory,
                notFoundProcessorChainFactory,
                false,
                httpRequestTimeoutConfiguration,
                vertx
            );
        //TODO: to check: is this needed ?
        // cut.setApplicationContext(mock(ApplicationContext.class));
    }

    @Test
    void shouldHandleJupiterRequest() {
        final ApiReactor apiReactor = mock(ApiReactor.class, withSettings().extraInterfaces(ReactorHandler.class));

        this.prepareJupiterMock(handlerEntrypoint, apiReactor);

        when(apiReactor.handle(any(MutableRequestExecutionContext.class))).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertResult();
    }

    @Test
    void shouldSetMetricsWhenHandlingJupiterRequest() {
        final ApiReactor apiReactor = mock(ApiReactor.class, withSettings().extraInterfaces(ReactorHandler.class));
        final ArgumentCaptor<MutableRequestExecutionContext> ctxCaptor = ArgumentCaptor.forClass(MutableRequestExecutionContext.class);

        this.prepareJupiterMock(handlerEntrypoint, apiReactor);

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
        when(apiReactor.handle(ctxCaptor.capture())).thenReturn(Completable.complete());
        cut.dispatch(rxRequest).test().assertResult();

        final MutableRequestExecutionContext ctxCaptorValue = ctxCaptor.getValue();
        assertThat(ctxCaptorValue.request().metrics().getTenant()).isEqualTo("TENANT");
        assertThat(ctxCaptorValue.request().metrics().getZone()).isEqualTo("ZONE");
    }

    @Test
    void shouldPropagateErrorWhenErrorWithJupiterRequest() {
        final ApiReactor apiReactor = mock(ApiReactor.class, withSettings().extraInterfaces(ReactorHandler.class));

        this.prepareJupiterMock(handlerEntrypoint, apiReactor);

        when(apiReactor.handle(any(MutableRequestExecutionContext.class)))
            .thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertErrorMessage(MOCK_ERROR_MESSAGE);
    }

    @Test
    void shouldHandleV3Request() {
        final ReactorHandler apiReactor = mock(ReactorHandler.class);

        this.prepareV3Mock(handlerEntrypoint, apiReactor);
        when(response.ended()).thenReturn(true);

        doAnswer(
                i -> {
                    simulateEndHandlerCall(i);
                    return null;
                }
            )
            .when(apiReactor)
            .handle(any(ExecutionContext.class), any(Handler.class));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertResult();
    }

    @Test
    void shouldHandleV3RequestWithTimeout() {
        when(httpRequestTimeoutConfiguration.getHttpRequestTimeout()).thenReturn(50L);

        final ReactorHandler apiReactor = mock(ReactorHandler.class);

        this.prepareV3Mock(handlerEntrypoint, apiReactor);
        when(response.ended()).thenReturn(true);
        final ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);

        doAnswer(
                i -> {
                    simulateEndHandlerCall(i);
                    return null;
                }
            )
            .when(apiReactor)
            .handle(ctxCaptor.capture(), any(Handler.class));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        final ExecutionContext ctxCaptorValue = ctxCaptor.getValue();
        assertThat(ctxCaptorValue.response()).isInstanceOf(TimeoutServerResponse.class);

        obs.assertResult();
    }

    @Test
    void shouldSetMetricsWhenHandlingV3Request() {
        final ReactorHandler apiReactor = mock(ReactorHandler.class);

        this.prepareV3Mock(handlerEntrypoint, apiReactor);
        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
        when(response.ended()).thenReturn(true);

        doAnswer(
                i -> {
                    final ExecutionContext ctx = i.getArgument(0, ExecutionContext.class);

                    assertEquals("TENANT", ctx.request().metrics().getTenant());
                    assertEquals("ZONE", ctx.request().metrics().getZone());
                    simulateEndHandlerCall(i);
                    return null;
                }
            )
            .when(apiReactor)
            .handle(any(ExecutionContext.class), any(Handler.class));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertResult();
    }

    @Test
    void shouldEndResponseWhenNotAlreadyEndedByV3Handler() {
        final ReactorHandler apiReactor = mock(ReactorHandler.class);

        this.prepareV3Mock(handlerEntrypoint, apiReactor);
        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
        when(response.ended()).thenReturn(false);
        when(rxResponse.rxEnd()).thenReturn(Completable.complete());

        doAnswer(
                i -> {
                    final ExecutionContext ctx = i.getArgument(0, ExecutionContext.class);

                    assertEquals("TENANT", ctx.request().metrics().getTenant());
                    assertEquals("ZONE", ctx.request().metrics().getZone());
                    simulateEndHandlerCall(i);
                    return null;
                }
            )
            .when(apiReactor)
            .handle(any(ExecutionContext.class), any(Handler.class));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertResult();
    }

    @Test
    void shouldPropagateErrorWhenExceptionWithV3Request() {
        final ReactorHandler apiReactor = mock(ReactorHandler.class);

        this.prepareV3Mock(handlerEntrypoint, apiReactor);

        doThrow(new RuntimeException(MOCK_ERROR_MESSAGE)).when(apiReactor).handle(any(ExecutionContext.class), any(Handler.class));

        final TestObserver<Void> obs = cut.dispatch(rxRequest).test();

        obs.assertErrorMessage(MOCK_ERROR_MESSAGE);
    }

    @Test
    void shouldHandleNotFoundWhenNoHandlerResolved() {
        ProcessorChain processorChain = spy(new ProcessorChain("id", List.of()));
        when(notFoundProcessorChainFactory.processorChain()).thenReturn(processorChain);
        when(httpAcceptorResolver.resolve(HOST, PATH)).thenReturn(null);

        cut.dispatch(rxRequest).test().assertResult();

        verify(notFoundProcessorChainFactory).processorChain();
        verify(processorChain).execute(any(), any());
    }

    @Test
    void shouldHandleNotFoundWhenNoTargetOnResolvedHandler() {
        this.prepareV3Mock(handlerEntrypoint, null);

        ProcessorChain processorChain = spy(new ProcessorChain("id", List.of()));
        when(notFoundProcessorChainFactory.processorChain()).thenReturn(processorChain);
        when(httpAcceptorResolver.resolve(HOST, PATH)).thenReturn(null);
        cut.dispatch(rxRequest).test().assertResult();

        verify(notFoundProcessorChainFactory).processorChain();
        verify(processorChain).execute(any(), any());
    }

    @Test
    void shouldSetMetricsWhenHandlingNotFoundRequest() {
        ProcessorChain processorChain = spy(new ProcessorChain("id", List.of()));
        when(notFoundProcessorChainFactory.processorChain()).thenReturn(processorChain);
        when(httpAcceptorResolver.resolve(HOST, PATH)).thenReturn(null);

        final ArgumentCaptor<MutableRequestExecutionContext> ctxCaptor = ArgumentCaptor.forClass(MutableRequestExecutionContext.class);

        when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
        when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
        when(processorChain.execute(ctxCaptor.capture(), any())).thenCallRealMethod();
        cut.dispatch(rxRequest).test().assertResult();

        final MutableRequestExecutionContext ctxCaptorValue = ctxCaptor.getValue();
        assertThat(ctxCaptorValue.request().metrics().getTenant()).isEqualTo("TENANT");
        assertThat(ctxCaptorValue.request().metrics().getZone()).isEqualTo("ZONE");
    }

    private void prepareJupiterMock(HttpAcceptorHandler handlerEntrypoint, ApiReactor apiReactor) {
        when(httpAcceptorResolver.resolve(HOST, PATH)).thenReturn(handlerEntrypoint);
        when(handlerEntrypoint.executionMode()).thenReturn(ExecutionMode.JUPITER);
        when(handlerEntrypoint.path()).thenReturn(PATH);
        when(handlerEntrypoint.target()).thenReturn(apiReactor);
    }

    private void prepareV3Mock(HttpAcceptorHandler handlerEntrypoint, ReactorHandler apiReactor) {
        when(httpAcceptorResolver.resolve(HOST, PATH)).thenReturn(handlerEntrypoint);

        if (apiReactor != null) {
            when(handlerEntrypoint.executionMode()).thenReturn(ExecutionMode.V3);
            when(handlerEntrypoint.target()).thenReturn(apiReactor);
        }
    }

    private void simulateEndHandlerCall(InvocationOnMock i) {
        final ExecutionContext ctx = i.getArgument(0);
        final Handler<ExecutionContext> endHandler = i.getArgument(1, Handler.class);
        endHandler.handle(ctx);
    }
}
