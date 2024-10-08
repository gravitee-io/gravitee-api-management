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
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.HttpResponse;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainResponse;
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;
import io.gravitee.gateway.reactive.handlers.api.v4.analytics.logging.LogHeadersCaptor;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class LogResponse extends io.gravitee.reporter.api.common.Response {

    protected final LoggingContext loggingContext;
    protected final HttpPlainResponse response;

    protected LogResponse(LoggingContext loggingContext, HttpPlainResponse response) {
        this.loggingContext = loggingContext;
        this.response = response;
    }

    public void capture() {
        if (isLogPayload() && loggingContext.isContentTypeLoggable(response.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
            final Buffer buffer = Buffer.buffer();
            response.chunks(
                response
                    .chunks()
                    .doOnNext(chunk -> BufferUtils.appendBuffer(buffer, chunk, loggingContext.getMaxSizeLogMessage()))
                    .doOnComplete(() -> this.setBody(buffer.toString()))
            );
        }

        if (isLogHeaders()) {
            this.setHeaders(response.headers());
        }

        this.setStatus(response.status());
    }

    @Override
    public void setHeaders(HttpHeaders headers) {
        if (headers instanceof LogHeadersCaptor) {
            super.setHeaders(((LogHeadersCaptor) headers).getCaptured());
        } else {
            super.setHeaders(HttpHeaders.create(headers));
        }
    }

    protected abstract boolean isLogPayload();

    protected abstract boolean isLogHeaders();
}
