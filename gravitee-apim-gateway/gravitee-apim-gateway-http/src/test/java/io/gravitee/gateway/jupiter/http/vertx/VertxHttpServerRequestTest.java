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
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.gateway.api.buffer.Buffer;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.core.http.HttpHeaders;
import io.vertx.reactivex.core.http.HttpServerRequest;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class VertxHttpServerRequestTest {

    protected static final String NEW_CHUNK = "New chunk";

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private IdGenerator idGenerator;

    private Flowable<io.vertx.reactivex.core.buffer.Buffer> chunks;

    private AtomicInteger subscriptionCount;
    private VertxHttpServerRequest cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        chunks =
            Flowable
                .just(
                    io.vertx.reactivex.core.buffer.Buffer.buffer("chunk1"),
                    io.vertx.reactivex.core.buffer.Buffer.buffer("chunk2"),
                    io.vertx.reactivex.core.buffer.Buffer.buffer("chunk3")
                )
                .doOnSubscribe(subscription -> subscriptionCount.incrementAndGet());

        when(httpServerRequest.method()).thenReturn(HttpMethod.POST);
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
        cut.body(Buffer.buffer(NEW_CHUNK));

        final TestSubscriber<Buffer> obs = cut.chunks().test();
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
            .onChunk(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnChunksThenGettingBodyMultipleTimes() {
        final TestObserver<Buffer> obs = cut
            .onChunk(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingChunksMultipleTimes() {
        final TestSubscriber<Buffer> obs = cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .ignoreElements()
            .andThen(cut.chunks())
            .test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

        assertEquals(1, subscriptionCount.get());
    }

    @Test
    void shouldSubscribeOnceWhenUsingOnBodyThenGettingBodyMultipleTimes() {
        final TestObserver<Buffer> obs = cut
            .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .ignoreElement()
            .andThen(cut.body())
            .test();

        obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
        assertEquals(1, subscriptionCount.get());
    }
}
