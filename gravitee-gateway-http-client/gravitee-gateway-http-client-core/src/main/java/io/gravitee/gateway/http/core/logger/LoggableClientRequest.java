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

import io.gravitee.gateway.api.ClientRequest;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.WriteStream;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientRequest implements ClientRequest {

    private final ClientRequest clientRequest;
    private final Request request;

    public LoggableClientRequest(final ClientRequest clientRequest, final Request request) {
        this.clientRequest = clientRequest;
        this.request = request;
    }

    @Override
    public ClientRequest connectTimeoutHandler(Handler<Throwable> timeoutHandler) {
        return clientRequest.connectTimeoutHandler(timeoutHandler);
    }

    @Override
    public void end() {
        HttpDump.logger.info("{}/{} >> upstream proxying complete", request.id(), request.transactionId());

        clientRequest.end();
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        HttpDump.logger.info("{}/{} >> proxying content to upstream: {} bytes", request.id(), request.transactionId(),
                chunk.length());
        HttpDump.logger.info("{}/{} >> {}", request.id(), request.transactionId(), chunk.toString());

        return clientRequest.write(chunk);
    }
}
