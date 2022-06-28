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

import static io.reactivex.Flowable.defer;
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

    private Flowable<Buffer> chunks;

    private AtomicInteger subscriptionCount;
    private VertxHttpServerResponse cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        chunks =
            Flowable
                .just(Buffer.buffer("chunk1"), Buffer.buffer("chunk2"), Buffer.buffer("chunk3"))
                .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerResponse.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerResponse.trailers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.response()).thenReturn(httpServerResponse);
        when(request.metrics()).thenReturn(metrics);
        when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.complete());

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
        cut.body(Buffer.buffer(NEW_CHUNK));
        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        assertEquals(1, subscriptionCount.get());
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
            .onChunk(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
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
            .onChunk(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingChunksMultipleTimes() {
        cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingBodyMultipleTimes() {
        cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .test()
            .assertComplete();

        cut.end().test().assertComplete();

        verify(httpServerResponse).rxSend(chunksCaptor.capture());

        final TestSubscriber<io.vertx.reactivex.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        assertEquals(1, subscriptionCount.get());
    }
}
