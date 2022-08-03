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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.context.MessageRequest;
import io.gravitee.gateway.jupiter.api.context.MessageResponse;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SseEntrypointConnectorTest {

    @Mock
    private MessageExecutionContext mockMessageExecutionContext;

    @Mock
    private MessageRequest mockMessageRequest;

    @Mock
    private MessageResponse mockMessageResponse;

    private SseEntrypointConnector sseEntrypointConnector;

    @BeforeEach
    void beforeEach() {
        sseEntrypointConnector = new SseEntrypointConnector();
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(sseEntrypointConnector.matchCriteriaCount()).isEqualTo(2);
    }

    @Test
    void shouldMatchesWithValidContext() {
        when(mockMessageExecutionContext.request()).thenReturn(mockMessageRequest);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        when(mockMessageRequest.headers()).thenReturn(httpHeaders);
        when(mockMessageRequest.method()).thenReturn(HttpMethod.GET);

        boolean matches = sseEntrypointConnector.matches(mockMessageExecutionContext);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadContentType() {
        when(mockMessageExecutionContext.request()).thenReturn(mockMessageRequest);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(mockMessageRequest.headers()).thenReturn(httpHeaders);
        when(mockMessageRequest.method()).thenReturn(HttpMethod.GET);

        boolean matches = sseEntrypointConnector.matches(mockMessageExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        when(mockMessageExecutionContext.request()).thenReturn(mockMessageRequest);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        when(mockMessageRequest.headers()).thenReturn(httpHeaders);
        when(mockMessageRequest.method()).thenReturn(HttpMethod.POST);

        boolean matches = sseEntrypointConnector.matches(mockMessageExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteWithoutDoingAnything() {
        sseEntrypointConnector.handleRequest(mockMessageExecutionContext).test().assertComplete();
        verifyNoInteractions(mockMessageExecutionContext);
    }

    @Test
    void shouldCompleteAndEndWhenResponseMessagesComplete() {
        Flowable<Message> empty = Flowable.empty();
        when(mockMessageResponse.messages()).thenReturn(empty);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(httpHeaders);
        when(mockMessageResponse.end()).thenReturn(Completable.complete());
        when(mockMessageExecutionContext.response()).thenReturn(mockMessageResponse);
        sseEntrypointConnector.handleResponse(mockMessageExecutionContext).test().assertComplete();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
    }

    @Test
    void shouldWriteSseEventAndCompleteAndEndWhenResponseMessagesComplete() {
        Flowable<Message> messages = Flowable.just(new DummyMessage("1"), new DummyMessage("2"), new DummyMessage("3"));
        when(mockMessageResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(httpHeaders);
        when(mockMessageResponse.write(any())).thenReturn(Completable.complete());
        when(mockMessageResponse.end()).thenReturn(Completable.complete());
        when(mockMessageExecutionContext.response()).thenReturn(mockMessageResponse);
        boolean b = sseEntrypointConnector.handleResponse(mockMessageExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockMessageResponse, times(6)).write(any());
    }

    @Test
    void shouldWriteErrorSseEventAndCompleteAndEndWhenResponseMessagesFail() {
        Flowable<Message> messages = Flowable.error(new RuntimeException("error"));
        when(mockMessageResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(httpHeaders);
        when(mockMessageResponse.end()).thenReturn(Completable.complete());
        when(mockMessageExecutionContext.response()).thenReturn(mockMessageResponse);
        boolean b = sseEntrypointConnector.handleResponse(mockMessageExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockMessageResponse, times(1)).end(any());
    }
}
