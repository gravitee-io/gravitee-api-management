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
package io.gravitee.gateway.core.logging;

import static io.gravitee.gateway.core.logging.utils.LoggingUtils.isContentTypeLoggable;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.RequestWrapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientRequest extends RequestWrapper {

    private final Log log;
    private Buffer buffer;
    private final ExecutionContext context;
    private boolean isContentTypeLoggable;

    public LoggableClientRequest(final Request request, final ExecutionContext context) {
        super(request);
        this.context = context;
        this.log = new Log(request.metrics().timestamp().toEpochMilli());
        this.log.setRequestId(request.id());

        // Associate log
        this.request.metrics().setLog(log);

        // Create a copy of HTTP request headers
        log.setClientRequest(new io.gravitee.reporter.api.common.Request());
        log.getClientRequest().setMethod(this.method());
        log.getClientRequest().setUri(this.uri());

        if (LoggingUtils.isRequestHeadersLoggable(context)) {
            log.getClientRequest().setHeaders(this.headers());
        }
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        request.bodyHandler(
            chunk -> {
                if (buffer == null) {
                    buffer = Buffer.buffer();
                    isContentTypeLoggable = isContentTypeLoggable(request.headers().get(HttpHeaderNames.CONTENT_TYPE), context);
                }
                bodyHandler.handle(chunk);
                if (isContentTypeLoggable && LoggingUtils.isRequestPayloadsLoggable(context)) {
                    appendLog(buffer, chunk);
                }
            }
        );
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        request.endHandler(
            result -> {
                if (buffer != null) {
                    log.getClientRequest().setBody(buffer.toString());
                }

                endHandler.handle(result);
            }
        );

        return this;
    }

    protected void appendLog(Buffer buffer, Buffer chunk) {
        buffer.appendBuffer(chunk);
    }
}
