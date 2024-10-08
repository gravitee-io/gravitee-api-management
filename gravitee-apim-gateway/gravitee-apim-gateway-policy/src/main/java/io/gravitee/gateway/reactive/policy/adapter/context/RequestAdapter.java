/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.gravitee.reporter.api.http.Metrics;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.SSLSession;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RequestAdapter implements io.gravitee.gateway.api.Request {

    private final HttpPlainRequest request;
    private MetricsAdapter metrics;
    private Runnable onResumeHandler;
    private Handler<Buffer> bodyHandler;
    private Handler<Void> endHandler;
    private WebSocketAdapter adaptedWebSocket;
    private final AtomicBoolean hasBeenResumed;

    public RequestAdapter(HttpPlainRequest request, final io.gravitee.reporter.api.v4.metric.Metrics metrics) {
        this.request = request;
        if (metrics != null) {
            this.metrics = new MetricsAdapter(metrics);
        }
        this.hasBeenResumed = new AtomicBoolean(false);
    }

    public void onResume(Runnable onResume) {
        this.onResumeHandler = onResume;
    }

    @Override
    public ReadStream<Buffer> resume() {
        if (hasBeenResumed.compareAndSet(false, true)) {
            onResumeHandler.run();
        } else {
            ((VertxHttpServerRequest) request).resume();
        }
        return this;
    }

    @Override
    public ReadStream<Buffer> pause() {
        ((VertxHttpServerRequest) request).pause();
        return this;
    }

    @Override
    public String id() {
        return request.id();
    }

    @Override
    public String transactionId() {
        return request.transactionId();
    }

    @Override
    public String uri() {
        return request.uri();
    }

    @Override
    public String host() {
        return request.host();
    }

    @Override
    public String path() {
        return request.path();
    }

    @Override
    public String pathInfo() {
        return request.pathInfo();
    }

    @Override
    public String contextPath() {
        return request.contextPath();
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        return request.parameters();
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        return request.pathParameters();
    }

    @Override
    public HttpHeaders headers() {
        return request.headers();
    }

    @Override
    public HttpMethod method() {
        return request.method();
    }

    @Override
    public String scheme() {
        return request.scheme();
    }

    @Override
    public HttpVersion version() {
        return request.version();
    }

    @Override
    public long timestamp() {
        return request.timestamp();
    }

    @Override
    public String remoteAddress() {
        return request.remoteAddress();
    }

    @Override
    public String localAddress() {
        return request.localAddress();
    }

    @Override
    public SSLSession sslSession() {
        return request.tlsSession();
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public boolean ended() {
        return request.ended();
    }

    @Override
    public io.gravitee.gateway.api.Request timeoutHandler(Handler<Long> timeoutHandler) {
        return this;
    }

    @Override
    public Handler<Long> timeoutHandler() {
        return null;
    }

    @Override
    public boolean isWebSocket() {
        return request.isWebSocket();
    }

    @Override
    public WebSocket websocket() {
        if (this.adaptedWebSocket == null) {
            this.adaptedWebSocket = new WebSocketAdapter(request.webSocket());
        }
        return adaptedWebSocket;
    }

    @Override
    public io.gravitee.gateway.api.Request customFrameHandler(Handler<HttpFrame> frameHandler) {
        return this;
    }

    @Override
    public io.gravitee.gateway.api.Request closeHandler(Handler<Void> closeHandler) {
        return this;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        this.bodyHandler = bodyHandler;
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        this.endHandler = endHandler;
        return this;
    }

    public Handler<Buffer> getBodyHandler() {
        return bodyHandler;
    }

    public void setBodyHandler(Handler<Buffer> bodyHandler) {
        this.bodyHandler = bodyHandler;
    }

    public Handler<Void> getEndHandler() {
        return endHandler;
    }
}
