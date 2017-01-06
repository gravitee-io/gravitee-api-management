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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ResponseTimeHandler implements Handler<Response> {

    private final Handler<Response> next;
    private final Request serverRequest;

    public ResponseTimeHandler(final Request serverRequest, final Handler<Response> next) {
        this.serverRequest = serverRequest;
        this.next = next;
    }

    @Override
    public void handle(Response response) {
        // Compute response-time and add it to the metrics
        long proxyResponseTimeInMs = System.currentTimeMillis() - serverRequest.metrics().timestamp().toEpochMilli();
        serverRequest.metrics().setResponseHttpStatus(response.status());
        serverRequest.metrics().setProxyResponseTimeMs(proxyResponseTimeInMs);
        serverRequest.metrics().setProxyLatencyMs(proxyResponseTimeInMs - serverRequest.metrics().getApiResponseTimeMs());
        serverRequest.metrics().setRequestContentType(serverRequest.headers().contentType());
        serverRequest.metrics().setResponseContentType(response.headers().contentType());

        // Push response to the next handler
        next.handle(response);
    }
}