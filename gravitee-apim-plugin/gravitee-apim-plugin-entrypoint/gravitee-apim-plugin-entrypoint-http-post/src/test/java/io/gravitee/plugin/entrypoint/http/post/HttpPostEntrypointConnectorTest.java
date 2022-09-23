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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import java.util.Map;
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
    private ExecutionContext mockExecutionContext;

    private HttpPostEntrypointConnector httpPostEntrypointConnector;
    private DummyMessageRequest dummyMessageRequest;

    @Mock
    private Response mockMessageResponse;

    private HttpHeaders responseHeaders;

    @BeforeEach
    void beforeEach() {
        dummyMessageRequest = new DummyMessageRequest();
        dummyMessageRequest.messages(Flowable.empty());
        lenient().when(mockExecutionContext.request()).thenReturn(dummyMessageRequest);
        lenient().when(mockExecutionContext.response()).thenReturn(mockMessageResponse);
        lenient().when(mockMessageResponse.headers()).thenReturn(HttpHeaders.create());
        lenient().when(mockMessageResponse.messages()).thenReturn(Flowable.empty());
        httpPostEntrypointConnector = new HttpPostEntrypointConnector(null);
    }

    @Test
    void shouldIdReturnHttpPost() {
        assertThat(httpPostEntrypointConnector.id()).isEqualTo("http-post");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(httpPostEntrypointConnector.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(httpPostEntrypointConnector.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportPublishModeOnly() {
        assertThat(httpPostEntrypointConnector.supportedModes()).containsOnly(ConnectorMode.PUBLISH);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(httpPostEntrypointConnector.matchCriteriaCount()).isEqualTo(1);
    }

    @Test
    void shouldMatchesWithValidContext() {
        dummyMessageRequest.method(HttpMethod.POST);

        boolean matches = httpPostEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        dummyMessageRequest.method(HttpMethod.GET);

        boolean matches = httpPostEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteAndSetRequestBuffersInRequestMessages() {
        dummyMessageRequest.headers(HttpHeaders.create());
        dummyMessageRequest.body(Maybe.just(Buffer.buffer("bodyContent")));
        httpPostEntrypointConnector.handleRequest(mockExecutionContext).test().assertComplete();

        dummyMessageRequest
            .messages()
            .test()
            .assertValue(message -> "bodyContent".equals(message.content().toString()) && message.id() != null);
    }

    @Test
    void shouldCompleteWhenMessagesComplete() {
        httpPostEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(mockMessageResponse).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertComplete();
        verify(mockMessageResponse).status(HttpResponseStatus.ACCEPTED.code());
        verify(mockMessageResponse).reason(HttpResponseStatus.ACCEPTED.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsFullError() {
        responseHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(responseHeaders);
        when(mockMessageResponse.messages())
            .thenReturn(
                Flowable.just(
                    DefaultMessage
                        .builder()
                        .error(true)
                        .metadata(Map.of("statusCode", HttpResponseStatus.NOT_FOUND.code()))
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
        httpPostEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(mockMessageResponse).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals("error"));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        verify(mockMessageResponse).status(HttpResponseStatus.NOT_FOUND.code());
        verify(mockMessageResponse).reason(HttpResponseStatus.NOT_FOUND.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsError() {
        responseHeaders = HttpHeaders.create();
        when(mockMessageResponse.messages())
            .thenReturn(Flowable.just(DefaultMessage.builder().error(true).content(Buffer.buffer("error")).build()));
        httpPostEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(mockMessageResponse).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals("error"));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
        verify(mockMessageResponse).status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(mockMessageResponse).reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
    }

    @Test
    void shouldCompleteWhenResponseMessagesContainsErrorWithBody() {
        responseHeaders = HttpHeaders.create();
        when(mockMessageResponse.messages()).thenReturn(Flowable.just(DefaultMessage.builder().error(true).build()));
        httpPostEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();

        ArgumentCaptor<Flowable<Buffer>> chunksCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(mockMessageResponse).chunks(chunksCaptor.capture());

        chunksCaptor.getValue().test().assertValue(buffer -> buffer.toString().equals(""));

        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isFalse();
        assertThat(responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isFalse();
        verify(mockMessageResponse).status(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        verify(mockMessageResponse).reason(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
    }
}
