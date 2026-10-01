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
package io.gravitee.gateway.reactive.http.vertx;

import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.MAX_BODY_DISCARD_DELAY_MS;
import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.MAX_DISCARDED_BODY_BYTES;
import static io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.NETTY_ATTR_CONNECTION_TIME;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.EXPECT;
import static io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest.UnconsumedBody;
import io.netty.channel.Channel;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.rxjava3.core.http.HttpConnection;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxHttpServerRequestTest {

    protected static final String NEW_CHUNK = "New chunk";
    protected static final String MOCK_EXCEPTION = "Mock exception";
    protected static final String BODY = "chunk1chunk2chunk3";

    @Mock
    HttpServerRequest httpServerRequest;

    @Mock
    IdGenerator idGenerator;

    AtomicInteger subscriptionCount;
    VertxHttpServerRequest cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable.just(
            io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk1"),
            io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk2"),
            io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk3")
        ).doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.toFlowable()).thenReturn(chunks);

        mockConnectionCreationTimestamp();

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }

    private void mockConnectionCreationTimestamp() {
        HttpConnection httpConnection = mock(HttpConnection.class);
        HttpServerConnection httpServerConnection = mock(HttpServerConnection.class);
        Channel channel = mock(Channel.class);
        Attribute attribute = mock(Attribute.class);

        when(httpServerRequest.connection()).thenReturn(httpConnection);
        when(httpConnection.getDelegate()).thenReturn(httpServerConnection);
        when(httpServerConnection.channel()).thenReturn(channel);
        when(channel.attr(AttributeKey.valueOf(NETTY_ATTR_CONNECTION_TIME))).thenReturn(attribute);
        when(attribute.get()).thenReturn(System.currentTimeMillis());
    }

    @Nested
    class HostsTest {

        @Test
        void should_return_original_host() {
            when(httpServerRequest.host()).thenReturn("original.host", "changed.host");
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);

            assertEquals("original.host", cut.originalHost());
            assertEquals("changed.host", cut.host());
        }
    }

    @Nested
    class ChunkSubscriptionTest {

        @Mock
        private MockedStatic<RequestUtils> requestUtilsMockedStatic;

        @BeforeEach
        public void beforeEach() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isStreaming(any(Request.class))).thenAnswer(invocation -> false);
        }

        @Test
        void should_subscribe_once_when_ignoring_and_replacing_existing_chunks() {
            cut.chunks(cut.chunks().ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))));

            final TestSubscriber<Buffer> obs = cut.chunks().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_replacing_existing_chunks_with_body() {
            final TestSubscriber<Buffer> obs = cut
                .chunks()
                .ignoreElements()
                .andThen(
                    Flowable.defer(() -> {
                        cut.body(Buffer.buffer(NEW_CHUNK));
                        return cut.chunks();
                    })
                )
                .test();
            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_not_subscribe_on_existing_chunks_when_just_replacing_existing_chunks() {
            // Note: never do that unless you really know what you are doing.
            cut.chunks(Flowable.just(Buffer.buffer(NEW_CHUNK)));

            final TestSubscriber<Buffer> obs = cut.chunks().test();
            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

            // Existing chunks are not consumed at all (not subscribed).
            assertEquals(0, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_onChunks_then_getting_chunks_multiple_times() {
            final TestSubscriber<Buffer> obs = cut
                .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_onChunks_then_getting_body_multiple_times() {
            final TestObserver<Buffer> obs = cut
                .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_getting_body_multiple_times() {
            final TestObserver<Buffer> obs = cut
                .onChunks(chunks -> chunks)
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_onBody_then_getting_chunks_multiple_times() {
            final TestSubscriber<Buffer> obs = cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_and_return_empty_when_using_onBody_then_getting_chunks_multiple_times() {
            mockWithEmpty();

            final TestSubscriber<Buffer> obs = cut
                .onBody(body -> body.flatMap(buffer -> Maybe.just(Buffer.buffer(NEW_CHUNK)))) // Should not reach it cause chunks are empty.
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertNoValues();
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_return_empty_buffer_when_bodyOrEmpty() {
            mockWithEmpty();

            final TestObserver<Buffer> obs = cut.bodyOrEmpty().test();
            obs.assertValue(buffer -> "".equals(buffer.toString()));
        }

        @Test
        void should_subscribe_once_and_return_empty_observable_when_bodyOrEmpty_then_getting_chunks() {
            mockWithEmpty();

            final TestSubscriber<Buffer> obs = cut.bodyOrEmpty().ignoreElement().andThen(Flowable.defer(() -> cut.chunks())).test();

            obs.assertNoValues();
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_onBody() {
            mockWithError();

            final TestSubscriber<Buffer> obs = cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_onChunks() {
            mockWithError();

            final TestSubscriber<Buffer> obs = cut
                .onChunks(c -> c.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_body() {
            mockWithError();

            final TestObserver<Buffer> obs = cut.body().test();

            obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_chunks() {
            mockWithError();

            final TestSubscriber<Buffer> obs = cut.chunks().test();

            obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_onBody_then_getting_body_multiple_times() {
            final TestObserver<Buffer> obs = cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_onBody_multiple_times() {
            final TestObserver<Buffer> obs = cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Completable.defer(() -> cut.onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(BODY))))))
                .andThen(Maybe.defer(() -> cut.body()))
                .test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }
    }

    @Nested
    class RequestFlowTest {

        @Test
        void should_pause_native_request() {
            cut.pause();
            verify(httpServerRequest).pause();
        }

        @Test
        void should_resume_native_request() {
            cut.resume();
            verify(httpServerRequest).resume();
        }
    }

    @Nested
    class UnconsumedBodyTest {

        private static final long TIMER_ID = 42L;

        @Mock
        Context context;

        @Mock
        Vertx vertx;

        @Captor
        ArgumentCaptor<Handler<io.vertx.rxjava3.core.buffer.Buffer>> bodyHandlerCaptor;

        @Captor
        ArgumentCaptor<Handler<Void>> endHandlerCaptor;

        @Captor
        ArgumentCaptor<Handler<Long>> timerHandlerCaptor;

        private MockedStatic<Vertx> vertxStatic;

        @BeforeEach
        void mockVertxContext() {
            vertxStatic = mockStatic(Vertx.class);
            vertxStatic.when(Vertx::currentContext).thenReturn(context);
            lenient().when(context.owner()).thenReturn(vertx);
            lenient().when(vertx.setTimer(anyLong(), any())).thenReturn(TIMER_ID);
            lenient().when(httpServerRequest.version()).thenReturn(io.vertx.core.http.HttpVersion.HTTP_1_1);
        }

        @AfterEach
        void closeVertxStatic() {
            vertxStatic.close();
        }

        @Test
        void should_discard_body_of_known_length_under_the_limit() {
            cut = requestWithHeader(CONTENT_LENGTH, String.valueOf(MAX_DISCARDED_BODY_BYTES));

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.DISCARD);
        }

        @Test
        void should_discard_then_close_when_body_is_over_the_limit() {
            cut = requestWithHeader(CONTENT_LENGTH, String.valueOf(MAX_DISCARDED_BODY_BYTES + 1));

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.DISCARD_THEN_CLOSE);
        }

        @Test
        void should_discard_then_close_when_body_length_is_unknown() {
            cut = requestWithHeader(TRANSFER_ENCODING, "chunked");

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.DISCARD_THEN_CLOSE);
        }

        @Test
        void should_discard_then_close_when_client_expects_100_continue() {
            when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers().add(CONTENT_LENGTH, "10").add(EXPECT, "100-continue"));
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.DISCARD_THEN_CLOSE);
        }

        @Test
        void should_have_nothing_to_discard_when_no_body_is_announced() {
            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.NONE);
        }

        @Test
        void should_have_nothing_to_discard_when_body_length_is_zero() {
            cut = requestWithHeader(CONTENT_LENGTH, "0");

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.NONE);
        }

        @Test
        void should_have_nothing_to_discard_when_request_ended() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");
            when(httpServerRequest.isEnded()).thenReturn(true);

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.NONE);
        }

        @Test
        void should_have_nothing_to_discard_when_body_was_subscribed() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");
            cut.chunks().test();

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.NONE);
        }

        @Test
        void should_have_nothing_to_discard_on_http2() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");
            when(httpServerRequest.version()).thenReturn(io.vertx.core.http.HttpVersion.HTTP_2);

            assertThat(cut.unconsumedBody()).isEqualTo(UnconsumedBody.NONE);
        }

        @Test
        void should_keep_connection_once_body_is_discarded() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");

            cut.discardUnconsumedBody(false);

            verify(httpServerRequest).resume();
            verify(httpServerRequest).endHandler(endHandlerCaptor.capture());
            endHandlerCaptor.getValue().handle(null);

            verify(httpServerRequest.connection(), never()).close();
            verify(vertx).cancelTimer(TIMER_ID);
        }

        @Test
        void should_close_connection_once_body_is_discarded_when_asked_to() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");

            cut.discardUnconsumedBody(true);

            verify(httpServerRequest.connection(), never()).close();
            verify(httpServerRequest).endHandler(endHandlerCaptor.capture());
            endHandlerCaptor.getValue().handle(null);

            verify(httpServerRequest.connection()).close();
        }

        @Test
        void should_close_connection_when_discarded_bytes_exceed_limit() {
            cut = requestWithHeader(TRANSFER_ENCODING, "chunked");

            cut.discardUnconsumedBody(false);

            verify(httpServerRequest).handler(bodyHandlerCaptor.capture());
            final Handler<io.vertx.rxjava3.core.buffer.Buffer> bodyHandler = bodyHandlerCaptor.getValue();
            bodyHandler.handle(io.vertx.rxjava3.core.buffer.Buffer.buffer(new byte[(int) MAX_DISCARDED_BODY_BYTES]));
            verify(httpServerRequest.connection(), never()).close();

            bodyHandler.handle(io.vertx.rxjava3.core.buffer.Buffer.buffer(new byte[1]));
            bodyHandler.handle(io.vertx.rxjava3.core.buffer.Buffer.buffer(new byte[1]));
            verify(httpServerRequest.connection(), times(1)).close();
        }

        @Test
        void should_close_connection_when_discard_delay_elapses() {
            cut = requestWithHeader(CONTENT_LENGTH, "10");

            cut.discardUnconsumedBody(false);

            verify(vertx).setTimer(eq(MAX_BODY_DISCARD_DELAY_MS), timerHandlerCaptor.capture());
            verify(httpServerRequest.connection(), never()).close();
            timerHandlerCaptor.getValue().handle(TIMER_ID);

            verify(httpServerRequest.connection()).close();
        }

        @Test
        void should_close_connection_immediately_when_asked_to_and_nothing_left_to_discard() {
            cut.discardUnconsumedBody(true);

            verify(httpServerRequest.connection()).close();
            verify(httpServerRequest, never()).resume();
        }

        private VertxHttpServerRequest requestWithHeader(CharSequence name, String value) {
            when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers().add(name, value));
            return new VertxHttpServerRequest(httpServerRequest, idGenerator);
        }
    }

    @Nested
    class MessagesInterceptorTest {

        @Test
        void should_set_onMessagesInterceptor() {
            final MessageFlow<Message> messageFlow = mock(MessageFlow.class);
            ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);
            final OnMessagesInterceptor.MessagesInterceptor interceptor = mock(OnMessagesInterceptor.MessagesInterceptor.class);
            cut.registerMessagesInterceptor(interceptor);

            verify(messageFlow).registerMessagesInterceptor(interceptor);
        }

        @Test
        void should_unset_onMessagesInterceptor() {
            final MessageFlow<Message> messageFlow = mock(MessageFlow.class);
            ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);

            cut.unregisterMessagesInterceptor("id");

            verify(messageFlow).unregisterMessagesInterceptor("id");
        }
    }

    @Nested
    class WebSocketRequestTest {

        @Mock
        private MockedStatic<RequestUtils> requestUtilsMockedStatic;

        @BeforeEach
        public void beforeEach() {
            reset(httpServerRequest);
            lenient().when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers());
            lenient().when(httpServerRequest.toFlowable()).thenReturn(Flowable.empty());

            mockConnectionCreationTimestamp();
        }

        @Test
        void should_check_websocket_only_once() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);

            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);

            for (int i = 0; i < 10; i++) {
                assertTrue(cut.isWebSocket());
            }

            requestUtilsMockedStatic.verify(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class)), times(1));
        }

        @Test
        void should_create_websocket() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);

            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            assertNotNull(cut.webSocket());
        }

        @Test
        void should_create_websocket_only_once() {
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            ReflectionTestUtils.setField(cut, "isWebSocket", true);
            final WebSocket webSocket = cut.webSocket();

            for (int i = 0; i < 10; i++) {
                assertSame(webSocket, cut.webSocket());
            }
        }

        @Test
        void should_no_create_websocket_when_request_is_not_websocket() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> false);
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            assertNull(cut.webSocket());
        }

        @Test
        void should_not_be_websocket_upgraded_when_request_is_not_websocket() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            assertFalse(cut.isWebSocketUpgraded());
        }

        @Test
        void should_not_be_websocket_upgraded_when_websocket_not_created() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            assertTrue(cut.isWebSocket());
            assertFalse(cut.isWebSocketUpgraded());
        }

        @Test
        void should_not_be_websocket_upgraded_when_web_socket_has_been_created_but_not_upgraded() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);

            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            final WebSocket webSocket = cut.webSocket();
            ReflectionTestUtils.setField(webSocket, "upgraded", false);

            assertNotNull(webSocket);
            assertFalse(cut.isWebSocketUpgraded());
        }

        @Test
        void should_be_websocket_upgraded_when_websocket_hasbeen_created_and_upgraded() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isWebSocket(any(HttpServerRequest.class))).thenAnswer(invocation -> true);
            cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
            final WebSocket webSocket = cut.webSocket();
            ReflectionTestUtils.setField(webSocket, "upgraded", true);

            assertNotNull(webSocket);
            assertTrue(cut.isWebSocketUpgraded());
        }
    }

    @Nested
    class StreamingRequestTest {

        @Mock
        private MockedStatic<RequestUtils> requestUtilsMockedStatic;

        @Test
        void should_check_streaming_only_once() {
            requestUtilsMockedStatic.when(() -> RequestUtils.isStreaming(any(Request.class))).thenAnswer(invocation -> true);

            cut = spy(new VertxHttpServerRequest(httpServerRequest, idGenerator));

            for (int i = 0; i < 10; i++) {
                assertTrue(cut.isStreaming());
            }

            requestUtilsMockedStatic.verify(() -> RequestUtils.isStreaming(any(Request.class)), times(1));
        }
    }

    private void mockWithEmpty() {
        final Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable.<io.vertx.rxjava3.core.buffer.Buffer>empty().doOnSubscribe(
            subscription -> subscriptionCount.incrementAndGet()
        );

        when(httpServerRequest.toFlowable()).thenReturn(chunks);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }

    private void mockWithError() {
        final Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable.<io.vertx.rxjava3.core.buffer.Buffer>error(
            new RuntimeException(MOCK_EXCEPTION)
        ).doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.toFlowable()).thenReturn(chunks);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }
}
