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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Failover;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.core.invoker.DefaultInvoker;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FailoverInvoker extends DefaultInvoker implements InitializingBean {

    @Autowired
    private Vertx vertx;

    private CircuitBreaker circuitBreaker;

    @Override
    public Request invoke(ExecutionContext executionContext, Request serverRequest, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        final Request failoverServerRequest = new FailoverRequest(serverRequest);

        circuitBreaker.execute(new io.vertx.core.Handler<Future<ProxyConnection>>() {
            @Override
            public void handle(Future<ProxyConnection> event) {
                FailoverInvoker.super.invoke(executionContext, failoverServerRequest, stream, proxyConnection -> {
                    proxyConnection.exceptionHandler(event::fail);
                    proxyConnection.responseHandler(
                            response -> event.complete(new FailoverProxyConnection(proxyConnection, response)));
                });
            }
        }).setHandler(new io.vertx.core.Handler<AsyncResult<ProxyConnection>>() {
            @Override
            public void handle(AsyncResult<ProxyConnection> event) {
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
        });

        return failoverServerRequest;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Failover failover = api.getProxy().getFailover();

        circuitBreaker = CircuitBreaker.create("cb-" + api.getId(),
                vertx,
                new CircuitBreakerOptions()
                        .setMaxRetries(failover.getMaxAttempts()) // number of failure before opening the circuit
                        .setTimeout(failover.getRetryTimeout()) // consider a failure if the operation does not succeed in time
                        .setResetTimeout(10000L) // time spent in open state before attempting to re-try
                        .setNotificationAddress(null));
    }

    private class FailoverConnection implements ProxyConnection {

        private Handler<ProxyResponse> responseHandler;

        @Override
        public WriteStream<Buffer> write(Buffer content) {
            return null;
        }

        @Override
        public void end() {

        }

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

    private class FailoverClientResponse implements ProxyResponse {

        private Handler<Void> endHandler;

        @Override
        public int status() {
            return HttpStatusCode.BAD_GATEWAY_502;
        }

        @Override
        public HttpHeaders headers() {
            return new HttpHeaders();
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
