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

import java.util.function.BiConsumer;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ResponseTimeHandler implements BiConsumer<Response, Throwable> {

    private final Request serverRequest;

    public ResponseTimeHandler(final Request serverRequest) {
        this.serverRequest = serverRequest;
    }

    @Override
    public void accept(Response response, Throwable throwable) {
        try {
            // Compute response-time and add it to the metrics
            long proxyResponseTimeInMs = System.currentTimeMillis() - serverRequest.metrics().timestamp().toEpochMilli();

            serverRequest.metrics().setProxyResponseTimeMs(proxyResponseTimeInMs);
            serverRequest.metrics().setResponseContentLength(response.headers().contentLength());
            serverRequest.metrics().setResponseContentType(response.headers().contentType());
            serverRequest.metrics().setResponseHttpStatus(response.status());
        } catch (Exception ex) {
            // Do nothing
        }
    }
}