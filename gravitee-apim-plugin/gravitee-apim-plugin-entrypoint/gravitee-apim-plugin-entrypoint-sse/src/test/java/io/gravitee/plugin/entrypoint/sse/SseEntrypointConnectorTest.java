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
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.entrypoint.sse.configuration.SseEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import java.util.Map;
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
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(response.end()).thenReturn(Completable.complete());
        cut = new SseEntrypointConnector(null);
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
        verifyNoInteractions(ctx);
    }

    @Test
    void shouldWriteSseMessages() {
        final Flowable<Message> messages = Flowable.just(new DefaultMessage("1"), new DefaultMessage("2"), new DefaultMessage("3"));
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
        chunkObs.assertValueAt(1, message -> message.toString().matches("id: .*\nevent: message\ndata: 1\n\n"));
        chunkObs.assertValueAt(2, message -> message.toString().matches("id: .*\nevent: message\ndata: 2\n\n"));
        chunkObs.assertValueAt(3, message -> message.toString().matches("id: .*\nevent: message\ndata: 3\n\n"));
    }

    @Test
    void shouldWriteSseMessagesWithoutCommentsWhenDisabled() {
        final Flowable<Message> messages = Flowable.just(
            new DefaultMessage("1")
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
        chunkObs.assertValueAt(1, message -> message.toString().matches("id: .*\nevent: message\ndata: 1\n\n"));
    }

    @Test
    void shouldWriteSseMessagesWithCommentsWhenEnabled() {
        final SseEntrypointConnectorConfiguration configuration = new SseEntrypointConnectorConfiguration();
        configuration.setHeadersAsComment(true);
        configuration.setMetadataAsComment(true);

        final Flowable<Message> messages = Flowable.just(
            new DefaultMessage("1")
                .headers(HttpHeaders.create().add("HeaderName", "HeaderValue"))
                .metadata(Map.of("MetadataName", "MetadataValue"))
        );
        final HttpHeaders httpHeaders = HttpHeaders.create();

        when(response.messages()).thenReturn(messages);
        when(response.headers()).thenReturn(httpHeaders);
        when(ctx.response()).thenReturn(response);

        cut = new SseEntrypointConnector(configuration);
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
                message.toString().matches("id: .*\nevent: message\ndata: 1\n:MetadataName: MetadataValue\n:HeaderName: HeaderValue\n\n")
        );
    }

    @Test
    void shouldWriteSseErrorMessage() {
        final Flowable<Message> messages = Flowable
            .<Message>just(new DefaultMessage("1"))
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

        chunkObs.assertComplete();
        chunkObs.assertValueCount(3);
        chunkObs.assertValueAt(0, message -> message.toString().startsWith("retry: "));
        chunkObs.assertValueAt(1, message -> message.toString().matches("id: .*\nevent: message\ndata: 1\n\n"));
        chunkObs.assertValueAt(2, message -> message.toString().matches("id: .*\nevent: error\ndata: MOCK EXCEPTION\n\n"));
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

    private boolean ignoreHeartbeat(final Buffer buffer) {
        return !buffer.toString().equals(":\n\n");
    }

    private void verifyResponseHeaders(HttpHeaders httpHeaders) {
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportSubscribeModeOnly() {
        assertThat(cut.supportedModes()).containsExactly(ConnectorMode.SUBSCRIBE);
    }
}
