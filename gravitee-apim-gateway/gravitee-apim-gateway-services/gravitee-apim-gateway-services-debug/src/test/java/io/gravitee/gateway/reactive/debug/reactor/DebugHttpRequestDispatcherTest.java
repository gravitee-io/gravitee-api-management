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
package io.gravitee.gateway.reactive.debug.reactor;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API;
import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.NETTY_ATTR_CONNECTION_TIME;
import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.debug.definition.DebugApiV2;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.debug.handlers.api.DebugApiReactorHandler;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.env.RequestClientAuthConfiguration;
import io.gravitee.gateway.env.RequestPathConfiguration;
import io.gravitee.gateway.env.RequestPathHandling;
import io.gravitee.gateway.env.RequestTimeoutConfiguration;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.ProcessorChain;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugCompletionProcessor;
import io.gravitee.gateway.reactive.debug.reactor.processor.DebugPlatformProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.ApiReactor;
import io.gravitee.gateway.reactive.reactor.handler.HttpAcceptorResolver;
import io.gravitee.gateway.reactive.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessorFactory;
import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.core.net.HostAndPort;
import io.vertx.rxjava3.core.http.HttpConnection;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * What happens to a debug session whose path the gateway refuses to route on.
 *
 * <p>The refusal is decided before the acceptor is resolved, so nothing downstream ever learns the
 * request was a debug one. Left alone, the debug event stays in {@code DEBUGGING} and the console
 * waits forever. What is pinned here is that the dispatcher answers the request <em>and</em> closes
 * the session, and that it does not interfere when there is nothing to refuse.
 *
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DebugHttpRequestDispatcherTest {

    private static final String HOST = "gravitee.io";
    private static final int PORT = -1;
    private static final String CANONICAL_PATH = "/event-id-proxyv4/b";
    private static final String PATH_WITH_DOT_SEGMENTS = "/event-id-proxyv4/a/../b";
    private static final String SERVER_ID = null;
    private static final String EVENT_ID = "event-id";

    @Spy
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private HttpServerRequest rxRequest;

    @Mock
    private HttpServerResponse rxResponse;

    @Mock
    private io.vertx.core.http.HttpServerRequest request;

    @Mock
    private io.vertx.core.http.HttpServerResponse response;

    @Mock
    private HttpAcceptorResolver httpAcceptorResolver;

    @Mock
    private NotFoundProcessorChainFactory notFoundProcessorChainFactory;

    @Mock
    private DebugPlatformProcessorChainFactory platformProcessorChainFactory;

    @Mock
    private DebugCompletionProcessor debugCompletionProcessor;

    @Mock
    private GatewayConfiguration gatewayConfiguration;

    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private ResponseProcessorChainFactory responseProcessorChainFactory;

    @Mock
    private RequestTimeoutConfiguration requestTimeoutConfiguration;

    @Mock
    private RequestClientAuthConfiguration requestClientAuthConfiguration;

    @Mock
    private ReactableDebugApi<?> debugApi;

    @Mock
    private ReporterService reporterService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Node node;

    @BeforeEach
    void init() {
        lenient().when(rxRequest.authority()).thenReturn(HostAndPort.create(HOST, PORT));
        lenient().when(rxRequest.version()).thenReturn(HttpVersion.HTTP_1_1);
        lenient().when(rxRequest.method()).thenReturn(HttpMethod.GET);
        lenient().when(rxRequest.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxRequest.toFlowable()).thenReturn(Flowable.empty());
        lenient().when(rxRequest.response()).thenReturn(rxResponse);
        lenient().when(rxRequest.getDelegate()).thenReturn(request);

        lenient().when(request.authority()).thenReturn(HostAndPort.create(HOST, PORT));
        lenient().when(request.method()).thenReturn(HttpMethod.GET);
        lenient().when(request.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(request.response()).thenReturn(response);

        lenient().when(rxResponse.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxResponse.trailers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(rxResponse.getDelegate()).thenReturn(response);

        lenient().when(response.headers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(response.trailers()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        lenient().when(response.end()).thenReturn(Future.succeededFuture());

        lenient().when(requestTimeoutConfiguration.getRequestTimeout()).thenReturn(0L);
        lenient().when(requestTimeoutConfiguration.getRequestTimeoutGraceDelay()).thenReturn(10L);
        lenient().when(notFoundProcessorChainFactory.rejectedPathProcessorChain()).thenReturn(new ProcessorChain("rejected", List.of()));
        lenient().when(notFoundProcessorChainFactory.processorChain()).thenReturn(new ProcessorChain("not-found", List.of()));
        lenient().when(platformProcessorChainFactory.preProcessorChain()).thenReturn(new ProcessorChain("pre", List.of()));
        lenient().when(platformProcessorChainFactory.postProcessorChain()).thenReturn(new ProcessorChain("post", List.of()));
        lenient().when(debugCompletionProcessor.execute(any())).thenReturn(Completable.complete());

        // Contextual logging resolves a Node through the component provider, and the rejection path
        // logs on the branch where no debug API matched.
        lenient().when(globalComponentProvider.getComponent(Node.class)).thenReturn(node);

        mockConnectionCreationTimestamp();
    }

    private void mockConnectionCreationTimestamp() {
        HttpConnection httpConnection = mock(HttpConnection.class);
        HttpServerConnection httpServerConnection = mock(HttpServerConnection.class);
        Channel channel = mock(Channel.class);
        ChannelHandlerContext channelHandlerContext = mock(ChannelHandlerContext.class);
        Attribute attribute = mock(Attribute.class);

        lenient().when(rxRequest.connection()).thenReturn(httpConnection);
        lenient().when(httpConnection.getDelegate()).thenReturn(httpServerConnection);
        lenient().when(httpServerConnection.channelHandlerContext()).thenReturn(channelHandlerContext);
        lenient().when(channelHandlerContext.channel()).thenReturn(channel);
        lenient().when(channel.attr(AttributeKey.valueOf(NETTY_ATTR_CONNECTION_TIME))).thenReturn(attribute);
        lenient().when(attribute.get()).thenReturn(System.currentTimeMillis());
    }

    private DebugHttpRequestDispatcher dispatcher(final RequestPathHandling handling) {
        return new DebugHttpRequestDispatcher(
            gatewayConfiguration,
            httpAcceptorResolver,
            idGenerator,
            globalComponentProvider,
            new RequestProcessorChainFactory(),
            responseProcessorChainFactory,
            platformProcessorChainFactory,
            notFoundProcessorChainFactory,
            requestTimeoutConfiguration,
            requestClientAuthConfiguration,
            new RequestPathConfiguration(handling),
            debugCompletionProcessor,
            vertx,
            true
        );
    }

    private void resolveDebugApi() {
        ApiReactor<?> apiReactor = mock(ApiReactor.class);
        HttpAcceptor acceptor = mock(HttpAcceptor.class);
        lenient()
            .when(apiReactor.api())
            .thenAnswer(invocation -> debugApi);
        lenient().when(acceptor.reactor()).thenReturn(apiReactor);
        lenient().when(httpAcceptorResolver.resolve(HOST, PATH_WITH_DOT_SEGMENTS, SERVER_ID)).thenReturn(acceptor);
    }

    @Nested
    class When_the_path_is_refused {

        @Test
        void should_close_the_debug_session_instead_of_leaving_the_console_waiting() {
            // Given a debug request the gateway will refuse to route on.
            when(rxRequest.path()).thenReturn(PATH_WITH_DOT_SEGMENTS);
            resolveDebugApi();

            // When it is dispatched on the debug port.
            dispatcher(RequestPathHandling.REJECT).dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            // Then the session is completed rather than abandoned mid-flight.
            verify(debugCompletionProcessor).execute(any());
        }

        @Test
        void should_hand_the_completion_the_api_it_could_not_otherwise_find() {
            // Given the same request. The API-scoped component provider is never installed, because
            // no reactor ever handles the request, so the attribute is the only channel left.
            when(rxRequest.path()).thenReturn(PATH_WITH_DOT_SEGMENTS);
            resolveDebugApi();

            // When it is dispatched.
            dispatcher(RequestPathHandling.REJECT).dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            // Then the context carries the debug API, and it is a debug context so the completion
            // processor can read the steps — empty here, since no policy ran.
            ArgumentCaptor<HttpExecutionContextInternal> captor = ArgumentCaptor.forClass(HttpExecutionContextInternal.class);
            verify(debugCompletionProcessor).execute(captor.capture());

            assertThat(captor.getValue()).isInstanceOf(DebugExecutionContext.class);
            assertThat((Object) captor.getValue().getInternalAttribute(ATTR_INTERNAL_REACTABLE_API)).isSameAs(debugApi);
        }

        @Test
        void should_close_the_session_of_an_api_running_in_v3_execution_mode() {
            // Given a V2 API whose executionMode is V3. It is not deployed behind an ApiReactor:
            // ApiReactorHandlerFactory branches into getApiReactorHandler, which the debug service
            // overrides to DebugApiReactorHandler — a ReactorHandler, never an ApiReactor.
            when(rxRequest.path()).thenReturn(PATH_WITH_DOT_SEGMENTS);
            final DebugApiReactorHandler handler = mock(DebugApiReactorHandler.class);
            final HttpAcceptor acceptor = mock(HttpAcceptor.class);
            lenient()
                .when(handler.debugApi())
                .thenAnswer(invocation -> debugApi);
            lenient().when(acceptor.reactor()).thenReturn(handler);
            lenient().when(httpAcceptorResolver.resolve(HOST, PATH_WITH_DOT_SEGMENTS, SERVER_ID)).thenReturn(acceptor);

            // When the request is refused on its path.
            dispatcher(RequestPathHandling.REJECT).dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            // Then the session is closed for this engine too. Matching only ApiReactor left the
            // console spinning on exactly the dead end this override exists to remove, and the only
            // clue was a warning saying no debug API matched — while the acceptor had matched.
            verify(debugCompletionProcessor).execute(any());
        }

        @Test
        void should_still_answer_when_no_debug_api_matches_the_path() {
            // Given a refused path that matches no acceptor: there is no session to close, but the
            // request must still be answered rather than hang.
            when(rxRequest.path()).thenReturn(PATH_WITH_DOT_SEGMENTS);
            when(httpAcceptorResolver.resolve(HOST, PATH_WITH_DOT_SEGMENTS, SERVER_ID)).thenReturn(null);

            dispatcher(RequestPathHandling.REJECT).dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            verify(debugCompletionProcessor, never()).execute(any());
        }
    }

    /**
     * The wiring tests above replace the rejected chain with an empty one and the completion
     * processor with a mock, so they pin that the dispatcher calls the right things. They would pass
     * unchanged if the response never carried a 400, or if the debug event were never moved out of
     * {@code DEBUGGING} — which is the entire behaviour this class exists to deliver. This runs the
     * real chain and the real processor, and asks the two questions a user would.
     */
    @Nested
    class End_to_end_on_the_debug_port {

        @Test
        void should_answer_400_and_move_the_event_out_of_debugging() throws Exception {
            // Given a real rejected chain and a real completion processor over a stored debug event.
            // A real debug API rather than a mock: the completion processor converts the definition
            // into the payload, so a hollow stub only proves that the failure branch is reachable.
            when(rxRequest.path()).thenReturn(PATH_WITH_DOT_SEGMENTS);
            resolveRealDebugApi();

            final Event stored = new Event();
            stored.setId(EVENT_ID);
            stored.setProperties(new HashMap<>());
            stored.setType(EventType.DEBUG_API);
            when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(stored));

            // The real chain actually writes the body, which the stubbed-empty chain never does.
            lenient().when(rxResponse.rxSend(any(Flowable.class))).thenReturn(Completable.complete());

            final DebugHttpRequestDispatcher dispatcher = dispatcherWithRealChain();

            // When a path the gateway refuses to route on arrives on the debug port.
            dispatcher.dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            // Then the caller got a 400...
            verify(rxResponse).setStatusCode(400);

            // ...and the session was closed, so the console stops waiting.
            final ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            verify(eventRepository, timeout(5_000)).update(captor.capture());
            assertThat(captor.getValue().getProperties()).containsEntry(API_DEBUG_STATUS.getValue(), ApiDebugStatus.SUCCESS.name());
        }

        private void resolveRealDebugApi() {
            final io.gravitee.definition.model.debug.DebugApiV2 definition = new io.gravitee.definition.model.debug.DebugApiV2();
            definition.setId("api-id");
            definition.setName("alpha");
            definition.setVersion("1.0");
            final DebugApiV2 realDebugApi = new DebugApiV2(EVENT_ID, definition);

            final ApiReactor<?> apiReactor = mock(ApiReactor.class);
            final HttpAcceptor acceptor = mock(HttpAcceptor.class);
            lenient()
                .when(apiReactor.api())
                .thenAnswer(invocation -> realDebugApi);
            lenient().when(acceptor.reactor()).thenReturn(apiReactor);
            lenient().when(httpAcceptorResolver.resolve(HOST, PATH_WITH_DOT_SEGMENTS, SERVER_ID)).thenReturn(acceptor);
        }

        private DebugHttpRequestDispatcher dispatcherWithRealChain() {
            final NotFoundProcessorChainFactory realChainFactory = new NotFoundProcessorChainFactory(
                new TransactionPreProcessorFactory(null, null),
                new StandardEnvironment(),
                reporterService,
                false,
                true,
                gatewayConfiguration
            );
            return new DebugHttpRequestDispatcher(
                gatewayConfiguration,
                httpAcceptorResolver,
                idGenerator,
                globalComponentProvider,
                new RequestProcessorChainFactory(),
                responseProcessorChainFactory,
                platformProcessorChainFactory,
                realChainFactory,
                requestTimeoutConfiguration,
                requestClientAuthConfiguration,
                new RequestPathConfiguration(RequestPathHandling.REJECT),
                new DebugCompletionProcessor(eventRepository, new ObjectMapper()),
                vertx,
                true
            );
        }
    }

    @Nested
    class When_there_is_nothing_to_refuse {

        @Test
        void should_not_interfere_with_a_canonical_path() {
            // Given a path that needs no resolution, under the strictest mode.
            when(rxRequest.path()).thenReturn(CANONICAL_PATH);
            when(httpAcceptorResolver.resolve(HOST, CANONICAL_PATH, SERVER_ID)).thenReturn(null);

            // When it is dispatched, the ordinary flow runs — here it finds no acceptor, which is
            // the not-found branch and none of this class's business.
            dispatcher(RequestPathHandling.REJECT).dispatch(rxRequest, SERVER_ID).test().awaitDone(10, TimeUnit.SECONDS);

            // Then the rejection path was never taken.
            verify(debugCompletionProcessor, never()).execute(any());
            verify(notFoundProcessorChainFactory, never()).rejectedPathProcessorChain();
        }
    }
}
