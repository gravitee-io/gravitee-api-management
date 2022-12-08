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
package io.gravitee.gateway.jupiter.http.vertx;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.ws.WebSocket;
import io.gravitee.gateway.jupiter.core.MessageFlow;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
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
class VertxHttpServerRequestTest {

    protected static final String NEW_CHUNK = "New chunk";
    protected static final String MOCK_EXCEPTION = "Mock exception";
    protected static final String BODY = "chunk1chunk2chunk3";

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private IdGenerator idGenerator;

    private AtomicInteger subscriptionCount;
    private VertxHttpServerRequest cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable
            .just(
                io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk1"),
                io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk2"),
                io.vertx.rxjava3.core.buffer.Buffer.buffer("chunk3")
            )
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.toFlowable()).thenReturn(chunks);
        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }

    @Test
    void shouldSubscribeOnceWhenIgnoringAndReplacingExistingChunks() {
        cut.chunks(cut.chunks().ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))));

        final TestSubscriber<Buffer> obs = cut.chunks().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenReplacingExistingChunksWithBody() {
        final TestSubscriber<Buffer> obs = cut
            .chunks()
            .ignoreElements()
            .andThen(
                Flowable.defer(
                    () -> {
                        cut.body(Buffer.buffer(NEW_CHUNK));
                        return cut.chunks();
                    }
                )
            )
            .test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldNotSubscribeOnExistingChunksWhenJustReplacingExistingChunks() {
        // Note: never do that unless you really know what you are doing.
        cut.chunks(Flowable.just(Buffer.buffer(NEW_CHUNK)));

        final TestSubscriber<Buffer> obs = cut.chunks().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        // Existing chunks are not consumed at all (not subscribed).
        assertEquals(0, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnChunksThenGettingChunksMultipleTimes() {
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
    void shouldSubscribeOnceWhenUsingOnChunksThenGettingBodyMultipleTimes() {
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
    void shouldSubscribeOnceWhenGettingBodyMultipleTimes() {
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
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingChunksMultipleTimes() {
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
    void shouldSubscribeOnceAndReturnEmptyWhenUsingOnBodyThenGettingChunksMultipleTimes() {
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
    void shouldReturnEmptyBufferWhenBodyOrEmpty() {
        mockWithEmpty();

        final TestObserver<Buffer> obs = cut.bodyOrEmpty().test();
        obs.assertValue(buffer -> "".equals(buffer.toString()));
    }

    @Test
    void shouldSubscribeOnceAndReturnEmptyObservableWhenBodyOrEmptyThenGettingChunks() {
        mockWithEmpty();

        final TestSubscriber<Buffer> obs = cut.bodyOrEmpty().ignoreElement().andThen(Flowable.defer(() -> cut.chunks())).test();

        obs.assertNoValues();
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingOnBody() {
        mockWithError();

        final TestSubscriber<Buffer> obs = cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Flowable.defer(() -> cut.chunks()))
            .test();

        obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingOnChunks() {
        mockWithError();

        final TestSubscriber<Buffer> obs = cut
            .onChunks(c -> c.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Flowable.defer(() -> cut.chunks()))
            .test();

        obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingBody() {
        mockWithError();

        final TestObserver<Buffer> obs = cut.body().test();

        obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingChunks() {
        mockWithError();

        final TestSubscriber<Buffer> obs = cut.chunks().test();

        obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingBodyMultipleTimes() {
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
    void shouldSubscribeOnceWhenUsingOnBodyMultipleTimes() {
        final TestObserver<Buffer> obs = cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Completable.defer(() -> cut.onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(BODY))))))
            .andThen(Maybe.defer(() -> cut.body()))
            .test();

        obs.assertValue(buffer -> BODY.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldNotBeWebSocketWhenHttp2() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_2);
        assertFalse(cut.isWebSocket());
    }

    @Test
    void shouldNotBeWebSocketWhenNoConnectionHeader() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn(null);
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertFalse(cut.isWebSocket());
    }

    @Test
    void shouldNotBeWebSocketWhenNoUpgradeHeader() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn(null);
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertFalse(cut.isWebSocket());
    }

    @Test
    void shouldNotBeWebSocketWhenNotGetMethod() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.POST);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertFalse(cut.isWebSocket());
    }

    @Test
    void shouldNotBeWebSocketWhenNotUpgradeWebSocket() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("something else");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertFalse(cut.isWebSocket());
    }

    @Test
    void shouldBeWebSocket() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertTrue(cut.isWebSocket());
    }

    @Test
    void shouldBeWebSocketConnectionHeaderMultiValues() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("keep-alive, Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertTrue(cut.isWebSocket());
    }

    @Test
    void shouldCheckWebSocketOnlyOnce() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);

        for (int i = 0; i < 10; i++) {
            assertTrue(cut.isWebSocket());
        }

        verify(httpServerRequest).getHeader(HttpHeaders.CONNECTION);
        verify(httpServerRequest).getHeader(HttpHeaders.UPGRADE);
        verify(httpServerRequest).method();
        verify(httpServerRequest).version();
    }

    @Test
    void shouldCreateWebSocket() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertNotNull(cut.webSocket());
    }

    @Test
    void shouldCreateWebSocketOnlyOnce() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        final WebSocket webSocket = cut.webSocket();

        for (int i = 0; i < 10; i++) {
            assertSame(webSocket, cut.webSocket());
        }
    }

    @Test
    void shouldNoCreateWebSocketWhenRequestIsNotWebSocket() {
        assertNull(cut.webSocket());
    }

    @Test
    void shouldNotBeWebSocketUpgradedWhenRequestIsNotWebSocket() {
        assertFalse(cut.isWebSocketUpgraded());
    }

    @Test
    void shouldNotBeWebSocketUpgradedWhenWebSocketNotCreated() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        assertTrue(cut.isWebSocket());
        assertFalse(cut.isWebSocketUpgraded());
    }

    @Test
    void shouldNotBeWebSocketUpgradedWhenWebSocketHasBeenCreatedButNotUpgraded() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        final WebSocket webSocket = cut.webSocket();
        ReflectionTestUtils.setField(webSocket, "upgraded", false);

        assertNotNull(webSocket);
        assertFalse(cut.isWebSocketUpgraded());
    }

    @Test
    void shouldBeWebSocketUpgradedWhenWebSocketHasBeenCreatedAndUpgraded() {
        when(httpServerRequest.getHeader(HttpHeaders.CONNECTION)).thenReturn("Upgrade");
        when(httpServerRequest.getHeader(HttpHeaders.UPGRADE)).thenReturn("websocket");
        when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
        when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
        final WebSocket webSocket = cut.webSocket();
        ReflectionTestUtils.setField(webSocket, "upgraded", true);

        assertNotNull(webSocket);
        assertTrue(cut.isWebSocketUpgraded());
    }

    @Test
    void shouldReturnOriginalHost() {
        when(httpServerRequest.host()).thenReturn("original.host", "changed.host");
        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);

        assertEquals("original.host", cut.originalHost());
        assertEquals("changed.host", cut.host());
    }

    @Test
    void shouldPause() {
        cut.pause();
        verify(httpServerRequest).pause();
    }

    @Test
    void shouldResume() {
        cut.resume();
        verify(httpServerRequest).resume();
    }

    @Test
    void shouldSetOnMessagesInterceptor() {
        final MessageFlow messageFlow = mock(MessageFlow.class);
        ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);
        final Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor = mock(Function.class);
        cut.setMessagesInterceptor(interceptor);

        verify(messageFlow).setOnMessagesInterceptor(interceptor);
    }

    @Test
    void shouldUnsetOnMessagesInterceptor() {
        final MessageFlow messageFlow = mock(MessageFlow.class);
        ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);

        cut.unsetMessagesInterceptor();

        verify(messageFlow).unsetOnMessagesInterceptor();
    }

    private void mockWithEmpty() {
        final Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable
            .<io.vertx.rxjava3.core.buffer.Buffer>empty()
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.toFlowable()).thenReturn(chunks);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }

    private void mockWithError() {
        final Flowable<io.vertx.rxjava3.core.buffer.Buffer> chunks = Flowable
            .<io.vertx.rxjava3.core.buffer.Buffer>error(new RuntimeException(MOCK_EXCEPTION))
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.toFlowable()).thenReturn(chunks);

        cut = new VertxHttpServerRequest(httpServerRequest, idGenerator);
    }
}
