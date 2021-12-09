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

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientResponse implements Response {

    private final Response response;
    private final Request request;
    private final Log log;
    private Buffer buffer;
    private final ExecutionContext context;
    private boolean isContentTypeLoggable;

    public LoggableClientResponse(final Request request, final Response response, final ExecutionContext context) {
        this.request = request;
        this.response = response;
        this.context = context;
        this.log = this.request.metrics().getLog();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        if (buffer == null) {
            buffer = Buffer.buffer();
            isContentTypeLoggable = isContentTypeLoggable(response.headers().get(HttpHeaderNames.CONTENT_TYPE), context);
        }

        if (isContentTypeLoggable && LoggingUtils.isResponsePayloadsLoggable(context)) {
            appendLog(buffer, chunk);
        }

        response.write(chunk);
        return response;
    }

    @Override
    public Response status(int statusCode) {
        log.setClientResponse(new io.gravitee.reporter.api.common.Response(statusCode));
        return response.status(statusCode);
    }

    @Override
    public Response endHandler(Handler<Void> endHandler) {
        writeClientResponseLog(buffer);
        return response.endHandler(endHandler);
    }

    private void writeClientResponseLog(Buffer buffer) {
        // Check if log is not already write by GDPR policy
        if (LoggingUtils.isResponseHeadersLoggable(context) && log.getClientResponse().getHeaders() == null) {
            // Here we are sure that headers has been full processed by policies
            log.getClientResponse().setHeaders(headers());
        }

        // Check if log is not already write by GDPR policy
        if (buffer != null && log.getClientResponse().getBody() == null) {
            log.getClientResponse().setBody(buffer.toString());
        }
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public String reason() {
        return response.reason();
    }

    @Override
    public Response reason(String reason) {
        return response.reason(reason);
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }

    @Override
    public boolean ended() {
        return response.ended();
    }

    @Override
    public HttpHeaders trailers() {
        return response.trailers();
    }

    @Override
    public Response writeCustomFrame(HttpFrame frame) {
        return response.writeCustomFrame(frame);
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        response.drainHandler(drainHandler);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return response.writeQueueFull();
    }

    protected void appendLog(Buffer buffer, Buffer chunk) {
        buffer.appendBuffer(chunk);
    }
}
