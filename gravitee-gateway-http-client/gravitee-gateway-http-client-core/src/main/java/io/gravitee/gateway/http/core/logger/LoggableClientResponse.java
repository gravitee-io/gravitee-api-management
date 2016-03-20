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
import io.gravitee.gateway.api.ClientResponse;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;

import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class LoggableClientResponse implements ClientResponse {

    private final ClientResponse clientResponse;
    private final Request request;

    public LoggableClientResponse(final ClientResponse clientResponse, final Request request) {
        this.clientResponse = clientResponse;
        this.request = request;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        return clientResponse.bodyHandler(chunk -> {
            HttpDump.logger.info("{} << proxying content to downstream: {} bytes", request.id(),
                    chunk.length());
            HttpDump.logger.info("{} << {}", request.id(), chunk.toString());

            bodyHandler.handle(chunk);
        });
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        return clientResponse.endHandler(result -> {
            HttpDump.logger.info("{} << downstream proxying complete", request.id());

            endHandler.handle(result);
        });
    }

    @Override
    public HttpHeaders headers() {
        clientResponse.headers().forEach((headerName, headerValues) -> HttpDump.logger.info("{} << {}: {}",
                request.id(), headerName, headerValues.stream().collect(Collectors.joining(","))));
        return clientResponse.headers();
    }

    @Override
    public int status() {
        HttpDump.logger.info("{} << HTTP Status - {}", request.id(), clientResponse.status());
        return clientResponse.status();
    }
}
