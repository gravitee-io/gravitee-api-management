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
package io.gravitee.gateway.core.failover;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.invoker.EndpointInvoker;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FailoverInvoker extends EndpointInvoker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailoverInvoker.class);
    private static final String errorMessageFormat = "[%s] %s";

    private final Vertx vertx;

    private final FailoverOptions options;

    private CircuitBreaker circuitBreaker;

    public FailoverInvoker(final Vertx vertx, final EndpointResolver endpointResolver, final FailoverOptions options) {
        super(endpointResolver);
        this.vertx = vertx;
        this.options = options;

        this.initialize();
    }

    @Override
    public void invoke(ExecutionContext context, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        ((MutableExecutionContext) context).request(new FailoverRequest(context.request()));

        final String apiId = String.valueOf(context.getAttribute(ExecutionContext.ATTR_API));
        circuitBreaker
            .execute(
                new io.vertx.core.Handler<Promise<ProxyConnection>>() {
                    @Override
                    public void handle(Promise<ProxyConnection> event) {
                        FailoverInvoker.super.invoke(
                            context,
                            stream,
                            proxyConnection -> {
                                proxyConnection.exceptionHandler(
                                    error -> {
                                        try {
                                            event.fail(error);
                                        } catch (IllegalStateException e) {
                                            final Future<ProxyConnection> future = event.future();
                                            if (future.failed()) {
                                                LOGGER.error(
                                                    String.format(
                                                        errorMessageFormat,
                                                        apiId,
                                                        future.cause() == null ? null : future.cause().getMessage()
                                                    ),
                                                    future.cause()
                                                );
                                            } else {
                                                LOGGER.error(String.format(errorMessageFormat, apiId, error.getMessage()), e);
                                            }
                                            throw e; // rethrow the exception to let vertx handle this case
                                        }
                                    }
                                );
                                proxyConnection.responseHandler(
                                    response -> {
                                        try {
                                            event.complete(new FailoverProxyConnection(proxyConnection, response));
                                        } catch (IllegalStateException e) {
                                            final Future<ProxyConnection> future = event.future();
                                            if (future.failed()) {
                                                LOGGER.error(
                                                    String.format(
                                                        errorMessageFormat,
                                                        apiId,
                                                        future.cause() == null ? null : future.cause().getMessage()
                                                    ),
                                                    future.cause()
                                                );
                                            } else {
                                                LOGGER.error(
                                                    String.format(
                                                        errorMessageFormat,
                                                        apiId,
                                                        "Failover invocation has succeeded but result already completed"
                                                    ),
                                                    e
                                                );
                                            }
                                            throw e; // rethrow the exception to let vertx handle this case
                                        }
                                    }
                                );
                            }
                        );
                    }
                }
            )
            .onComplete(
                event -> {
                    if (event.failed()) {
                        FailoverConnection connection = new FailoverConnection();
                        connectionHandler.handle(connection);
                        connection.sendBadGatewayResponse();
                    } else {
                        FailoverProxyConnection proxyConnection = (FailoverProxyConnection) event.result();
                        connectionHandler.handle(proxyConnection);
                        proxyConnection.sendResponse();
                    }
                }
            );
    }

    private void initialize() {
        circuitBreaker =
            CircuitBreaker.create(
                "cb-" + options.hashCode(),
                vertx,
                new CircuitBreakerOptions()
                    .setMaxRetries(options.getMaxAttempts()) // number of failure before opening the circuit
                    .setTimeout(options.getRetryTimeout()) // consider a failure if the operation does not succeed in time
                    .setResetTimeout(10000L) // time spent in open state before attempting to re-try
                    .setNotificationAddress(null)
            );
    }

    private class FailoverConnection implements ProxyConnection {

        private Handler<ProxyResponse> responseHandler;

        @Override
        public WriteStream<Buffer> write(Buffer content) {
            return this;
        }

        @Override
        public void end() {}

        @Override
        public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        private void sendBadGatewayResponse() {
            FailoverClientResponse response = new FailoverClientResponse();
            this.responseHandler.handle(response);
            response.endHandler().handle(null);
        }
    }

    private static class FailoverClientResponse implements ProxyResponse {

        private Handler<Void> endHandler;

        @Override
        public int status() {
            return HttpStatusCode.BAD_GATEWAY_502;
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.create();
        }

        @Override
        public ProxyResponse bodyHandler(Handler<Buffer> bodyHandler) {
            // No need to record this handler because no data will be handle
            return this;
        }

        @Override
        public ProxyResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        Handler<Void> endHandler() {
            return this.endHandler;
        }
    }
}
