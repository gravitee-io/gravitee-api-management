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
package io.gravitee.gateway.core.logging;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.RequestWrapper;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientRequest extends RequestWrapper {

    private final Log log;
    private Buffer buffer;

    public LoggableClientRequest(final Request request) {
        super(request);
        this.log = new Log(request.metrics().timestamp().toEpochMilli());
        this.log.setRequestId(request.id());

        // Associate log
        this.request.metrics().setLog(log);

        // Create a copy of HTTP request headers
        log.setClientRequest(new io.gravitee.reporter.api.common.Request());
        log.getClientRequest().setMethod(this.method());
        String uri = null;
        if (this.headers().containsKey("X-Forwarded-Proto")) {
            uri = this.headers().getFirst("X-Forwarded-Proto");
        } else {
            uri = request.scheme();
        }
        uri += "://" + this.headers().getFirst("host") + this.uri();

        //if(request.headers['X-Forwarded-Prefix'])
        log.getClientRequest().setUri(uri);
        log.getClientRequest().setHeaders(new HttpHeaders(this.headers()));
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        request.bodyHandler(chunk -> {
            if (buffer == null) {
                buffer = Buffer.buffer();
            }
            bodyHandler.handle(chunk);
            appendLog(buffer, chunk);
        });

        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        request.endHandler(result -> {
            if (buffer != null) {
                log.getClientRequest().setBody(buffer.toString());
            }

            endHandler.handle(result);
        });

        return this;
    }

    protected void appendLog(Buffer buffer, Buffer chunk) {
        buffer.appendBuffer(chunk);
    }
}
