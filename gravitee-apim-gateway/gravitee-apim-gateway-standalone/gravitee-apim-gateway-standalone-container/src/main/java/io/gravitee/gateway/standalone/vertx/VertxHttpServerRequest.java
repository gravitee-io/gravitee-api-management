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
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.reporter.api.http.Metrics;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import java.util.Map;
import javax.net.ssl.SSLSession;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Guillaume GILLON (guillaume.gillon at outlook.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest implements Request {

    private final String id;
    private final long timestamp;

    protected final HttpServerRequest serverRequest;

    private MultiValueMap<String, String> queryParameters = null;

    private MultiValueMap<String, String> pathParameters = null;

    private HttpHeaders headers = null;

    private final Metrics metrics;

    private Handler<Long> timeoutHandler;

    public VertxHttpServerRequest(HttpServerRequest httpServerRequest, IdGenerator idGenerator) {
        this.serverRequest = httpServerRequest;
        this.timestamp = System.currentTimeMillis();
        this.id = idGenerator.randomString();

        this.metrics = Metrics.on(timestamp).build();
        this.metrics.setRequestId(id());
        this.metrics.setHttpMethod(method());
        this.metrics.setLocalAddress(localAddress());
        this.metrics.setRemoteAddress(remoteAddress());
        this.metrics.setHost(serverRequest.host());
        this.metrics.setUri(uri());
        this.metrics.setUserAgent(serverRequest.getHeader(HttpHeaders.USER_AGENT));
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String transactionId() {
        throw new IllegalStateException("Request not yet managed.");
    }

    @Override
    public String uri() {
        return serverRequest.uri();
    }

    @Override
    public String path() {
        return serverRequest.path();
    }

    @Override
    public String pathInfo() {
        return path();
    }

    @Override
    public String contextPath() {
        throw new IllegalStateException("Request not yet managed.");
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        if (queryParameters == null) {
            queryParameters = URIUtils.parameters(serverRequest.uri());
        }

        return queryParameters;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        if (pathParameters == null) {
            pathParameters = new LinkedMultiValueMap<>();
        }

        return pathParameters;
    }

    @Override
    public HttpHeaders headers() {
        if (headers == null) {
            MultiMap vertxHeaders = serverRequest.headers();
            headers = new HttpHeaders(vertxHeaders.size());
            for (Map.Entry<String, String> header : vertxHeaders) {
                headers.add(header.getKey(), header.getValue());
            }
        }

        return headers;
    }

    @Override
    public HttpMethod method() {
        try {
            return HttpMethod.valueOf(serverRequest.method().name());
        } catch (IllegalArgumentException iae) {
            return HttpMethod.OTHER;
        }
    }

    @Override
    public String scheme() {
        return serverRequest.scheme();
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.valueOf(serverRequest.version().name());
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String remoteAddress() {
        SocketAddress address = serverRequest.remoteAddress();
        return (address != null) ? address.host() : null;
    }

    @Override
    public String localAddress() {
        SocketAddress address = serverRequest.localAddress();
        return (address != null) ? address.host() : null;
    }

    @Override
    public SSLSession sslSession() {
        return serverRequest.sslSession();
    }

    @Override
    public Request bodyHandler(Handler<Buffer> bodyHandler) {
        if (!serverRequest.isEnded()) {
            serverRequest.handler(
                event -> {
                    bodyHandler.handle(Buffer.buffer(event.getBytes()));
                    metrics.setRequestContentLength(metrics.getRequestContentLength() + event.length());
                }
            );
        }

        return this;
    }

    @Override
    public Request endHandler(Handler<Void> endHandler) {
        serverRequest.endHandler(
            new io.vertx.core.Handler<Void>() {
                @Override
                public void handle(Void event) {
                    endHandler.handle(event);
                }
            }
        );
        return this;
    }

    @Override
    public Request pause() {
        serverRequest.pause();
        return this;
    }

    @Override
    public Request resume() {
        serverRequest.resume();
        return this;
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public boolean ended() {
        return serverRequest.isEnded();
    }

    @Override
    public Request timeoutHandler(Handler<Long> timeoutHandler) {
        this.timeoutHandler = timeoutHandler;
        return this;
    }

    @Override
    public Handler<Long> timeoutHandler() {
        return this.timeoutHandler;
    }

    @Override
    public boolean isWebSocket() {
        return false;
    }

    @Override
    public WebSocket websocket() {
        throw new IllegalStateException();
    }

    @Override
    public Request customFrameHandler(Handler<HttpFrame> frameHandler) {
        return this;
    }

    @Override
    public Request closeHandler(Handler<Void> closeHandler) {
        serverRequest.connection().closeHandler(closeHandler::handle);
        return this;
    }

    public HttpServerRequest getNativeServerRequest() {
        return serverRequest;
    }

    public Response create() {
        return new VertxHttpServerResponse(this);
    }
}
