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
package io.gravitee.gateway.handlers.api.logging;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.reporter.api.log.Log;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LoggableClientResponse implements Response {

    private final Response response;
    private final Request request;
    private final Log log;
    private Buffer buffer;

    public LoggableClientResponse(final Request request, final Response response) {
        this.request = request;
        this.response = response;
        this.log = this.request.metrics().getLog();
    }

    @Override
    public WriteStream<Buffer> write(Buffer content) {
        if (buffer == null) {
            buffer = Buffer.buffer();
        }

        buffer.appendBuffer(content);

        return response.write(content);
    }

    @Override
    public Response status(int statusCode) {
        log.setClientResponse(new io.gravitee.reporter.api.common.Response(statusCode));
        return response.status(statusCode);
    }

    @Override
    public void end() {
        calculate(buffer);
        response.end();
    }

    @Override
    public void end(Buffer buffer) {
        calculate(buffer);
        response.end(buffer);
    }

    private void calculate(Buffer buffer) {
        // Here we are sure that headers has been full processed by policies
        log.setClientResponse(new io.gravitee.reporter.api.common.Response(status()));
        log.getClientResponse().setHeaders(headers());

        if (buffer != null) {
            log.getClientResponse().setBody(buffer.toString());
        }
    }

    @Override
    public int status() {
        return response.status();
    }

    @Override
    public HttpHeaders headers() {
        return response.headers();
    }
}
