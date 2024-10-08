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
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class LogRequest extends io.gravitee.reporter.api.common.Request {

    protected final LoggingContext loggingContext;
    protected final HttpPlainRequest request;

    protected LogRequest(LoggingContext loggingContext, HttpPlainRequest request) {
        this.loggingContext = loggingContext;
        this.request = request;

        this.setMethod(request.method());
    }

    public void capture() {
        if (isLogPayload() && loggingContext.isContentTypeLoggable(request.headers().get(HttpHeaderNames.CONTENT_TYPE))) {
            final Buffer buffer = Buffer.buffer();

            request.chunks(
                request
                    .chunks()
                    .doOnNext(chunk -> BufferUtils.appendBuffer(buffer, chunk, loggingContext.getMaxSizeLogMessage()))
                    .doOnComplete(() -> this.setBody(buffer.toString()))
            );
        }

        if (isLogHeaders()) {
            this.setHeaders(HttpHeaders.create(request.headers()));
        }
    }

    protected abstract boolean isLogPayload();

    protected abstract boolean isLogHeaders();
}
