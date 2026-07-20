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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.reactive.api.context.GenericExecutionContext;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.gravitee.reporter.api.v4.metric.Metrics;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpClosedException;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpConnection;
import io.vertx.rxjava3.core.http.HttpHeaders;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.http.HttpServerResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import org.slf4j.helpers.NOPLogger;
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
    private HttpConnection httpConnection;

    @Mock
    private Metrics metrics;

    @Captor
    private ArgumentCaptor<Flowable<io.vertx.core.buffer.Buffer>> chunksCaptor;

    private AtomicInteger subscriptionCount;
    private VertxHttpServerResponse cut;

    @BeforeEach
    void init() {
        subscriptionCount = new AtomicInteger(0);
        Flowable<Buffer> chunks = Flowable.just(Buffer.buffer("chunk1"), Buffer.buffer("chunk2"), Buffer.buffer("chunk3")).doOnSubscribe(
            subscription -> subscriptionCount.incrementAndGet()
        );

        when(httpServerResponse.headers()).thenReturn(HttpHeaders.headers());
        when(httpServerResponse.trailers()).thenReturn(HttpHeaders.headers());
        when(httpServerRequest.response()).thenReturn(httpServerResponse);
        lenient().when(ctx.metrics()).thenReturn(metrics);
        lenient().when(ctx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);
        lenient().when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.complete());
        lenient().when(httpServerResponse.rxEnd()).thenReturn(Completable.complete());

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> NEW_CHUNK.equals(buffer.toString()));
            assertEquals(1, subscriptionCount.get());
        }

        @Test
        void should_not_subscribe_on_existing_chunks_when_just_replacing_existing_body() {
            // Note: never do that unless you really know what you are doing.
            cut.body(Buffer.buffer(NEW_CHUNK));
            cut.end(ctx).test().assertComplete();

            verify(httpServerResponse).rxSend(chunksCaptor.capture());

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();
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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

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

            final TestSubscriber<io.vertx.core.buffer.Buffer> obs = chunksCaptor.getValue().test();

            obs.assertValue(buffer -> BODY.equals(buffer.toString()));

            verify(metrics).setResponseContentLength(BODY.length());
        }

        @Test
        void should_record_client_close_reason_when_response_write_fails_and_preserve_committed_status() {
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.error(new IOException("Connection reset by peer")));
            cut.body(Buffer.buffer(BODY));

            // The write failure is swallowed (the dispatch must end normally) but recorded on the metrics.
            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(metrics).setErrorKey(ClientCloseClassifier.CLIENT_ABORTED_TCP_RESET);
            verify(metrics).setFailure(any());
            // Mirror pattern: the committed HTTP status is never rewritten by the classification.
            verify(httpServerResponse, never()).setStatusCode(anyInt());
        }
    }

    @Nested
    class ResponseStreamFailureTest {

        @Test
        void should_build_500_response_stream_failure_with_cause() {
            IllegalStateException cause = new IllegalStateException("response translation failed");

            var failure = VertxHttpServerResponse.responseStreamFailure(cause);

            assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, failure.statusCode());
            assertEquals(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED, failure.key());
            assertEquals(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED_MESSAGE, failure.message());
            assertSame(cause, failure.cause());
        }

        @Test
        void should_close_http_1_connection_when_response_stream_fails() {
            IOException failure = new IOException("No space left on device");
            CompletableSubject close = CompletableSubject.create();
            Metrics responseMetrics = Metrics.builder().build();
            when(ctx.metrics()).thenReturn(responseMetrics);
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
            when(httpServerRequest.connection()).thenReturn(httpConnection);
            when(httpConnection.rxClose()).thenReturn(close);
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(Completable.error(failure));

            TestObserver<Void> end = cut.end(ctx).test().assertNotComplete().assertNoErrors();
            close.onComplete();
            end.assertComplete().assertNoErrors();

            verify(httpConnection).rxClose();
            verify(httpServerResponse, never()).rxReset(anyLong());
            verify(httpServerResponse, never()).rxEnd();
            assertEquals(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED, responseMetrics.getErrorKey());
            assertEquals(
                VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED_MESSAGE + " (No space left on device)",
                responseMetrics.getErrorMessage()
            );
            assertEquals(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED, responseMetrics.getFailure().getKey());
        }

        @Test
        void should_reset_http_2_stream_when_response_stream_fails() {
            CompletableSubject reset = CompletableSubject.create();
            when(request.version()).thenReturn(HttpVersion.HTTP_2);
            when(httpServerResponse.rxReset(0x2L)).thenReturn(reset);
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new IllegalStateException("response translation failed"))
            );

            TestObserver<Void> end = cut.end(ctx).test().assertNotComplete().assertNoErrors();
            reset.onComplete();
            end.assertComplete().assertNoErrors();

            verify(httpServerResponse).rxReset(0x2L);
            verify(httpServerRequest, never()).connection();
            verify(httpServerResponse, never()).rxEnd();
        }

        @Test
        void should_not_abort_again_when_wrapped_failure_is_a_client_close() {
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new RuntimeException("write failed", new IOException("Broken pipe")))
            );

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpServerRequest, never()).connection();
            verify(httpServerResponse, never()).rxReset(anyLong());
            verify(metrics).setErrorKey(ClientCloseClassifier.CLIENT_ABORTED_BROKEN_PIPE);
        }

        @Test
        void should_abort_http_1_when_chunk_source_failure_looks_like_client_close() {
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
            when(httpServerRequest.connection()).thenReturn(httpConnection);
            when(httpConnection.rxClose()).thenReturn(Completable.complete());
            when(httpServerResponse.rxSend(any(Flowable.class))).thenAnswer(invocation -> {
                Flowable<?> chunks = invocation.getArgument(0);
                return chunks.ignoreElements();
            });
            cut.chunks(Flowable.error(new IOException("Broken pipe")));

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpConnection).rxClose();
            verify(metrics).setErrorKey(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED);
            verify(metrics, never()).setErrorKey(ClientCloseClassifier.CLIENT_ABORTED_BROKEN_PIPE);
        }

        @Test
        void should_abort_http_2_when_chunk_source_failure_looks_like_client_close() {
            when(request.version()).thenReturn(HttpVersion.HTTP_2);
            when(httpServerResponse.rxReset(0x2L)).thenReturn(Completable.complete());
            when(httpServerResponse.rxSend(any(Flowable.class))).thenAnswer(invocation -> {
                Flowable<?> chunks = invocation.getArgument(0);
                return chunks.ignoreElements();
            });
            cut.chunks(Flowable.error(new HttpClosedException("Connection was closed")));

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpServerResponse).rxReset(0x2L);
            verify(metrics).setErrorKey(VertxHttpServerResponse.GATEWAY_RESPONSE_STREAM_FAILED);
            verify(metrics, never()).setErrorKey(ClientCloseClassifier.CLIENT_ABORTED_CHANNEL_CLOSED);
        }

        @Test
        void should_not_report_undeliverable_error_when_disposed_while_abort_is_pending() {
            List<Throwable> pluginErrors = new CopyOnWriteArrayList<>();
            RxJavaPlugins.setErrorHandler(pluginErrors::add);
            try {
                var close = CompletableSubject.create();
                when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
                when(httpServerRequest.connection()).thenReturn(httpConnection);
                when(httpConnection.rxClose()).thenReturn(close);
                // Fail before subscribing to the chunk source: cancellation must tolerate a null subscription.
                when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                    Completable.error(new IllegalStateException("native send failed before subscription"))
                );

                var end = cut.end(ctx).test().assertNotComplete().assertNoErrors();
                assertEquals(0, subscriptionCount.get());
                end.dispose();
                close.onComplete();

                assertTrue(pluginErrors.isEmpty(), () -> "Unexpected RxJava plugin errors: " + pluginErrors);
            } finally {
                RxJavaPlugins.reset();
            }
        }

        @Test
        void should_truncate_raw_http_1_transfer_after_observed_chunk_when_source_fails() throws Exception {
            var vertx = Vertx.vertx();
            var tail = PublishProcessor.<Buffer>create().toSerialized();
            var responseFailure = new AtomicReference<Throwable>();
            var rawSocketFailure = new AtomicReference<Throwable>();
            var rawResponse = new StringBuffer();
            var responseClosed = new CountDownLatch(1);
            var firstChunkObserved = new AtomicBoolean();
            var rawCtx = mock(GenericExecutionContext.class);
            when(rawCtx.metrics()).thenReturn(Metrics.builder().build());
            when(rawCtx.withLogger(any())).thenReturn(NOPLogger.NOP_LOGGER);

            io.vertx.rxjava3.core.http.HttpServer server = null;
            io.vertx.rxjava3.core.net.NetClient client = null;
            io.vertx.rxjava3.core.net.NetSocket socket = null;
            try {
                server = vertx
                    .createHttpServer(new HttpServerOptions().setHost("127.0.0.1").setPort(0))
                    .requestHandler(nativeRequest -> {
                        ((HttpServerConnection) nativeRequest.connection().getDelegate()).channelHandlerContext()
                            .channel()
                            .attr(AttributeKey.valueOf(VertxHttpServerRequest.NETTY_ATTR_CONNECTION_TIME))
                            .set(System.currentTimeMillis());
                        var gatewayRequest = new VertxHttpServerRequest(nativeRequest, () -> "raw-http-1-stream-failure");
                        var gatewayResponse = gatewayRequest.response();
                        gatewayResponse.chunks(Flowable.just(Buffer.buffer("first")).concatWith(tail));
                        gatewayResponse.end(rawCtx).subscribe(() -> {}, responseFailure::set);
                    })
                    .listen()
                    .blockingGet();

                client = vertx.createNetClient();
                socket = client.connect(server.actualPort(), "127.0.0.1").blockingGet();
                socket
                    .handler(buffer -> {
                        rawResponse.append(buffer.toString(StandardCharsets.ISO_8859_1));
                        if (rawResponse.indexOf("first") >= 0 && firstChunkObserved.compareAndSet(false, true)) {
                            tail.onError(new IllegalStateException("response translation failed"));
                        }
                    })
                    .exceptionHandler(failure -> {
                        rawSocketFailure.set(failure);
                        responseClosed.countDown();
                    })
                    .closeHandler(ignored -> responseClosed.countDown());
                socket.write("GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n").blockingAwait();

                assertTrue(
                    responseClosed.await(5, TimeUnit.SECONDS),
                    () -> "Raw HTTP/1 connection did not close; socket failure=" + rawSocketFailure.get()
                );
                assertTrue(responseFailure.get() == null, () -> "Gateway response completion failed: " + responseFailure.get());
                assertTrue(firstChunkObserved.get(), "Client did not observe the first response chunk");
                assertTrue(rawResponse.toString().toLowerCase(Locale.ROOT).contains("transfer-encoding: chunked"));
                assertTrue(rawResponse.toString().contains("first"));
                assertTrue(
                    !rawResponse.toString().contains("0\r\n\r\n"),
                    () -> "Truncated response unexpectedly contained a terminal chunk: " + rawResponse
                );
            } finally {
                if (socket != null) {
                    socket.close().onErrorComplete().blockingAwait();
                }
                if (client != null) {
                    client.close().onErrorComplete().blockingAwait();
                }
                if (server != null) {
                    server.close().onErrorComplete().blockingAwait();
                }
                vertx.close().onErrorComplete().blockingAwait();
            }
        }

        @Test
        void should_keep_existing_failure_when_response_stream_fails() {
            Metrics existingMetrics = Metrics.builder().build();
            existingMetrics.setErrorKey("GATEWAY_POLICY_INTERNAL_ERROR");
            when(ctx.metrics()).thenReturn(existingMetrics);
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
            when(httpServerRequest.connection()).thenReturn(httpConnection);
            when(httpConnection.rxClose()).thenReturn(Completable.complete());
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new IllegalStateException("response translation failed"))
            );

            cut.end(ctx).test().assertComplete().assertNoErrors();

            assertEquals("GATEWAY_POLICY_INTERNAL_ERROR", existingMetrics.getErrorKey());
            verify(httpConnection).rxClose();
        }

        @Test
        void should_complete_cleanup_when_http_1_abort_fails() {
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);
            when(httpServerRequest.connection()).thenReturn(httpConnection);
            when(httpConnection.rxClose()).thenReturn(Completable.error(new IOException("connection already closed")));
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new IllegalStateException("response translation failed"))
            );

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpConnection).rxClose();
        }

        @Test
        void should_complete_cleanup_when_http_2_reset_fails() {
            when(request.version()).thenReturn(HttpVersion.HTTP_2);
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new IllegalStateException("response translation failed"))
            );
            when(httpServerResponse.rxReset(0x2L)).thenReturn(Completable.error(new IllegalStateException("stream already closed")));

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpServerResponse).rxReset(0x2L);
        }

        @Test
        void should_complete_cleanup_when_http_2_reset_throws() {
            when(request.version()).thenReturn(HttpVersion.HTTP_2);
            when(httpServerResponse.rxSend(any(Flowable.class))).thenReturn(
                Completable.error(new IllegalStateException("response translation failed"))
            );
            when(httpServerResponse.rxReset(0x2L)).thenThrow(new IllegalStateException("stream already closed"));

            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpServerResponse).rxReset(0x2L);
        }

        @Test
        void should_not_abort_successful_or_empty_response_streams() {
            when(request.version()).thenReturn(HttpVersion.HTTP_1_1);

            cut.end(ctx).test().assertComplete().assertNoErrors();
            cut = new VertxHttpServerResponse(request);
            cut.chunks(Flowable.empty());
            cut.end(ctx).test().assertComplete().assertNoErrors();
            cut = new VertxHttpServerResponse(request);
            cut.chunks(null);
            cut.end(ctx).test().assertComplete().assertNoErrors();

            verify(httpServerRequest, never()).connection();
            verify(httpServerResponse, never()).rxReset(anyLong());
            verify(httpServerResponse).rxEnd();
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
        final Flowable<Buffer> chunks = Flowable.<Buffer>error(new RuntimeException(MOCK_EXCEPTION)).doOnSubscribe(subscription ->
            subscriptionCount.incrementAndGet()
        );

        cut = new VertxHttpServerResponse(request);
        cut.chunks(chunks);
    }
}
