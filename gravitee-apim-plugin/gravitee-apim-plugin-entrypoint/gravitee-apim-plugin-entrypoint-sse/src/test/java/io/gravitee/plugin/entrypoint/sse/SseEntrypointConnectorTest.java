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
package io.gravitee.plugin.entrypoint.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.sse.configuration.SseEntrypointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SseEntrypointConnectorTest {

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    private SseEntrypointConnector cut;

    @BeforeEach
    void beforeEach() {
        lenient().when(request.headers()).thenReturn(HttpHeaders.create());
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(response.end(ctx)).thenReturn(Completable.complete());
        cut = new SseEntrypointConnector(Qos.NONE, null);
    }

    @Test
    void shouldIdReturnSse() {
        assertThat(cut.id()).isEqualTo("sse");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportSubscribeModeOnly() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(cut.matchCriteriaCount()).isEqualTo(2);
    }

    @Test
    void shouldMatchesWithValidContext() {
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.ACCEPT, "text/event-stream");
        when(request.headers()).thenReturn(httpHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadContentType() {
        when(ctx.request()).thenReturn(request);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(request.headers()).thenReturn(httpHeaders);
        when(request.method()).thenReturn(HttpMethod.GET);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        when(ctx.request()).thenReturn(request);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        when(request.headers()).thenReturn(httpHeaders);
        when(request.method()).thenReturn(HttpMethod.POST);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteWithoutDoingAnything() {
        cut.handleRequest(ctx).test().assertComplete();
    }

    @Test
    void shouldNotUpdateContextWithLastEventIdHeader() {
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set("Last-Event-ID", "1");
        lenient().when(request.headers()).thenReturn(httpHeaders);
        cut.handleRequest(ctx).test().assertComplete();

        verifyNoInteractions(ctx);
    }

    @Test
    void shouldWriteSseMessages() {
        final Flowable<Message> messages = Flowable.just(
            new DefaultMessage("content 1").id("1"),
            new DefaultMessage("content 2").id("2"),
            new DefaultMessage("content 3").id("3")
        );
        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verifyResponseHeaders(httpHeaders);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();

        chunkObs.assertComplete();
        chunkObs.assertValueCount(4);
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
        chunkObs.assertValueAt(1, message -> message.toString().equals("id: 1\nevent: message\ndata: content 1\n\n"));
        chunkObs.assertValueAt(2, message -> message.toString().equals("id: 2\nevent: message\ndata: content 2\n\n"));
        chunkObs.assertValueAt(3, message -> message.toString().equals("id: 3\nevent: message\ndata: content 3\n\n"));
    }

    @Test
    void shouldWriteSseHeartBeatMessages() {
        try {
            // Set up the test scheduler globally to get fine control on time.
            final TestScheduler testScheduler = new TestScheduler();
            RxJavaPlugins.setComputationSchedulerHandler(s -> testScheduler);

            // Set heart beat each 1s.
            final SseEntrypointConnectorConfiguration configuration = new SseEntrypointConnectorConfiguration();
            configuration.setHeartbeatIntervalInMs(1000);

            // Fake a message emission each 8s. Should take 26s to produce all 3 messages.
            final Flowable<Message> messages = Flowable
                .just(
                    new DefaultMessage("content 1").id("1"),
                    new DefaultMessage("content 2").id("2"),
                    new DefaultMessage("content 3").id("3")
                )
                .zipWith(Flowable.interval(8000, TimeUnit.MILLISECONDS), (message, aLong) -> message);

            final HttpHeaders httpHeaders = HttpHeaders.create();

            when(response.messages()).thenReturn(messages);
            when(response.headers()).thenReturn(httpHeaders);
            when(ctx.response()).thenReturn(response);

            cut = new SseEntrypointConnector(Qos.NONE, configuration);
            final TestObserver<Void> obs = cut.handleResponse(ctx).test();
            obs.assertComplete();

            verifyResponseHeaders(httpHeaders);
            verify(response).chunks(chunksCaptor.capture());

            final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();

            chunkObs.assertNotComplete();

            // Retry message is produced instantaneously (0s).
            chunkObs.assertValueAt(0, buffer -> buffer.toString().contains("retry"));

            for (int i = 1; i < 8; i++) {
                // Advance time second per second should produce heartbeat messages (7s).
                testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
                testScheduler.triggerActions();
                chunkObs.assertValueAt(i, buffer -> buffer.toString().equals(":\n\n"));
            }

            // At the 8th second, the first message is produced (8s).
            testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();
            chunkObs.assertValueAt(8, message -> message.toString().equals("id: 1\nevent: message\ndata: content 1\n\n"));

            // Jump to 10s in the future should produce 7 heartbeat and one next message (18s).
            testScheduler.advanceTimeBy(10000, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();

            for (int i = 9; i < 16; i++) {
                chunkObs.assertValueAt(i, buffer -> buffer.toString().equals(":\n\n"));
            }

            chunkObs.assertValueAt(16, message -> message.toString().equals("id: 2\nevent: message\ndata: content 2\n\n"));

            // Advance time by 7 seconds with no message should produce 7 heartbeats (25s).
            testScheduler.advanceTimeBy(7000, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();

            for (int i = 17; i < 24; i++) {
                chunkObs.assertValueAt(i, buffer -> buffer.toString().equals(":\n\n"));
            }

            // Advance time by 1 second more should produce the last message (26s).
            testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
            testScheduler.triggerActions();

            // The last message is emitted and should complete.
            chunkObs.assertValueAt(24, message -> message.toString().equals("id: 3\nevent: message\ndata: content 3\n\n"));

            chunkObs.assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    void shouldWriteSseMessagesWithoutCommentsWhenDisabled() {
        final Flowable<Message> messages = Flowable.just(
            new DefaultMessage("content 1")
                .id("1")
                .headers(HttpHeaders.create().add("HeaderName", "HeaderValue"))
                .metadata(Map.of("MetadataName", "MetadataValue"))
        );
        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verifyResponseHeaders(httpHeaders);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();

        chunkObs.assertComplete();
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
        chunkObs.assertValueAt(1, message -> message.toString().equals("id: 1\nevent: message\ndata: content 1\n\n"));
    }

    @Test
    void shouldWriteSseMessagesWithCommentsWhenEnabled() {
        final SseEntrypointConnectorConfiguration configuration = new SseEntrypointConnectorConfiguration();
        configuration.setHeadersAsComment(true);
        configuration.setMetadataAsComment(true);

        final Flowable<Message> messages = Flowable.just(
            new DefaultMessage("content 1")
                .id("1")
                .headers(HttpHeaders.create().add("HeaderName", "HeaderValue"))
                .metadata(Map.of("MetadataName", "MetadataValue"))
        );
        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        cut = new SseEntrypointConnector(Qos.NONE, configuration);
        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verifyResponseHeaders(httpHeaders);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();

        chunkObs.assertComplete();
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
        chunkObs.assertValueAt(
            1,
            message ->
                message
                    .toString()
                    .equals("id: 1\nevent: message\ndata: content 1\n:MetadataName: MetadataValue\n:HeaderName: HeaderValue\n\n")
        );
    }

    @Test
    void shouldWriteSseErrorMessage() {
        final Flowable<Message> messages = Flowable
            .<Message>just(new DefaultMessage("content 1").id("1"))
            .concatWith(Flowable.error(new RuntimeException("MOCK EXCEPTION")));

        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verifyResponseHeaders(httpHeaders);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();

        chunkObs.assertValueCount(3);
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
        chunkObs.assertValueAt(1, message -> message.toString().equals("id: 1\nevent: message\ndata: content 1\n\n"));
        chunkObs.assertValueAt(2, message -> message.toString().matches("id: .*\nevent: error\ndata: MOCK EXCEPTION\n\n"));
        chunkObs.assertComplete();
    }

    @Test
    void shouldCompleteWithEmptyResponse() {
        final Flowable<Message> messages = Flowable.empty();

        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verifyResponseHeaders(httpHeaders);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();

        chunkObs.assertComplete();
        chunkObs.assertValueCount(1);
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
    }

    @Test
    void shouldWriteErrorSseEventWhenErrorOccurs() {
        Flowable<Message> messages = Flowable.error(new RuntimeException("error"));
        when(response.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);
        cut.handleResponse(ctx).test().assertComplete();
        verifyResponseHeaders(httpHeaders);
    }

    @Test
    void shouldWriteGoAwaySseEventWhenStopping() throws Exception {
        final TestScheduler testScheduler = new TestScheduler();

        final DefaultMessage message = new DefaultMessage("test");
        when(response.messages())
            .thenReturn(
                Flowable
                    .<Message>just(message, message, message)
                    .zipWith(Flowable.interval(1000, TimeUnit.MILLISECONDS, testScheduler), (m, aLong) -> m)
            );
        when(response.headers()).thenReturn(HttpHeaders.create());
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().filter(this::ignoreHeartbeat).test();
        chunkObs.assertNotComplete();
        chunkObs.assertValueAt(0, buffer -> buffer.toString().startsWith("retry: "));

        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        chunkObs.assertValueAt(1, buffer -> buffer.toString().matches("event: message\ndata: test\n\n"));

        testScheduler.advanceTimeBy(1000, TimeUnit.MILLISECONDS);
        chunkObs.assertValueAt(2, buffer -> buffer.toString().matches("event: message\ndata: test\n\n"));

        cut.preStop();
        chunkObs.assertComplete();
        chunkObs.assertValueAt(3, buffer -> buffer.toString().equals("event: goaway\ndata: Stopping, please reconnect\n\n"));
    }

    private boolean ignoreHeartbeat(final Buffer buffer) {
        return !buffer.toString().equals(":\n\n");
    }

    private void verifyResponseHeaders(HttpHeaders httpHeaders) {
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
    }
}
