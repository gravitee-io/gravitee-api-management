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
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
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
    private ExecutionContext mockExecutionContext;

    @Mock
    private Request mockRequest;

    @Mock
    private Response mockResponse;

    private SseEntrypointConnector sseEntrypointConnector;

    @BeforeEach
    void beforeEach() {
        lenient().when(mockExecutionContext.request()).thenReturn(mockRequest);
        lenient().when(mockExecutionContext.response()).thenReturn(mockResponse);
        lenient().when(mockResponse.writeHeaders()).thenReturn(Completable.complete());
        lenient().when(mockResponse.write(any())).thenReturn(Completable.complete());
        lenient().when(mockResponse.end()).thenReturn(Completable.complete());
        sseEntrypointConnector = new SseEntrypointConnector(null);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(sseEntrypointConnector.matchCriteriaCount()).isEqualTo(2);
    }

    @Test
    void shouldMatchesWithValidContext() {
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.ACCEPT, "text/event-stream");
        when(mockRequest.headers()).thenReturn(httpHeaders);
        when(mockRequest.method()).thenReturn(HttpMethod.GET);

        boolean matches = sseEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithBadContentType() {
        when(mockExecutionContext.request()).thenReturn(mockRequest);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        when(mockRequest.headers()).thenReturn(httpHeaders);
        when(mockRequest.method()).thenReturn(HttpMethod.GET);

        boolean matches = sseEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldNotMatchesWithBadMethod() {
        when(mockExecutionContext.request()).thenReturn(mockRequest);
        HttpHeaders httpHeaders = HttpHeaders.create();
        httpHeaders.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        when(mockRequest.headers()).thenReturn(httpHeaders);
        when(mockRequest.method()).thenReturn(HttpMethod.POST);

        boolean matches = sseEntrypointConnector.matches(mockExecutionContext);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldCompleteWithoutDoingAnything() {
        sseEntrypointConnector.handleRequest(mockExecutionContext).test().assertComplete();
        verifyNoInteractions(mockExecutionContext);
    }

    @Test
    void shouldCompleteAndEndWhenResponseMessagesComplete() {
        Flowable<Message> empty = Flowable.empty();
        when(mockResponse.messages()).thenReturn(empty);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockResponse.headers()).thenReturn(httpHeaders);
        when(mockResponse.end()).thenReturn(Completable.complete());
        when(mockExecutionContext.response()).thenReturn(mockResponse);
        sseEntrypointConnector.handleResponse(mockExecutionContext).test().assertComplete();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
    }

    @Test
    void shouldWriteSseEventAndCompleteAndEndWhenResponseMessagesComplete() {
        Flowable<Message> messages = Flowable.just(new DefaultMessage("1"), new DefaultMessage("2"), new DefaultMessage("3"));
        when(mockResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockResponse.headers()).thenReturn(httpHeaders);
        when(mockResponse.write(any())).thenReturn(Completable.complete());
        when(mockResponse.end()).thenReturn(Completable.complete());
        when(mockExecutionContext.response()).thenReturn(mockResponse);
        boolean b = sseEntrypointConnector.handleResponse(mockExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockResponse, times(8)).write(any());
    }

    @Test
    void shouldWriteErrorSseEventAndCompleteAndEndWhenResponseMessagesFail() {
        Flowable<Message> messages = Flowable.error(new RuntimeException("error"));
        when(mockResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockResponse.headers()).thenReturn(httpHeaders);
        when(mockResponse.end()).thenReturn(Completable.complete());
        when(mockExecutionContext.response()).thenReturn(mockResponse);
        boolean b = sseEntrypointConnector.handleResponse(mockExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockResponse, times(1)).end(any());
    }
}
