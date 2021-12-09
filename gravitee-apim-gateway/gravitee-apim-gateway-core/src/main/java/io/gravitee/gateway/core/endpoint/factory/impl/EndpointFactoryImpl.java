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
package io.gravitee.gateway.core.endpoint.factory.impl;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.connector.api.Connection;
import io.gravitee.connector.api.Connector;
import io.gravitee.connector.api.Response;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.endpoint.ManagedEndpoint;
import io.gravitee.gateway.core.endpoint.factory.EndpointFactory;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointFactoryImpl implements EndpointFactory {

    @Override
    public io.gravitee.gateway.api.endpoint.Endpoint create(Endpoint endpoint, Connector<Connection, ProxyRequest> connector) {
        return new ManagedEndpoint(
            endpoint,
            new io.gravitee.gateway.api.Connector() {
                @Override
                public void request(ProxyRequest request, ExecutionContext context, Handler<ProxyConnection> proxyConnectionHandler) {
                    connector.request(context, request, result -> proxyConnectionHandler.handle(new ConnectorProxyConnection(result)));
                }

                @Override
                public Lifecycle.State lifecycleState() {
                    return connector.lifecycleState();
                }

                @Override
                public io.gravitee.gateway.api.Connector start() throws Exception {
                    connector.start();
                    return this;
                }

                @Override
                public io.gravitee.gateway.api.Connector stop() throws Exception {
                    connector.stop();
                    return this;
                }
            }
        );
    }

    private static class ConnectorProxyConnection implements ProxyConnection {

        public ConnectorProxyConnection(Connection connection) {
            this.connection = connection;
        }

        private final Connection connection;

        @Override
        public ProxyConnection writeCustomFrame(HttpFrame frame) {
            connection.writeCustomFrame(frame);
            return this;
        }

        @Override
        public ProxyConnection cancel() {
            connection.cancel();
            return this;
        }

        @Override
        public ProxyConnection cancelHandler(Handler<Void> cancelHandler) {
            connection.cancelHandler(cancelHandler);
            return this;
        }

        @Override
        public ProxyConnection exceptionHandler(Handler<Throwable> exceptionHandler) {
            connection.exceptionHandler(exceptionHandler);
            return this;
        }

        @Override
        public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
            connection.responseHandler(
                response -> {
                    if (response instanceof ProcessorFailure) {
                        responseHandler.handle(new ConnectionProxyErrorResponse(response));
                    } else {
                        responseHandler.handle(new ConnectionProxyResponse(response));
                    }
                }
            );
            return this;
        }

        @Override
        public WriteStream<Buffer> write(Buffer content) {
            connection.write(content);
            return this;
        }

        @Override
        public void end() {
            connection.end();
        }

        @Override
        public void end(Buffer buffer) {
            connection.end(buffer);
        }

        @Override
        public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
            connection.drainHandler(drainHandler);

            return this;
        }

        @Override
        public boolean writeQueueFull() {
            return connection.writeQueueFull();
        }
    }

    private static class ConnectionProxyResponse implements ProxyResponse {

        final Response response;

        private ConnectionProxyResponse(Response response) {
            this.response = response;
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
        public HttpHeaders headers() {
            return response.headers();
        }

        @Override
        public boolean connected() {
            return response.connected();
        }

        @Override
        public ProxyResponse customFrameHandler(Handler<HttpFrame> frameHandler) {
            response.customFrameHandler(frameHandler);
            return this;
        }

        @Override
        public HttpHeaders trailers() {
            return response.trailers();
        }

        @Override
        public ProxyResponse cancelHandler(Handler<Void> cancelHandler) {
            response.cancelHandler(cancelHandler);
            return this;
        }

        @Override
        public void cancel() {
            response.cancel();
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            response.bodyHandler(bodyHandler);
            return this;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            response.endHandler(endHandler);
            return this;
        }

        @Override
        public ReadStream<Buffer> pause() {
            response.pause();
            return this;
        }

        @Override
        public ReadStream<Buffer> resume() {
            response.resume();
            return this;
        }
    }

    private static class ConnectionProxyErrorResponse extends ConnectionProxyResponse implements ProcessorFailure {

        private ConnectionProxyErrorResponse(Response response) {
            super(response);
        }

        @Override
        public int statusCode() {
            return ((ProcessorFailure) this.response).statusCode();
        }

        @Override
        public String message() {
            return ((ProcessorFailure) this.response).message();
        }

        @Override
        public String key() {
            return ((ProcessorFailure) this.response).key();
        }

        @Override
        public Map<String, Object> parameters() {
            return ((ProcessorFailure) this.response).parameters();
        }

        @Override
        public String contentType() {
            return ((ProcessorFailure) this.response).contentType();
        }
    }
}
