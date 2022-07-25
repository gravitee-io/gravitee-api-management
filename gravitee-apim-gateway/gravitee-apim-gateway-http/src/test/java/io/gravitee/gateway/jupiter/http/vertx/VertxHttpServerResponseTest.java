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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.reactivex.core.http.HttpHeaders;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class VertxHttpServerResponseTest {

    protected static final String NEW_CHUNK = "New chunk";
    protected static final String MOCK_EXCEPTION = "Mock exception";
    protected static final String BODY = "chunk1chunk2chunk3";

    @Mock
    private HttpServerResponse httpServerResponse;

    @Mock
    private VertxHttpServerRequest request;

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private Metrics metrics;

    @Captor
    private ArgumentCaptor<Flowable<io.vertx.reactivex.core.buffer.Buffer>> chunksCaptor;

    private AtomicInteger subscriptionCount;
    private VertxHttpServerResponse cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        Flowable<Buffer> chunks = Flowable
            .just(Buffer.buffer("chunk1"), Buffer.buffer("chunk2"), Buffer.buffer("chunk3"))
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerResponse.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerResponse.trailers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.response()).thenReturn(httpServerResponse);
        lenient().when(request.metrics()).thenReturn(metrics);
        lenient().when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.complete());

        ReflectionTestUtils.setField(request, "nativeRequest", httpServerRequest);

        cut = new VertxHttpServerResponse(request);
        cut.chunks(chunks);
    }

    @Test
    void shouldSubscribeOnceWhenIgnoringAndReplacingExistingChunks() {
        cut.chunks(cut.chunks().ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))));
        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenReplacingExistingChunksWithBody() {
        cut
            .chunks()
            .ignoreElements()
            .andThen(Completable.fromRunnable(() -> cut.body(Buffer.buffer(NEW_CHUNK))))
            .andThen(Completable.defer(() -> cut.end()))
            .test()
            .assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldNotSubscribeOnExistingChunksWhenJustReplacingExistingBody() {
        // Note: never do that unless you really know what you are doing.
        cut.body(Buffer.buffer(NEW_CHUNK));
        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        // Existing chunks are not consumed at all (not subscribed).
        assertEquals(0, subscriptionCount.get());
    }

    @Test
    void shouldNotSubscribeOnExistingChunksWhenJustReplacingExistingChunks() {
        // Note: never do that unless you really know what you are doing.
        cut.chunks(Flowable.just(Buffer.buffer(NEW_CHUNK)));
        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        // Existing chunks are not consumed at all (not subscribed).
        assertEquals(0, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnChunksThenGettingChunksMultipleTimes() {
        cut
            .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Flowable.defer(() -> cut.chunks()))
            .ignoreElements()
            .andThen(Flowable.defer(() -> cut.chunks()))
            .ignoreElements()
            .andThen(Flowable.defer(() -> cut.chunks()))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnChunksThenGettingBodyMultipleTimes() {
        cut
            .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenGettingBodyMultipleTimes() {
        cut
            .onChunks(chunks -> chunks)
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> BODY.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingChunksMultipleTimes() {
        cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Flowable.defer(() -> cut.chunks()))
            .ignoreElements()
            .andThen(Flowable.defer(() -> cut.chunks()))
            .ignoreElements()
            .andThen(Flowable.defer(() -> cut.chunks()))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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
    void shouldReturnEmptyBufferWhenChunksIsNull() {
        mockWithNull();

        final TestObserver<Buffer> obs = cut.bodyOrEmpty().test();
        obs.assertValue(buffer -> "".equals(buffer.toString()));
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

        obs.assertErrorMessage(MOCK_EXCEPTION);
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingOnChunks() {
        mockWithError();

        final TestSubscriber<Buffer> obs = cut
            .onChunks(c -> c.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Flowable.defer(() -> cut.chunks()))
            .test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingBody() {
        mockWithError();

        final TestObserver<Buffer> obs = cut.body().test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenErrorOccursAndUsingChunks() {
        mockWithError();

        final TestSubscriber<Buffer> obs = cut.chunks().test();

        obs.assertErrorMessage(MOCK_EXCEPTION);
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingBodyMultipleTimes() {
        cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .ignoreElement()
            .andThen(Maybe.defer(() -> cut.body()))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyMultipleTimes() {
        cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(Completable.defer(() -> cut.onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(BODY))))))
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

        obs.assertValue(buffer -> BODY.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldNotSubscribeAndCompleteWhenRequestIsWebSocket() {
        when(request.isWebSocketUpgraded()).thenReturn(true);

        final TestObserver<Void> obs = cut.end().test();

        obs.assertComplete();
        verify(httpServerResponse, times(0)).rxSend(any(Flowable.class));
        verify(httpServerResponse, times(0)).rxEnd();
    }

    private void mockWithEmpty() {
        final Flowable<Buffer> chunks = Flowable.<Buffer>empty().doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        cut = new VertxHttpServerResponse(request);
        cut.chunks(chunks);
    }

    private void mockWithNull() {
        cut = new VertxHttpServerResponse(request);
        cut.chunks(null);
    }

    private void mockWithError() {
        final Flowable<Buffer> chunks = Flowable
            .<Buffer>error(new RuntimeException(MOCK_EXCEPTION))
            .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        cut = new VertxHttpServerResponse(request);
        cut.chunks(chunks);
    }
}
