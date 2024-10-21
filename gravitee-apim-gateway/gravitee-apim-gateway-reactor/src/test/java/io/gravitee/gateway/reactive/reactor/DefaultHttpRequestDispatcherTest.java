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
package io.gravitee.gateway.reactive.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.processor.provider.ProcessorProviderChain;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.opentelemetry.TracingContext;
import io.gravitee.gateway.reactive.core.context.MutableExecutionContext;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.DefaultPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.opentelemetry.OpenTelemetryFactory;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.MultiMap;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
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
    public static final String SERVER_ID = null;

    @Spy
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private Node node;

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
    private DefaultPlatformProcessorChainFactory platformProcessorChainFactory;

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
    private HttpAcceptor handlerEntrypoint;

    @Mock
    private Environment environment;

    @Mock
    private ReporterService reporterService;

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private RequestTimeoutConfiguration requestTimeoutConfiguration;

    @Mock
    private OpenTelemetryFactory openTelemetryFactory;

    @Mock
    private RequestClientAuthConfiguration requestClientAuthConfiguration;

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
        lenient().when(response.end()).thenReturn(Future.succeededFuture());

        lenient().when(requestProcessorChainFactory.create()).thenReturn(new ProcessorProviderChain<>(List.of()));
        lenient().when(responseProcessorChainFactory.create()).thenReturn(new ProcessorProviderChain<>(List.of()));
        lenient().when(platformProcessorChainFactory.preProcessorChain()).thenReturn(new ProcessorChain("pre", List.of()));
        lenient().when(platformProcessorChainFactory.postProcessorChain()).thenReturn(new ProcessorChain("post", List.of()));
        lenient().when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(0L);
        lenient().when(requestTimeoutConfiguration.getRequestTimeoutGraceDelay()).thenReturn(10L);

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
                TracingContext.noop(),
                requestTimeoutConfiguration,
                requestClientAuthConfiguration,
                vertx
            );
        //TODO: to check: is this needed ?
        // cut.setApplicationContext(mock(ApplicationContext.class));
    }

    @Nested
    class V2EmulatedApiReactor {

        @Mock
        private ApiReactor apiReactor;

        @BeforeEach
        public void prepareV4EmulationMock() {
            when(httpAcceptorResolver.resolve(HOST, PATH, SERVER_ID)).thenReturn(handlerEntrypoint);
            when(handlerEntrypoint.path()).thenReturn(PATH);
            when(handlerEntrypoint.reactor()).thenReturn(apiReactor);
            when(apiReactor.tracingContext()).thenReturn(TracingContext.noop());
        }

        @Test
        void shouldHandleV4EmulationRequest() {
            when(apiReactor.handle(any(MutableExecutionContext.class))).thenReturn(Completable.complete());

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            obs.assertResult();
        }

        @Test
        void shouldPropagateErrorWhenErrorWithV4EmulationRequest() {
            when(apiReactor.handle(any(MutableExecutionContext.class)))
                .thenReturn(Completable.error(new RuntimeException(MOCK_ERROR_MESSAGE)));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            obs.assertError(RuntimeException.class);
            obs.assertError(t -> MOCK_ERROR_MESSAGE.equals(t.getMessage()));
        }
    }

    @Nested
    class V2ApiReactor {

        @Mock
        private ReactorHandler apiReactor;

        @BeforeEach
        public void prepareV2ApiReactor() {
            when(httpAcceptorResolver.resolve(HOST, PATH, SERVER_ID)).thenReturn(handlerEntrypoint);
            when(handlerEntrypoint.reactor()).thenReturn(apiReactor);
            when(apiReactor.tracingContext()).thenReturn(TracingContext.noop());
        }

        @Test
        void shouldHandleV3Request() {
            when(response.ended()).thenReturn(true);

            doAnswer(i -> {
                    simulateEndHandlerCall(i);
                    return null;
                })
                .when(apiReactor)
                .handle(any(ExecutionContext.class), any(Handler.class));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            verify(vertx, never()).setTimer(anyLong(), any());
            verify(vertx, never()).cancelTimer(anyLong());

            obs.assertResult();
        }

        @Test
        void shouldHandleV3RequestWithTimeout() {
            long vertxTimerId = 125366;
            long timeout = 57;

            doReturn(vertxTimerId).when(vertx).setTimer(anyLong(), any());
            when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(timeout);

            when(response.ended()).thenReturn(true);
            final ArgumentCaptor<ExecutionContext> ctxCaptor = ArgumentCaptor.forClass(ExecutionContext.class);

            doAnswer(i -> {
                    simulateEndHandlerCall(i);
                    return null;
                })
                .when(apiReactor)
                .handle(ctxCaptor.capture(), any(Handler.class));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            verify(vertx).setTimer(eq(timeout), any());
            verify(vertx).cancelTimer(vertxTimerId);

            obs.assertResult();
        }

        @Test
        void shouldSetMetricsWhenHandlingV3Request() {
            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
            when(response.ended()).thenReturn(true);

            doAnswer(i -> {
                    final ExecutionContext ctx = i.getArgument(0, ExecutionContext.class);

                    assertEquals("TENANT", ctx.request().metrics().getTenant());
                    assertEquals("ZONE", ctx.request().metrics().getZone());
                    simulateEndHandlerCall(i);
                    return null;
                })
                .when(apiReactor)
                .handle(any(ExecutionContext.class), any(Handler.class));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            obs.assertResult();
        }

        @Test
        void shouldEndResponseWhenNotAlreadyEndedByV3Handler() {
            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
            when(response.ended()).thenReturn(false);
            when(rxResponse.rxEnd()).thenReturn(Completable.complete());

            doAnswer(i -> {
                    final ExecutionContext ctx = i.getArgument(0, ExecutionContext.class);

                    assertEquals("TENANT", ctx.request().metrics().getTenant());
                    assertEquals("ZONE", ctx.request().metrics().getZone());
                    simulateEndHandlerCall(i);
                    return null;
                })
                .when(apiReactor)
                .handle(any(ExecutionContext.class), any(Handler.class));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            obs.assertResult();
        }

        @Test
        void shouldEndResponseWhenClientConnexionIsClosed() {
            when(gatewayConfiguration.tenant()).thenReturn(Optional.of("TENANT"));
            when(gatewayConfiguration.zone()).thenReturn(Optional.of("ZONE"));
            when(response.ended()).thenReturn(false);
            when(rxResponse.rxEnd()).thenReturn(Completable.error(new RuntimeException()));

            doAnswer(i -> {
                    final ExecutionContext ctx = i.getArgument(0, ExecutionContext.class);

                    assertEquals("TENANT", ctx.request().metrics().getTenant());
                    assertEquals("ZONE", ctx.request().metrics().getZone());
                    simulateEndHandlerCall(i);
                    return null;
                })
                .when(apiReactor)
                .handle(any(ExecutionContext.class), any(Handler.class));

            cut
                .dispatch(rxRequest, SERVER_ID)
                // Simulate when the client is ending the connection before the gateway got a response from the endpoint
                .doOnSubscribe(Disposable::dispose)
                .test()
                .assertNoErrors();
        }

        @Test
        void shouldPropagateErrorWhenExceptionWithV3Request() {
            doThrow(new RuntimeException(MOCK_ERROR_MESSAGE)).when(apiReactor).handle(any(ExecutionContext.class), any(Handler.class));

            final TestObserver<Void> obs = cut.dispatch(rxRequest, SERVER_ID).test();

            obs.assertError(RuntimeException.class);
            obs.assertError(t -> MOCK_ERROR_MESSAGE.equals(t.getMessage()));
        }
    }

    @Nested
    class NotFoundApiReactor {

        @Test
        void shouldHandleNotFoundWhenNoHandlerResolved() {
            ProcessorChain processorChain = spy(new ProcessorChain("id", List.of()));
            when(notFoundProcessorChainFactory.processorChain()).thenReturn(processorChain);
            when(httpAcceptorResolver.resolve(HOST, PATH, SERVER_ID)).thenReturn(null);

            cut.dispatch(rxRequest, SERVER_ID).test().assertResult();

            verify(notFoundProcessorChainFactory).processorChain();
            verify(processorChain).execute(any(), any());
        }

        @Test
        void shouldHandleNotFoundWhenNoTargetOnResolvedHandler() {
            when(httpAcceptorResolver.resolve(HOST, PATH, SERVER_ID)).thenReturn(null);

            ProcessorChain processorChain = spy(new ProcessorChain("id", List.of()));
            when(notFoundProcessorChainFactory.processorChain()).thenReturn(processorChain);
            when(httpAcceptorResolver.resolve(HOST, PATH, SERVER_ID)).thenReturn(null);
            cut.dispatch(rxRequest, SERVER_ID).test().assertResult();

            verify(notFoundProcessorChainFactory).processorChain();
            verify(processorChain).execute(any(), any());
        }
    }

    private void simulateEndHandlerCall(InvocationOnMock i) {
        final ExecutionContext ctx = i.getArgument(0);
        final Handler<ExecutionContext> endHandler = i.getArgument(1, Handler.class);
        endHandler.handle(ctx);
    }
}
