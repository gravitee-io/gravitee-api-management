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
package io.gravitee.gateway.http.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.vertx.core.http.HttpClientRequest;

import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class VertxProxyConnection implements ProxyConnection {

    private final HttpClientRequest httpClientRequest;
    private final ProxyRequest proxyRequest;
    private ProxyResponse proxyResponse;
    private Handler<Throwable> timeoutHandler;
    private Handler<ProxyResponse> responseHandler;
    private boolean canceled = false;
    private boolean transmitted = false;
    private boolean headersWritten = false;

    VertxProxyConnection(final ProxyRequest proxyRequest, final HttpClientRequest httpClientRequest) {
        this.proxyRequest = proxyRequest;
        this.httpClientRequest = httpClientRequest;
    }

    public void setProxyResponse(ProxyResponse proxyResponse) {
        this.proxyResponse = proxyResponse;
    }

    @Override
    public ProxyConnection cancel() {
        this.canceled = true;
        this.httpClientRequest.reset();
        if (proxyResponse != null) {
            proxyResponse.bodyHandler(null);
        }
        return this;
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public boolean isTransmitted() {
        return transmitted;
    }

    @Override
    public VertxProxyConnection exceptionHandler(Handler<Throwable> timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return this;
    }

    @Override
    public VertxProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public void handleResponse(ProxyResponse proxyResponse) {
        transmitted = true;
        this.responseHandler.handle(proxyResponse);
    }

    public void handleConnectTimeout(Throwable throwable) {
        this.timeoutHandler.handle(throwable);
    }

    public Handler<Throwable> timeoutHandler() {
        return this.timeoutHandler;
    }

    @Override
    public VertxProxyConnection write(Buffer chunk) {
        if (! headersWritten) {
            this.writeHeaders();
        }

        httpClientRequest.write(io.vertx.core.buffer.Buffer.buffer(chunk.getBytes()));

        return this;
    }

    private void writeHeaders() {
        // Copy headers to upstream
        copyRequestHeaders(proxyRequest.headers(), httpClientRequest);

        // Check chunk flag on the request if there are some content to push and if transfer_encoding is set
        // with chunk value
        if (hasContent(proxyRequest.headers())) {
            String contentLengthHeader = proxyRequest.headers().getFirst(HttpHeaders.CONTENT_LENGTH);
            if (contentLengthHeader == null) {
                // No content-length... so let's go for chunked transfer-encoding
                httpClientRequest.setChunked(true);
            }
        }

        headersWritten = true;
    }

    private boolean hasContent(HttpHeaders headers) {
        return headers.contentLength() > 0 || headers.contentType() != null;
    }

    private void copyRequestHeaders(HttpHeaders headers, HttpClientRequest httpClientRequest) {
        for (Map.Entry<String, List<String>> headerValues : headers.entrySet()) {
            httpClientRequest.putHeader(headerValues.getKey(), headerValues.getValue());
        }
    }

    @Override
    public void end() {
        if (! headersWritten) {
            this.writeHeaders();
        }

        httpClientRequest.end();
    }
}
