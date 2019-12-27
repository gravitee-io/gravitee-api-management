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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.MediaType;
import io.gravitee.common.ssl.SSLInfo;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientResponse implements Response {

    private final Response response;
    private final Request request;
    private final LogConfiguration logConfiguration;
    private final Log log;
    private Buffer buffer;
    private boolean isEventStream;

    public LoggableClientResponse(final Request request, final Response response, LogConfiguration logConfiguration) {
        this.request = request;
        this.response = response;
        this.logConfiguration = logConfiguration;
        this.log = this.request.metrics().getLog();
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        if (buffer == null) {
            buffer = Buffer.buffer();
            isEventStream = MediaType.TEXT_EVENT_STREAM.equalsIgnoreCase(response.headers().contentType());
        }

        if (!isEventStream) {
            appendLog(buffer, content);
        }

        response.write(content);
        return response;
    }

    @Override
    public Response status(int statusCode) {
        log.setClientResponse(new io.gravitee.reporter.api.common.Response(statusCode));
        return response.status(statusCode);
    }

    @Override
    public void end() {
        calculate(buffer);
        response.end();
    }

    @Override
    public void end(Buffer buffer) {
        calculate(buffer);
        response.end(buffer);
    }

    private void calculate(Buffer buffer) {
        // Here we are sure that headers has been full processed by policies
        log.getClientResponse().setHeaders(headers());
        if (logConfiguration.isLogSSLInformation() && request.sslSession() != null) {
            log.getClientResponse().setSslInfo(new SSLInfo(request.sslSession(), logConfiguration.isLogCertificateChains()));
        }

        if (buffer != null) {
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

    public LogConfiguration getLogConfiguration() {
        return logConfiguration;
    }
}
