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
package io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.request;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpRequestInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.reporter.api.v4.metric.Metrics;

/**
 * Allows to log the response status, headers and body sent to the backend endpoint depending on what is configured on the {@link LoggingContext}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogEndpointRequest extends LogRequest {

    public LogEndpointRequest(LoggingContext loggingContext, HttpRequestInternal request) {
        super(loggingContext, request);
        this.setUri("");
    }

    public void setupCapture(HttpExecutionContextInternal ctx) {
        final Buffer buffer = Buffer.buffer();

        if (isLogPayload() && loggingContext.isContentTypeLoggable(request.headers().get(HttpHeaderNames.CONTENT_TYPE), ctx)) {
            request.registerBuffersInterceptor(chunks -> {
                if (loggingContext.isBodyLoggable()) {
                    chunks = chunks
                        .doOnNext(chunk -> BufferUtils.appendBuffer(buffer, chunk, loggingContext.getMaxSizeLogMessage()))
                        .doFinally(() -> this.setBody(buffer.toString()));
                } else {
                    this.setBody("BODY NOT CAPTURED");
                }

                return chunks;
            });
        }
    }

    public void finalizeCapture(HttpExecutionContextInternal ctx) {
        if (isLogHeaders()) {
            this.setHeaders(HttpHeaders.create(request.headers()));
        }

        Metrics metrics = ctx.metrics();
        this.setUri(metrics.getEndpoint() != null ? metrics.getEndpoint() : "");
        setMethod(ctx.request().method());
    }

    @Override
    public void setHeaders(HttpHeaders headers) {
        super.setHeaders(HttpHeaders.create(headers));
    }

    protected boolean isLogPayload() {
        return loggingContext.endpointRequestPayload();
    }

    protected boolean isLogHeaders() {
        return loggingContext.endpointRequestHeaders();
    }
}
