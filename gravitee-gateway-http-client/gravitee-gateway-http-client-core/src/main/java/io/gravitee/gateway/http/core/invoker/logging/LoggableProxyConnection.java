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
package io.gravitee.gateway.http.core.invoker.logging;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableProxyConnection implements ProxyConnection {

    private final ProxyConnection proxyConnection;
    private final Request request;
    private Buffer buffer;

    public LoggableProxyConnection(final ProxyConnection proxyConnection, final ProxyRequest proxyRequest) {
        this.proxyConnection = proxyConnection;
        this.request = proxyRequest.request();

        this.request.metrics().setProxyRequest(new io.gravitee.reporter.api.http.Request());
        this.request.metrics().getProxyRequest().setUri(proxyRequest.uri().toString());
        this.request.metrics().getProxyRequest().setMethod(proxyRequest.method());
        this.request.metrics().getProxyRequest().setHeaders(proxyRequest.headers());
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
        return proxyConnection.responseHandler(new LoggableProxyConnection.LoggableProxyResponseHandler(responseHandler));
    }

    @Override
    public void end() {
        if (buffer != null) {
            request.metrics().getProxyRequest().setBody(buffer.toString());
        }

        proxyConnection.end();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        if (buffer == null) {
            buffer = Buffer.buffer();
        }
        buffer.appendBuffer(chunk);

        return proxyConnection.write(chunk);
    }

    class LoggableProxyResponseHandler implements Handler<ProxyResponse> {
        private final Handler<ProxyResponse> responseHandler;

        LoggableProxyResponseHandler(final Handler<ProxyResponse> responseHandler) {
            this.responseHandler = responseHandler;
        }

        @Override
        public void handle(ProxyResponse proxyResponse) {
            responseHandler.handle(new LoggableProxyConnection.LoggableProxyResponse(request, proxyResponse));
        }
    }

    class LoggableProxyResponse implements ProxyResponse {

        private final ProxyResponse proxyResponse;
        private final Request request;
        private Buffer buffer;

        LoggableProxyResponse(final Request request, final ProxyResponse proxyResponse) {
            this.request = request;
            this.proxyResponse = proxyResponse;

            this.request.metrics().setProxyResponse(new io.gravitee.reporter.api.http.Response(proxyResponse.status()));
            this.request.metrics().getProxyResponse().setHeaders(proxyResponse.headers());
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            return proxyResponse.bodyHandler(chunk -> {
                if (buffer == null) {
                    buffer = Buffer.buffer();
                }

                buffer.appendBuffer(chunk);
                bodyHandler.handle(chunk);
            });
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            return proxyResponse.endHandler(result -> {
                if (buffer != null) {
                    request.metrics().getProxyResponse().setBody(buffer.toString());
                }

                endHandler.handle(result);
            });
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
    }
}
