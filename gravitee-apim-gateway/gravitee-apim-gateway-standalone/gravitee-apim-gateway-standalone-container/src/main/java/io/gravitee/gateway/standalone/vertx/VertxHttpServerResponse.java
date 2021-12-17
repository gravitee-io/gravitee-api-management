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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.WriteStream;
import io.netty.buffer.ByteBuf;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerResponse implements Response {

    protected final HttpServerResponse serverResponse;
    private final Request serverRequest;
    protected final HttpHeaders headers = new HttpHeaders();

    protected HttpHeaders trailers;

    public VertxHttpServerResponse(final VertxHttpServerRequest serverRequest) {
        this.serverRequest = serverRequest;
        this.serverResponse = serverRequest.getNativeServerRequest().response();
    }

    @Override
    public int status() {
        return serverResponse.getStatusCode();
    }

    @Override
    public String reason() {
        return serverResponse.getStatusMessage();
    }

    @Override
    public Response reason(String reason) {
        if (reason != null) {
            serverResponse.setStatusMessage(reason);
        }
        return this;
    }

    @Override
    public Response status(int statusCode) {
        serverResponse.setStatusCode(statusCode);
        return this;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public boolean ended() {
        return serverResponse.ended();
    }

    @Override
    public HttpHeaders trailers() {
        if (trailers == null) {
            trailers = new HttpHeaders();
        }
        return trailers;
    }

    @Override
    public Response write(Buffer chunk) {
        if (valid()) {
            if (!serverResponse.headWritten()) {
                writeHeaders();

                // Vertx requires to set the chunked flag if transfer_encoding header as the "chunked" value
                String transferEncodingHeader = headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
                if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncodingHeader)) {
                    serverResponse.setChunked(true);
                } else if (transferEncodingHeader == null) {
                    String connectionHeader = headers().getFirst(HttpHeaders.CONNECTION);
                    String contentLengthHeader = headers().getFirst(HttpHeaders.CONTENT_LENGTH);
                    if (contentLengthHeader == null) {
                        serverResponse.setChunked(true);
                    }
                }
            }

            serverRequest.metrics().setResponseContentLength(serverRequest.metrics().getResponseContentLength() + chunk.length());
            serverResponse.write(io.vertx.core.buffer.Buffer.buffer((ByteBuf) chunk.getNativeBuffer()));
        }
        return this;
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        serverResponse.drainHandler((aVoid -> drainHandler.handle(null)));
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return valid() && serverResponse.writeQueueFull();
    }

    @Override
    public void end() {
        if (valid()) {
            if (!serverResponse.headWritten()) {
                writeHeaders();
            }

            writeTrailers();

            serverResponse
                .end()
                .onComplete(
                    event -> {
                        if (this.endHandler != null) {
                            this.endHandler.handle(null);
                        }
                    }
                );
        }
    }

    Handler<Void> endHandler = null;

    @Override
    public Response endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    protected void writeTrailers() {
        if (trailers != null) {
            trailers.forEach(serverResponse::putTrailer);
        }
    }

    private boolean valid() {
        return !serverResponse.closed() && !serverResponse.ended();
    }

    protected void writeHeaders() {
        headers.forEach(serverResponse::putHeader);
    }

    public HttpConnection getNativeConnection() {
        return ((VertxHttpServerRequest) serverRequest).getNativeServerRequest().connection();
    }
}
