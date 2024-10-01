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
package io.gravitee.gateway.reactive.handlers.api.processor.error;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import appender.MemoryAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.handlers.api.processor.AbstractProcessorTest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class SimpleFailureProcessorTest extends AbstractProcessorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private final MemoryAppender memoryAppender = new MemoryAppender();

    @Captor
    ArgumentCaptor<Flowable<Buffer>> bufferCaptor;

    private SimpleFailureProcessor simpleFailureProcessor;

    @BeforeEach
    public void beforeEach() {
        simpleFailureProcessor = SimpleFailureProcessor.instance();
        memoryAppender.reset();
    }

    @Test
    void should_set_default_application_when_no_application_associated_to_the_request() {
        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyCtx.metrics().getApplicationId()).isEqualTo("1");
    }

    @Test
    void should_not_set_default_application_when_an_application_is_associated_to_the_request() {
        spyCtx.metrics().setApplicationId("my-application");

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyCtx.metrics().getApplicationId()).isEqualTo("my-application");
    }

    @Test
    void should_build_500_response_when_no_execution_failure() {
        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
    }

    @Test
    void should_build_response_based_on_the_execution_failure_without_message() {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.SERVICE_UNAVAILABLE.code());
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        verify(mockResponse).status(eq(HttpResponseStatus.SERVICE_UNAVAILABLE.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.SERVICE_UNAVAILABLE.reasonPhrase()));
    }

    @Test
    void should_build_a_json_response_when_json_accept_header() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("application/json"));

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        String expectedJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(expectedJson);
    }

    @Test
    void should_build_a_json_response_when_json_accept_header_with_comma() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("application/json, application/text"));

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        String expectedJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(expectedJson);
    }

    @Test
    void should_build_a_json_response_when_wildcard_accept_header() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of(MediaType.WILDCARD));

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(contentAsJson);
    }

    @Test
    void should_build_a_json_response_when_wildcard_accept_header_with_comma() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("*/*, application/text"));

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(contentAsJson);
    }

    @Test
    public void should_build_a_json_response_when_json_accept_header_and_json_content_type() {
        String contentAsJson = "{\"text\": \"error\"}";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .message(contentAsJson)
            .contentType("application/json");
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("application/json"));

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");

        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(contentAsJson);
    }

    @Test
    void should_build_a_text_response_when_no_accept_header() {
        String contentAsText = "error";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message(contentAsText);
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);

        verify(mockResponse).chunks(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().blockingFirst()).hasToString(contentAsText);
    }

    @ParameterizedTest(name = "response status ''{0}''")
    @ValueSource(
        ints = {
            0, HttpStatusCode.CONTINUE_100, HttpStatusCode.NO_CONTENT_204, HttpStatusCode.NOT_MODIFIED_304, HttpStatusCode.BAD_GATEWAY_502,
        }
    )
    void should_add_connection_close_header_when_error_status_is_not_a_client_error_family(Integer responseStatus) {
        ExecutionFailure executionFailure = new ExecutionFailure(responseStatus);
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        Assertions
            .assertThat(mockResponse.headers().getAll(HttpHeaderNames.CONNECTION))
            .hasSize(1)
            .containsExactly(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @ParameterizedTest(name = "response status ''{0}''")
    @ValueSource(ints = { 400, 404, 499 })
    void should_not_add_connection_close_header_when_error_status_is_a_client_error_family(Integer responseStatus) {
        ExecutionFailure executionFailure = new ExecutionFailure(responseStatus);
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        Assertions.assertThat(mockResponse.headers().contains(HttpHeaderNames.CONNECTION)).isFalse();
    }

    @Test
    void should_log_message_when_failure_has_exception_parameter() {
        configureMemoryAppender();

        when(mockRequest.id()).thenReturn("request-id");
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .message("message error")
            .parameters(Map.of("exception", new RuntimeException("root exception")));
        spyCtx.setInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);

        simpleFailureProcessor.execute(spyCtx).test().assertResult();

        assertThat(memoryAppender.getLoggedEvents()).hasSize(1);
        SoftAssertions.assertSoftly(soft -> {
            var event = memoryAppender.getLoggedEvents().get(0);
            soft.assertThat(event.getMessage()).contains("An error occurred while executing request");
            soft.assertThat(event.getArgumentArray()).contains("request-id", "message error");
            soft.assertThat(event.getThrowableProxy().getMessage()).isEqualTo("root exception");
        });
    }

    private void configureMemoryAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(AbstractFailureProcessor.class);
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(Level.DEBUG);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }
}
