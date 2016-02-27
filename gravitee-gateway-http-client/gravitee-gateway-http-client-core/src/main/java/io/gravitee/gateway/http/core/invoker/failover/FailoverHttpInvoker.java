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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Failover;
import io.gravitee.gateway.api.ClientRequest;
import io.gravitee.gateway.api.ClientResponse;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.gateway.http.core.invoker.AbstractHttpInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class FailoverHttpInvoker extends AbstractHttpInvoker {

    private final Logger LOGGER = LoggerFactory.getLogger(FailoverHttpInvoker.class);

    private final static String ATTEMPTS_COUNTER_ATTRIBUTE = "gravitee.attribute.failover.attempts";

    @Override
    protected ClientRequest invoke0(String host, int port, HttpMethod method, String requestUri, Request serverRequest,
                                    ExecutionContext executionContext, Handler<ClientResponse> response) {
        return httpClient.request(host, port, method, requestUri, serverRequest,
                response::handle).connectTimeoutHandler(result -> {
            LOGGER.warn("Connection timeout from {}:{}", host, port);
            int attempts = getAttempts(executionContext);
            int maxAttempts = getFailover().getMaxAttempts();

            LOGGER.debug("Current attempts is {} (max={})", attempts, maxAttempts);

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
    protected String nextEndpoint(ExecutionContext executionContext, Request serverRequest) {
        // Use this is to iterate over each defined endpoint
        // How to maintain a list of already attempted endpoints ?
        return super.nextEndpoint(executionContext, serverRequest);
    }

    private Failover getFailover() {
        return api.getProxy().getFailover();
    }

    private class FailoverClientResponse implements ClientResponse {

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
        public ClientResponse bodyHandler(Handler<BodyPart> bodyPartHandler) {
            // No need to record this handler because no data will be handle
            return this;
        }

        @Override
        public ClientResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        Handler<Void> endHandler() {
            return this.endHandler;
        }
    }
}
