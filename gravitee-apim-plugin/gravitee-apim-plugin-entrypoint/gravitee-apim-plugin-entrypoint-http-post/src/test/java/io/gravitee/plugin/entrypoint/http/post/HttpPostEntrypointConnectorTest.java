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
package io.gravitee.plugin.entrypoint.http.post;

import static io.gravitee.common.http.HttpHeadersValues.CONNECTION_CLOSE;
import static io.gravitee.common.http.HttpHeadersValues.CONNECTION_GO_AWAY;
import static io.gravitee.gateway.jupiter.api.connector.entrypoint.async.EntrypointAsyncConnector.STOP_MESSAGE_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.plugin.entrypoint.http.post.configuration.HttpPostEntrypointConnectorConfiguration;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HttpPostEntrypointConnectorTest {

    @Mock
    private ExecutionContext ctx;

    private HttpPostEntrypointConnector cut;
    private DummyMessageRequest dummyRequest;

    @Mock
    private Response response;

    private HttpHeaders responseHeaders;

    @BeforeEach
    void beforeEach() {
        dummyRequest = new DummyMessageRequest();
        dummyRequest.messages(Flowable.empty());
        lenient().when(ctx.request()).thenReturn(dummyRequest);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(response.headers()).thenReturn(HttpHeaders.create());
        lenient().when(response.messages()).thenReturn(Flowable.empty());
        cut = new HttpPostEntrypointConnector(Qos.NONE, null);
    }

    @Test
    void shouldIdReturnHttpPost() {
        assertThat(cut.id()).isEqualTo("http-post");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportPublishModeOnly() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.PUBLISH);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(cut.matchCriteriaCount()).isEqualTo(1);
    }

    @Test
    void shouldMatchesWithValidContext() {
        dummyRequest.method(HttpMethod.POST);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        dummyRequest.method(HttpMethod.GET);

        boolean matches = cut.matches(ctx);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteAndSetRequestBuffersInRequestMessages() {
        dummyRequest.headers(HttpHeaders.create());
        dummyRequest.body(Maybe.just(Buffer.buffer("bodyContent")));
        cut.handleRequest(ctx).test().assertComplete();

        dummyRequest.messages().test().assertValue(message -> "bodyContent".equals(message.content().toString()));
    }

    @Test
    void shouldAddRequestHeadersInMessageWhenConfigured() {
        final HttpHeaders headers = HttpHeaders.create();
        headers.add("X-Header", "X-Value");
        dummyRequest.headers(headers);
        dummyRequest.body(Maybe.just(Buffer.buffer("bodyContent")));

        HttpPostEntrypointConnectorConfiguration configuration = new HttpPostEntrypointConnectorConfiguration();
        configuration.setRequestHeadersToMessage(true);

        cut = new HttpPostEntrypointConnector(Qos.AUTO, configuration);
        cut.handleRequest(ctx).test().assertComplete();

        dummyRequest
            .messages()
            .test()
            .assertValue(message -> "bodyContent".equals(message.content().toString()) && message.headers().deeplyEquals(headers));
    }

    @Test
    void shouldCompleteWhenMessagesComplete() {
        cut.handleResponse(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertComplete();
        verify(response).status(HttpResponseStatus.ACCEPTED.code());
        verify(response).reason(HttpResponseStatus.ACCEPTED.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsFullError() {
        responseHeaders = HttpHeaders.create();
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("statusCode", HttpResponseStatus.NOT_FOUND.code());
        when(response.headers()).thenReturn(responseHeaders);
        when(response.messages())
            .thenReturn(
                Flowable.just(
                    DefaultMessage
                        .builder()
                        .error(true)
                        .metadata(metadata)
                        .headers(
                            HttpHeaders
                                .create()
                                .set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                                .set(HttpHeaderNames.CONTENT_LENGTH, "5")
                        )
                        .content(Buffer.buffer("error"))
                        .build()
                )
            );
        cut.handleResponse(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals("error"));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        verify(response).status(HttpResponseStatus.NOT_FOUND.code());
        verify(response).reason(HttpResponseStatus.NOT_FOUND.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsError() {
        responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);
        when(response.messages()).thenReturn(Flowable.just(DefaultMessage.builder().error(true).content(Buffer.buffer("error")).build()));
        cut.handleResponse(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals("error"));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
        verify(response).status(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
        verify(response).reason(HttpResponseStatus.SERVICE_UNAVAILABLE.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsErrorWithBody() {
        responseHeaders = HttpHeaders.create();
        when(response.messages()).thenReturn(Flowable.just(DefaultMessage.builder().error(true).build()));
        cut.handleResponse(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals(""));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
        verify(response).status(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
        verify(response).reason(HttpResponseStatus.SERVICE_UNAVAILABLE.reasonPhrase());
    }

    @Test
    void shouldCompleteWithStopMessageWhenStoppingDuringRequest() {
        responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);

        final TestScheduler testScheduler = new TestScheduler();

        dummyRequest.body(Maybe.just(Buffer.buffer("test")).delay(1000, TimeUnit.MILLISECONDS, testScheduler));
        when(response.messages()).thenReturn(Flowable.<Message>empty().delay(2000, TimeUnit.MILLISECONDS, testScheduler));
        when(ctx.interruptMessagesWith(any(ExecutionFailure.class))).thenReturn(Flowable.just(new DefaultMessage("Stop")));

        final TestObserver<Void> obs = cut.handleRequest(ctx).mergeWith(cut.handleResponse(ctx)).test();

        // When merging request and response, it should not complete because response is not yet completed.
        obs.assertNotComplete();

        // Trigger stop.
        cut.doStop();

        // Should have completed because request has been interrupted by the stop.
        obs.assertComplete();

        // Should inform that the connection has been closed.
        assertThat(responseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(CONNECTION_CLOSE);
    }

    @Test
    void shouldCompleteWithStopMessageAndConnectionGoAwayHeaderWhenHTTP2() {
        responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);

        final TestScheduler testScheduler = new TestScheduler();

        dummyRequest.setHttpVersion(HttpVersion.HTTP_2);
        dummyRequest.body(Maybe.just(Buffer.buffer("test")).delay(1000, TimeUnit.MILLISECONDS, testScheduler));
        when(response.messages()).thenReturn(Flowable.<Message>empty().delay(2000, TimeUnit.MILLISECONDS, testScheduler));
        when(ctx.interruptMessagesWith(any(ExecutionFailure.class))).thenReturn(Flowable.just(new DefaultMessage("Stop")));

        final TestObserver<Void> obs = cut.handleRequest(ctx).mergeWith(cut.handleResponse(ctx)).test();

        // When merging request and response, it should not complete because response is not yet completed.
        obs.assertNotComplete();

        // Trigger stop.
        cut.doStop();

        // Should have completed because request has been interrupted by the stop.
        obs.assertComplete();

        // Should inform that the http2 connection must be shutdown.
        assertThat(responseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(CONNECTION_GO_AWAY);
    }

    @Test
    void shouldCompleteWithStopMessageWhenStoppingDuringResponse() {
        responseHeaders = HttpHeaders.create();
        when(response.headers()).thenReturn(responseHeaders);

        final TestScheduler testScheduler = new TestScheduler();
        when(response.messages()).thenReturn(Flowable.<Message>empty().delay(1000, TimeUnit.MILLISECONDS, testScheduler));
        when(ctx.interruptMessagesWith(any(ExecutionFailure.class))).thenReturn(Flowable.just(new DefaultMessage(STOP_MESSAGE_CONTENT)));

        cut.handleResponse(ctx).test().assertComplete();

        final ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunkObs = chunksCaptor.getValue().test();
        chunkObs.assertNotComplete();

        testScheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();
        chunkObs.assertNotComplete();

        cut.doStop();
        chunkObs.assertComplete();
        chunkObs.assertValue(buffer -> buffer.toString().equals(STOP_MESSAGE_CONTENT));
        verify(response).status(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
        verify(response).reason(HttpResponseStatus.SERVICE_UNAVAILABLE.reasonPhrase());

        // Should inform that the connection has been closed.
        assertThat(responseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(CONNECTION_CLOSE);
    }
}
