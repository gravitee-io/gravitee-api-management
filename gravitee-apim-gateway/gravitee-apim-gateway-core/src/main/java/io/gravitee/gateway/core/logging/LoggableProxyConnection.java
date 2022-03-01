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

import static io.gravitee.gateway.core.logging.utils.LoggingUtils.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableProxyConnection implements ProxyConnection {

    private final ProxyConnection proxyConnection;
    private final ProxyRequest proxyRequest;
    private final ExecutionContext context;
    private final Log log;
    private Buffer buffer;
    private boolean isContentTypeLoggable;

    public LoggableProxyConnection(final ProxyConnection proxyConnection, final ProxyRequest proxyRequest, final ExecutionContext context) {
        this.proxyConnection = proxyConnection;
        this.proxyRequest = proxyRequest;
        this.context = context;
        Log log = context.request().metrics().getLog();

        // If log is enable only for 'Proxy only' mode, the log structure is not yet created
        if (log == null) {
            log = new Log(context.request().metrics().timestamp().toEpochMilli());
            log.setRequestId(context.request().metrics().getRequestId());

            // Associate log
            context.request().metrics().setLog(log);
        }

        this.log = log;
        this.log.setProxyRequest(new Request());
        this.log.getProxyRequest().setUri(context.request().metrics().getEndpoint());
        this.log.getProxyRequest().setMethod(proxyRequest.method());
        if (LoggingUtils.isProxyRequestHeadersLoggable(context)) {
            this.log.getProxyRequest().setHeaders(proxyRequest.headers());
        }
    }

    @Override
    public ProxyConnection cancel() {
        return proxyConnection.cancel();
    }

    @Override
    public ProxyConnection exceptionHandler(Handler<Throwable> timeoutHandler) {
        return proxyConnection.exceptionHandler(timeoutHandler);
    }

    @Override
    public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        return responseHandler(proxyConnection, responseHandler, context);
    }

    @Override
    public void end() {
        // Check if log is not already write by GDPR policy
        if (buffer != null && this.log.getProxyRequest().getBody() == null) {
            this.log.getProxyRequest().setBody(buffer.toString());
        }

        proxyConnection.end();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        if (buffer == null) {
            buffer = Buffer.buffer();
            isContentTypeLoggable = isContentTypeLoggable(proxyRequest.headers().contentType(), context);
        }

        proxyConnection.write(chunk);

        if (isContentTypeLoggable && LoggingUtils.isProxyRequestPayloadsLoggable(context)) {
            appendLog(buffer, chunk);
        }

        return this;
    }

    protected void appendLog(Buffer buffer, Buffer chunk) {
        buffer.appendBuffer(chunk);
    }

    protected ProxyConnection responseHandler(
        ProxyConnection proxyConnection,
        Handler<ProxyResponse> responseHandler,
        final ExecutionContext context
    ) {
        return proxyConnection.responseHandler(new LoggableProxyConnection.LoggableProxyResponseHandler(responseHandler, context));
    }

    class LoggableProxyResponseHandler implements Handler<ProxyResponse> {

        private final Handler<ProxyResponse> responseHandler;
        protected final ExecutionContext context;

        LoggableProxyResponseHandler(final Handler<ProxyResponse> responseHandler, final ExecutionContext context) {
            this.responseHandler = responseHandler;
            this.context = context;
        }

        @Override
        public void handle(ProxyResponse proxyResponse) {
            handle(responseHandler, proxyResponse);
        }

        protected void handle(Handler<ProxyResponse> responseHandler, ProxyResponse proxyResponse) {
            responseHandler.handle(new LoggableProxyConnection.LoggableProxyResponse(proxyResponse, context));
        }
    }

    class LoggableProxyResponse implements ProxyResponse {

        private final ProxyResponse proxyResponse;
        private final ExecutionContext context;
        private Buffer buffer;
        private boolean isContentTypeLoggable;

        LoggableProxyResponse(final ProxyResponse proxyResponse, final ExecutionContext context) {
            this.proxyResponse = proxyResponse;
            this.context = context;

            log.setProxyResponse(new Response(proxyResponse.status()));
            if (LoggingUtils.isProxyResponseHeadersLoggable(context)) {
                log.getProxyResponse().setHeaders(proxyResponse.headers());
            }
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            proxyResponse.bodyHandler(
                chunk -> {
                    if (buffer == null) {
                        buffer = Buffer.buffer();
                        isContentTypeLoggable = isContentTypeLoggable(proxyResponse.headers().contentType(), context);
                    }

                    if (isContentTypeLoggable && LoggingUtils.isProxyResponsePayloadsLoggable(context)) {
                        appendLog(buffer, chunk);
                    }

                    bodyHandler.handle(chunk);
                }
            );

            return this;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            proxyResponse.endHandler(
                result -> {
                    if (buffer != null) {
                        log.getProxyResponse().setBody(buffer.toString());
                    }

                    endHandler.handle(result);
                }
            );

            return this;
        }

        @Override
        public ReadStream<Buffer> pause() {
            return proxyResponse.pause();
        }

        @Override
        public ReadStream<Buffer> resume() {
            return proxyResponse.resume();
        }

        @Override
        public HttpHeaders headers() {
            return proxyResponse.headers();
        }

        @Override
        public int status() {
            return proxyResponse.status();
        }

        @Override
        public String reason() {
            return proxyResponse.reason();
        }

        @Override
        public boolean connected() {
            return proxyResponse.connected();
        }

        protected void appendLog(Buffer buffer, Buffer chunk) {
            buffer.appendBuffer(chunk);
        }
    }
}
