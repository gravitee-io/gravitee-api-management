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
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.context.HttpResponseInternal;
import io.gravitee.gateway.reactive.core.v4.analytics.BufferUtils;
import io.gravitee.gateway.reactive.core.v4.analytics.LoggingContext;

/**
 * Allows to log the response status, headers and body returned to the client depending on what is configured on the {@link LoggingContext}.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LogEntrypointResponse extends LogResponse {

    public LogEntrypointResponse(LoggingContext loggingContext, HttpResponseInternal response) {
        super(loggingContext, response);
    }

    public void capture(HttpExecutionContextInternal ctx) {
        if (isLogPayload() && loggingContext.isContentTypeLoggable(response.headers().get(HttpHeaderNames.CONTENT_TYPE), ctx)) {
            final Buffer buffer = Buffer.buffer();
            if (loggingContext.isBodyLoggable()) {
                response.chunks(
                    response
                        .chunks()
                        .doOnNext(chunk -> BufferUtils.appendBuffer(buffer, chunk, loggingContext.getMaxSizeLogMessage()))
                        .doOnComplete(() -> this.setBody(buffer.toString()))
                );
            } else {
                this.setBody("BODY NOT CAPTURED");
            }
        }

        if (isLogHeaders()) {
            this.setHeaders(response.headers());
        }

        this.setStatus(response.status());
    }

    protected boolean isLogPayload() {
        return loggingContext.entrypointResponsePayload();
    }

    protected boolean isLogHeaders() {
        return loggingContext.entrypointResponseHeaders();
    }
}
