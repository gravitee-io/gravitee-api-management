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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.response;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LogHeadersCaptor;

/**
 * Allows to log the response status, headers and body returned by the backend endpoint depending on what is configured on the {@link LoggingContext}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogEndpointResponse extends LogResponse {

    public LogEndpointResponse(LoggingContext loggingContext, HttpResponseInternal response) {
        super(loggingContext, response);
    }

    public void setupCapture(HttpExecutionContextInternal ctx) {
        if (isLogHeaders()) {
            // Set a headers captor to capture the response headers coming from the endpoint, excluding any already set response headers (ste by a policy, processor, ...).
            response.setHeaders(new LogHeadersCaptor(response.headers()));
        }

        response.registerBuffersInterceptor(chunks -> {
            // Nothing to prepare for the endpoint response.
            if (isLogPayload() && loggingContext.isContentTypeLoggable(response.headers().get(HttpHeaderNames.CONTENT_TYPE), ctx)) {
                final Buffer buffer = Buffer.buffer();
                if (loggingContext.isBodyLoggable()) {
                    chunks = chunks
                        .doOnNext(chunk -> BufferUtils.appendBuffer(buffer, chunk, loggingContext.getMaxSizeLogMessage()))
                        .doFinally(() -> this.setBody(buffer.toString()));
                } else {
                    this.setBody("BODY NOT CAPTURED");
                }
            }

            // For the endpoint response logging, we can capture the response headers and status when the body receiving starts.
            return chunks.doOnSubscribe(subscription -> captureStatusAndHeaders());
        });
    }

    public void finalizeCapture(HttpExecutionContextInternal ctx) {
        if (isLogHeaders()) {
            response.setHeaders(((LogHeadersCaptor) response.headers()).getDelegate());
        }
    }

    private void captureStatusAndHeaders() {
        if (isLogHeaders()) {
            this.setHeaders(response.headers());
        }
        this.setStatus(response.status());
    }

    protected boolean isLogPayload() {
        return loggingContext.endpointResponsePayload();
    }

    protected boolean isLogHeaders() {
        return loggingContext.endpointResponseHeaders();
    }
}
