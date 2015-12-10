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
package io.gravitee.gateway.core.reactor.handler;

import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ResponseTimeHandler implements Handler<Response> {

    private final Handler<Response> wrappedHandler;
    private final long proxyInvocationStart;

    public ResponseTimeHandler(Handler<Response> wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
        this.proxyInvocationStart = System.currentTimeMillis();
    }

    @Override
    public void handle(Response response) {
        // Provide the response to the wrapped handler
        wrappedHandler.handle(response);

        // Compute response-time and add it to the metrics
        long proxyResponseTimeInMs = System.currentTimeMillis() - proxyInvocationStart;
        response.metrics().setProxyResponseTimeMs(proxyResponseTimeInMs);

        response.metrics().setResponseContentLength(response.headers().contentLength());
        response.metrics().setResponseContentType(response.headers().contentType());
        response.metrics().setResponseHttpStatus(response.status());
    }
}