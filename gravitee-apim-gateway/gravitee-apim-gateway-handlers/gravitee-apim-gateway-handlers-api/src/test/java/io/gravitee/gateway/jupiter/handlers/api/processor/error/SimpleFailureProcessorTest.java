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
package io.gravitee.gateway.jupiter.handlers.api.processor.error;

import static io.gravitee.gateway.api.http.HttpHeaderNames.ACCEPT;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.handlers.api.processor.AbstractProcessorTest;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class SimpleFailureProcessorTest extends AbstractProcessorTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Captor
    ArgumentCaptor<Buffer> bufferCaptor;

    private SimpleFailureProcessor simpleFailureProcessor;

    @BeforeEach
    public void beforeEach() {
        simpleFailureProcessor = SimpleFailureProcessor.instance();
    }

    @Test
    public void shouldCompleteWith500StatusWithoutExecutionFailure() {
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @Test
    public void shouldCompleteWithChangingResponseBodyWithoutProcessorFailureMessage() {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
        ctx.setInternalAttribute(ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @Test
    public void shouldCompleteWithJsonErrorAndAcceptHeaderJson() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        ctx.setInternalAttribute(ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("application/json"));
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
        verify(mockResponse).body(bufferCaptor.capture());

        assertThat(bufferCaptor.getValue().toString()).isEqualTo(contentAsJson);
    }

    @Test
    public void shouldCompleteWithJsonErrorAndAcceptHeaderWildCard() throws JsonProcessingException {
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message("error");
        String contentAsJson = mapper.writeValueAsString(new ExecutionFailureAsJson(executionFailure));
        ctx.setInternalAttribute(ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of(MediaType.WILDCARD));
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
        verify(mockResponse).body(bufferCaptor.capture());
        assertThat(bufferCaptor.getValue().toString()).isEqualTo(contentAsJson);
    }

    @Test
    public void shouldCompleteWithJsonErrorAndAcceptHeaderJsonAndContentTypeJson() {
        String contentAsJson = "{\"text\": \"error\"}";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
            .message(contentAsJson)
            .contentType("application/json");
        ctx.setInternalAttribute(ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        spyRequestHeaders.add(ACCEPT, List.of("application/json"));
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("application/json");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
        verify(mockResponse).body(bufferCaptor.capture());

        assertThat(bufferCaptor.getValue().toString()).isEqualTo(contentAsJson);
    }

    @Test
    public void shouldCompleteWithTxtErrorAndNoAcceptHeader() {
        String contentAsTxt = "error";
        ExecutionFailure executionFailure = new ExecutionFailure(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()).message(contentAsTxt);
        ctx.setInternalAttribute(ExecutionContext.ATTR_INTERNAL_EXECUTION_FAILURE, executionFailure);
        simpleFailureProcessor.execute(ctx).test().assertResult();
        verify(mockMetrics).setApplication(eq("1"));
        verify(mockResponse).status(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()));
        verify(mockResponse).reason(eq(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
        assertThat(spyResponseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)).isTrue();
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONTENT_TYPE)).isEqualTo("text/plain");
        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
        verify(mockResponse).body(bufferCaptor.capture());

        assertThat(bufferCaptor.getValue().toString()).isEqualTo(contentAsTxt);
    }
}
