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
package io.gravitee.gateway.http.core.logger;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableProxyConnection implements ProxyConnection {

    private final static Logger logger = LoggerFactory.getLogger("io.gravitee.gateway.http.client");

    private final ProxyConnection proxyConnection;
    private final Request request;

    public LoggableProxyConnection(final ProxyConnection proxyConnection, final ProxyRequest proxyRequest) {
        this.proxyConnection = proxyConnection;
        this.request = proxyRequest.request();

        logger.info("{}/{} >> Rewriting: {} -> {}", request.id(), request.transactionId(),
                request.uri(), proxyRequest.uri());
        logger.info("{}/{} >> {} {}", request.id(), request.transactionId(),
                proxyRequest.method(), proxyRequest.uri().getRawPath());

        proxyRequest.headers().forEach((headerName, headerValues) -> logger.info("{}/{} >> {}: {}",
                proxyRequest.request().id(), proxyRequest.request().transactionId(), headerName,
                headerValues.stream().collect(Collectors.joining(","))));
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
        return proxyConnection.responseHandler(new LoggableProxyResponseHandler(responseHandler));
    }

    @Override
    public void end() {
        logger.info("{}/{} >> upstream proxying complete", request.id(), request.transactionId());

        proxyConnection.end();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        logger.info("{}/{} >> proxying content to upstream: {} bytes", request.id(), request.transactionId(),
                chunk.length());
        logger.info("{}/{} >> {}", request.id(), request.transactionId(), chunk.toString());

        return proxyConnection.write(chunk);
    }

    class LoggableProxyResponseHandler implements Handler<ProxyResponse> {
        private final Handler<ProxyResponse> responseHandler;

        LoggableProxyResponseHandler(final Handler<ProxyResponse> responseHandler) {
            this.responseHandler = responseHandler;
        }

        @Override
        public void handle(ProxyResponse proxyResponse) {
            responseHandler.handle(new LoggableProxyResponse(proxyResponse));
        }
    }

    class LoggableProxyResponse implements ProxyResponse {

        private final ProxyResponse proxyResponse;

        LoggableProxyResponse(final ProxyResponse proxyResponse) {
            this.proxyResponse = proxyResponse;

            proxyResponse.headers().forEach((headerName, headerValues) -> logger.info("{} << {}: {}",
                    request.id(), headerName, headerValues.stream().collect(Collectors.joining(","))));

            logger.info("{} << HTTP Status - {}", request.id(), proxyResponse.status());
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            return proxyResponse.bodyHandler(chunk -> {
                logger.info("{}/{} << proxying content to downstream: {} bytes", request.id(),
                        request.transactionId(), chunk.length());
                logger.info("{}/{} << {}", request.id(), request.transactionId(), chunk.toString());

                bodyHandler.handle(chunk);
            });
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            return proxyResponse.endHandler(result -> {
                logger.info("{}/{} << downstream proxying complete", request.id(), request.transactionId());

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
