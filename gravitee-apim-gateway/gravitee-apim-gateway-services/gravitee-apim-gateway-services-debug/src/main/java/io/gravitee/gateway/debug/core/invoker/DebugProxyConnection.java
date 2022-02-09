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
package io.gravitee.gateway.debug.core.invoker;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugProxyConnection implements ProxyConnection {

    private final ProxyConnection proxyConnection;
    private final DebugExecutionContext context;

    public DebugProxyConnection(final ProxyConnection proxyConnection, final ExecutionContext context) {
        this.proxyConnection = proxyConnection;
        this.context = (DebugExecutionContext) context;
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
        proxyConnection.end();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        proxyConnection.write(chunk);
        return this;
    }

    protected ProxyConnection responseHandler(
        ProxyConnection proxyConnection,
        Handler<ProxyResponse> responseHandler,
        final DebugExecutionContext context
    ) {
        return proxyConnection.responseHandler(new DebugProxyResponseHandler(responseHandler, context));
    }

    static class DebugProxyResponseHandler implements Handler<ProxyResponse> {

        private final Handler<ProxyResponse> responseHandler;
        protected final DebugExecutionContext context;

        DebugProxyResponseHandler(final Handler<ProxyResponse> responseHandler, final DebugExecutionContext context) {
            this.responseHandler = responseHandler;
            this.context = context;
        }

        @Override
        public void handle(ProxyResponse proxyResponse) {
            handle(responseHandler, proxyResponse);
        }

        protected void handle(Handler<ProxyResponse> responseHandler, ProxyResponse proxyResponse) {
            context.getInvokerResponse().setHeaders(proxyResponse.headers());
            context.getInvokerResponse().setStatus(proxyResponse.status());
            responseHandler.handle(new DebugProxyResponse(proxyResponse, context));
        }
    }

    static class DebugProxyResponse implements ProxyResponse {

        private final ProxyResponse proxyResponse;
        private final DebugExecutionContext context;

        DebugProxyResponse(final ProxyResponse proxyResponse, final DebugExecutionContext context) {
            this.proxyResponse = proxyResponse;
            this.context = context;
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            proxyResponse.bodyHandler(
                chunk -> {
                    context.getInvokerResponse().getBuffer().appendBuffer(chunk);
                    bodyHandler.handle(chunk);
                }
            );

            return this;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            proxyResponse.endHandler(endHandler::handle);

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
        public boolean connected() {
            return proxyResponse.connected();
        }
    }
}
