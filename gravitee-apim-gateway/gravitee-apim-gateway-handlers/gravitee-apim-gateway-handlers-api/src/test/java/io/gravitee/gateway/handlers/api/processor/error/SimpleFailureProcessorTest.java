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
package io.gravitee.gateway.handlers.api.processor.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.AbstractProcessor;
import io.gravitee.reporter.api.http.Metrics;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class SimpleFailureProcessorTest {

    public static final String FAILURE_KEY = "failureKey";
    public static final int FAILURE_CODE = 400;
    SimpleFailureProcessor cut;

    @Mock
    private AbstractProcessor<ExecutionContext> processorNext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private ProcessorFailure processorFailure;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private HttpHeaders requestHeaders;

    @Mock
    private HttpHeaders responseHeaders;

    private Metrics metrics;

    @BeforeEach
    void setUp() {
        cut = new SimpleFailureProcessor();
        cut.handler(processorNext);
        metrics = Metrics.on(System.currentTimeMillis()).build();
        when(request.metrics()).thenReturn(metrics);
        //        when(request.headers()).thenReturn(requestHeaders);
        lenient().when(request.headers()).thenReturn(requestHeaders);
        when(response.headers()).thenReturn(responseHeaders);
        lenient().when(response.status()).thenReturn(FAILURE_CODE);
        when(executionContext.request()).thenReturn(request);
        when(executionContext.response()).thenReturn(response);
        when(processorFailure.key()).thenReturn(FAILURE_KEY);
        when(processorFailure.statusCode()).thenReturn(FAILURE_CODE);
    }

    @Test
    @DisplayName("Should handle failure without message")
    void shouldHandleFailureWithoutFailureMessage() {
        when(executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(processorFailure);

        cut.handle(executionContext);

        assertThat(metrics.getErrorKey()).isEqualTo(FAILURE_KEY);
        verify(response).status(FAILURE_CODE);
        verify(response).reason("Bad Request");
        verify(responseHeaders).set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        verify(processorNext).handle(executionContext);
    }

    @Test
    @DisplayName("Should handle failure with message and without ACCEPT header")
    void shouldHandleFailureWithFailureMessageWithoutAcceptAcceptHeader() {
        when(executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(processorFailure);
        final String failureMessage = "Message";
        when(processorFailure.message()).thenReturn(failureMessage);

        cut.handle(executionContext);

        assertThat(metrics.getErrorKey()).isEqualTo(FAILURE_KEY);
        verify(response).status(FAILURE_CODE);
        verify(response).reason("Bad Request");
        verify(responseHeaders).set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        verify(processorNext).handle(executionContext);
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(failureMessage.length()));
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_TYPE, MediaType.TEXT_PLAIN);
        verify(response).write(any());
    }

    @ParameterizedTest
    @DisplayName("Should handle failure with message and ACCEPT header, failure content type json")
    @CsvSource(value = { "application/json, application/json", "*/*, application/json" })
    void shouldHandleFailureWithAcceptHeader(String acceptHeader, String expectedContentType) {
        when(executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(processorFailure);
        final String failureMessage = "Message";
        when(processorFailure.message()).thenReturn(failureMessage);
        when(processorFailure.contentType()).thenReturn(MediaType.APPLICATION_JSON);
        when(requestHeaders.getAll(HttpHeaderNames.ACCEPT)).thenReturn(List.of(acceptHeader));

        cut.handle(executionContext);

        assertThat(metrics.getErrorKey()).isEqualTo(FAILURE_KEY);
        verify(response).status(FAILURE_CODE);
        verify(response).reason("Bad Request");
        verify(responseHeaders).set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        verify(processorNext).handle(executionContext);
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(failureMessage.length()));
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_TYPE, expectedContentType);
        verify(response).write(any());
    }

    @ParameterizedTest
    @DisplayName("Should handle failure with message and ACCEPT header, failure content type json")
    @CsvSource(
        value = { "application/json|application/json", "*/*|application/json", "text/html,application/json;q=0.9|application/json" },
        delimiter = '|'
    )
    void shouldHandleFailureWithAcceptHeaderFailureNoContentType(String acceptHeader, String expectedContentType) {
        when(executionContext.getAttribute(ExecutionContext.ATTR_PREFIX + "failure")).thenReturn(processorFailure);
        final String failureMessage = "Message";
        when(processorFailure.message()).thenReturn(failureMessage);
        when(requestHeaders.getAll(HttpHeaderNames.ACCEPT)).thenReturn(List.of(acceptHeader));

        cut.handle(executionContext);

        assertThat(metrics.getErrorKey()).isEqualTo(FAILURE_KEY);
        verify(response).status(FAILURE_CODE);
        verify(response).reason("Bad Request");
        verify(responseHeaders).set(HttpHeaderNames.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        verify(processorNext).handle(executionContext);
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_LENGTH, "44");
        verify(responseHeaders).set(HttpHeaderNames.CONTENT_TYPE, expectedContentType);
        verify(response).write(any());
    }
}
