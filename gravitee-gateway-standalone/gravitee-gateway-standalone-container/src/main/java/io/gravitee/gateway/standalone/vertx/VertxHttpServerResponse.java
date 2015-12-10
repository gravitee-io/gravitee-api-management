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
package io.gravitee.gateway.standalone.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.reporter.api.metrics.Metrics;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxHttpServerResponse implements Response {

    private final HttpServerResponse httpServerResponse;

    private final HttpHeaders headers = new HttpHeaders();

    private final Metrics metrics = new Metrics();

    private boolean headersWritten = false;

    VertxHttpServerResponse(HttpServerResponse httpServerResponse) {
        this.httpServerResponse = httpServerResponse;
    }

    @Override
    public int status() {
        return httpServerResponse.getStatusCode();
    }

    @Override
    public Response status(int statusCode) {
        httpServerResponse.setStatusCode(statusCode);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Response write(BodyPart bodyPart) {
        if (! headersWritten) {
            writeHeaders();

            // Vertx requires to set the chunked flag if transfer_encoding header as the "chunked" value
            String transferEncoding = headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
            if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                httpServerResponse.setChunked(true);
            }
        }

        httpServerResponse.write(Buffer.buffer(bodyPart.getBodyPartAsBytes()));
        return this;
    }

    @Override
    public void end() {
        if (! headersWritten) {
            writeHeaders();
        }

        httpServerResponse.end();
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    private void writeHeaders() {
        headers.entrySet().forEach(
                headerEntry -> httpServerResponse.putHeader(headerEntry.getKey(), headerEntry.getValue()));
        headersWritten = true;
    }
}
