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
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.reporter.api.http.Metrics;
import io.netty.buffer.ByteBuf;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse implements Response {

    private final HttpServerResponse httpServerResponse;

    private final HttpHeaders headers = new HttpHeaders();

    private final Metrics metrics;

    private final HttpVersion version;

    public VertxHttpServerResponse(final HttpServerRequest httpServerRequest, final Metrics metrics) {
        this.httpServerResponse = httpServerRequest.response();
        version = httpServerRequest.version();
        this.metrics = metrics;
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
    public boolean ended() {
        return httpServerResponse.ended();
    }

    @Override
    public Response write(Buffer chunk) {
        if (valid()) {
            if (!httpServerResponse.headWritten()) {
                writeHeaders();

                // Vertx requires to set the chunked flag if transfer_encoding header as the "chunked" value
                String transferEncodingHeader = headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
                if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncodingHeader)) {
                    httpServerResponse.setChunked(true);
                } else if (transferEncodingHeader == null) {
                    String connectionHeader = headers().getFirst(HttpHeaders.CONNECTION);
                    String contentLengthHeader = headers().getFirst(HttpHeaders.CONTENT_LENGTH);
                    if (HttpHeadersValues.CONNECTION_CLOSE.equalsIgnoreCase(connectionHeader)
                            && contentLengthHeader == null) {
                        httpServerResponse.setChunked(true);
                    }
                }
            }

            metrics.setResponseContentLength(metrics.getResponseContentLength() + chunk.length());
            httpServerResponse.write(io.vertx.core.buffer.Buffer.buffer((ByteBuf) chunk.getNativeBuffer()));
        }
        return this;
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        httpServerResponse.drainHandler((aVoid -> drainHandler.handle(null)));
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return valid() && httpServerResponse.writeQueueFull();
    }

    @Override
    public void end() {
        if (valid()) {
            if (!httpServerResponse.headWritten()) {
                writeHeaders();
            }

            httpServerResponse.end();
        }
    }

    private boolean valid() {
        return !httpServerResponse.closed() && !httpServerResponse.ended();
    }

    private void writeHeaders() {
        // As per https://tools.ietf.org/html/rfc7540#section-8.1.2.2
        // connection-specific header fields must be remove from response headers
        headers.forEach((headerName, headerValues) -> {
            if (version == HttpVersion.HTTP_1_0 || version == HttpVersion.HTTP_1_1
                    || (!headerName.equalsIgnoreCase(HttpHeaders.CONNECTION)
                    && !headerName.equalsIgnoreCase(HttpHeaders.KEEP_ALIVE)
                    && !headerName.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING))) {
                httpServerResponse.putHeader(headerName, headerValues);
            }
        });
    }
}
