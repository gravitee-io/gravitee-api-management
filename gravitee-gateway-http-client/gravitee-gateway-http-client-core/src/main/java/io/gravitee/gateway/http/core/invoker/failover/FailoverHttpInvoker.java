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
package io.gravitee.gateway.http.core.invoker.failover;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Failover;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyRequestConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.http.core.invoker.DefaultHttpInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FailoverHttpInvoker extends DefaultHttpInvoker {

    private final Logger LOGGER = LoggerFactory.getLogger(FailoverHttpInvoker.class);

    private final static String ATTEMPTS_COUNTER_ATTRIBUTE = "gravitee.attribute.failover.attempts";

    @Override
    protected ProxyRequestConnection invoke0(HttpClient httpClient, Request serverRequest, ProxyRequest proxyRequest,
                                             ExecutionContext executionContext, Handler<ProxyResponse> response) {
        return httpClient.request(proxyRequest, response).connectTimeoutHandler(result -> {
            LOGGER.warn("Connection timeout from {}:{}", proxyRequest.uri().getHost(), proxyRequest.uri().getPort());
            int attempts = getAttempts(executionContext);
            int maxAttempts = getFailover().getMaxAttempts();

            LOGGER.debug("Current attempt is {} (max={})", attempts, maxAttempts);

            if (maxAttempts == 0 || attempts < maxAttempts) {
                invoke(executionContext, serverRequest, response);
            } else {
                LOGGER.warn("Failover reach max attempts limit ({})", maxAttempts);
                FailoverClientResponse clientResponse = new FailoverClientResponse();

                // Returning the last response from upstream
                response.handle(clientResponse);
                clientResponse.endHandler().handle(null);
            }
        });
    }

    private int getAttempts(ExecutionContext executionContext) {
        Object attrAttempts = executionContext.getAttribute(ATTEMPTS_COUNTER_ATTRIBUTE);
        int attempts = 1;
        if (attrAttempts != null) {
            attempts = ((int) attrAttempts) + 1;
        }

        executionContext.setAttribute(ATTEMPTS_COUNTER_ATTRIBUTE, attempts);
        return attempts;
    }

    @Override
    protected String nextEndpoint(ExecutionContext executionContext) {
        // Use this is to iterate over each defined endpoint
        // How to maintain a list of already attempted endpoints ?
        return endpointManager.loadbalancer().next();
    }

    private Failover getFailover() {
        return api.getProxy().getFailover();
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
