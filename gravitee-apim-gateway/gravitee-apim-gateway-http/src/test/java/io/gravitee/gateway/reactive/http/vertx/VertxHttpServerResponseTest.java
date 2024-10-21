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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
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
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class VertxHttpServerResponseTest {

    protected static final String NEW_CHUNK = "New chunk";
    protected static final String MOCK_EXCEPTION = "Mock exception";
    protected static final String BODY = "chunk1chunk2chunk3";

    @Mock
    private HttpServerResponse httpServerResponse;

    @Mock
    private VertxHttpServerRequest request;

    @Mock
    private GenericExecutionContext ctx;

    @Mock
    private HttpServerRequest httpServerRequest;

    @Mock
    private Metrics metrics;

    @Captor
    private ArgumentCaptor<Flowable<io.vertx.rxjava3.core.buffer.Buffer>> chunksCaptor;

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
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.complete());

        ReflectionTestUtils.setField(request, "nativeRequest", httpServerRequest);

        cut = new VertxHttpServerResponse(request);
        cut.chunks(chunks);
    }

    @Nested
    class ChunkSubscriptionTest {

        @Test
        void should_subscribe_once_when_ignoring_and_replacing_existing_chunks() {
            cut.chunks(cut.chunks().ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))));
            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_replacing_existing_chunks_with_body() {
            cut
                .chunks()
                .ignoreElements()
                .andThen(Completable.fromRunnable(() -> cut.body(Buffer.buffer(NEW_CHUNK))))
                .andThen(Completable.defer(() -> cut.end(ctx)))
                .test()
                .assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_not_subscribe_on_existing_chunks_when_just_replacing_existing_body() {
            // Note: never do that unless you really know what you are doing.
            cut.body(Buffer.buffer(NEW_CHUNK));
            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

            // Existing chunks are not consumed at all (not subscribed).
            assertEquals(0, subscriptionCount.get());
        }

        @Test
        void should_not_subscribe_on_existing_chunks_when_just_replacing_existing_chunks() {
            // Note: never do that unless you really know what you are doing.
            cut.chunks(Flowable.just(Buffer.buffer(NEW_CHUNK)));
            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));

            // Existing chunks are not consumed at all (not subscribed).
            assertEquals(0, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_on_chunks_then_getting_chunks_multiple_times() {
            cut
                .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_on_chunks_then_getting_body_multiple_times() {
            cut
                .onChunks(chunks -> chunks.ignoreElements().andThen(Flowable.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_getting_body_multiple_times() {
            cut
                .onChunks(chunks -> chunks)
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_on_body_then_getting_chunks_multiple_times() {
            cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .ignoreElements()
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_and_return_empty_when_using_on_body_then_getting_chunks_multiple_times() {
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
        void should_return_empty_buffer_when_chunks_is_null() {
            mockWithNull();

            final TestObserver<Buffer> obs = cut.bodyOrEmpty().test();
            obs.assertValue(buffer -> "".equals(buffer.toString()));
        }

        @Test
        void should_return_empty_buffer_when_body_or_empty() {
            mockWithEmpty();

            final TestObserver<Buffer> obs = cut.bodyOrEmpty().test();
            obs.assertValue(buffer -> "".equals(buffer.toString()));
        }

        @Test
        void should_subscribe_once_and_return_empty_observable_when_body_or_empty_then_getting_chunks() {
            mockWithEmpty();

            final TestSubscriber<Buffer> obs = cut.bodyOrEmpty().ignoreElement().andThen(Flowable.defer(() -> cut.chunks())).test();

            obs.assertNoValues();
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_on_body() {
            mockWithError();

            final TestSubscriber<Buffer> obs = cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Flowable.defer(() -> cut.chunks()))
                .test();

            obs.assertError(throwable -> MOCK_EXCEPTION.equals(throwable.getMessage()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_error_occurs_and_using_on_chunks() {
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
        void should_subscribe_once_when_using_on_body_then_getting_body_multiple_times() {
            cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .ignoreElement()
                .andThen(Maybe.defer(() -> cut.body()))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_subscribe_once_when_using_on_body_multiple_times() {
            cut
                .onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(NEW_CHUNK))))
                .andThen(Completable.defer(() -> cut.onBody(body -> body.ignoreElement().andThen(Maybe.just(Buffer.buffer(BODY))))))
                .test()
                .assertComplete();

            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_not_subscribe_and_complete_when_request_is_web_socket() {
            when(request.isWebSocketUpgraded()).thenReturn(true);

            final TestObserver<Void> obs = cut.end(ctx).test();

            obs.assertComplete();
            verify(httpServerResponse, times(0)).rxSend(any(Flowable.class));
            verify(httpServerResponse, times(0)).rxEnd();
        }
    }

    @Nested
    class StreamingRequestTest {

        @Mock
        private MockedStatic<RequestUtils> requestUtilsMockedStatic;

        @Test
        void should_check_streaming_only_once() {
            requestUtilsMockedStatic
                .when(() -> RequestUtils.isStreaming(any(VertxHttpServerRequest.class), any(Response.class)))
                .thenAnswer(invocation -> true);

            for (int i = 0; i < 2; i++) {
                assertTrue(cut.isStreaming());
            }

            requestUtilsMockedStatic.verify(
                () -> RequestUtils.isStreaming(any(VertxHttpServerRequest.class), any(Response.class)),
                times(1)
            );
        }
    }

    @Nested
    class MessagesInterceptorTest {

        @Test
        void should_set_on_messages_interceptor() {
            final MessageFlow<Message> messageFlow = mock(MessageFlow.class);
            ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);
            final OnMessagesInterceptor.MessagesInterceptor interceptor = mock(OnMessagesInterceptor.MessagesInterceptor.class);
            cut.registerMessagesInterceptor(interceptor);

            verify(messageFlow).registerMessagesInterceptor(interceptor);
        }

        @Test
        void should_unset_on_messages_interceptor() {
            final MessageFlow<Message> messageFlow = mock(MessageFlow.class);
            ReflectionTestUtils.setField(cut, "messageFlow", messageFlow);

            cut.unregisterMessagesInterceptor("id");

            verify(messageFlow).unregisterMessagesInterceptor("id");
        }
    }

    @Nested
    class MetricsTest {

        @Test
        void should_set_metrics_content_length_when_ending_response() {
            cut.body(Buffer.buffer(BODY));
            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.rxjava3.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));

            verify(metrics).setResponseContentLength(BODY.length());
        }
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
