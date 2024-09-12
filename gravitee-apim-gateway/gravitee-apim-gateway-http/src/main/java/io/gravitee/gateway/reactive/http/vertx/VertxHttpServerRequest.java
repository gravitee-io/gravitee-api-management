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
package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.gateway.reactive.core.BufferFlow;
import io.gravitee.gateway.reactive.core.DefaultTlsSession;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.gateway.reactive.http.vertx.ws.VertxWebSocket;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.net.SocketAddress;
import javax.net.ssl.SSLSession;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest extends AbstractRequest {

    protected final HttpServerRequest nativeRequest;
    private Boolean isWebSocket = null;
    private Boolean isStreaming = null;
    private final VertxHttpServerRequestOptions options;

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator) {
        this(nativeRequest, idGenerator, new VertxHttpServerRequestOptions());
    }

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator, VertxHttpServerRequestOptions options) {
        this.nativeRequest = nativeRequest;
        this.originalHost = this.nativeRequest.host();
        this.timestamp = System.currentTimeMillis();
        this.id = idGenerator.randomString();
        this.headers = new VertxHttpHeaders(nativeRequest.headers().getDelegate());
        this.bufferFlow = new BufferFlow(nativeRequest.toFlowable().map(Buffer::buffer), this::isStreaming);
        this.messageFlow = null;
        this.options = options;
    }

    public VertxHttpServerResponse response() {
        return new VertxHttpServerResponse(this);
    }

    @Override
    public String uri() {
        if (uri == null) {
            uri = nativeRequest.uri();
        }

        return uri;
    }

    @Override
    public String path() {
        if (path == null) {
            path = nativeRequest.path();
        }

        return path;
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        if (parameters == null) {
            parameters = URIUtils.parameters(nativeRequest.uri());
        }

        return parameters;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        if (pathParameters == null) {
            pathParameters = new LinkedMultiValueMap<>();
        }

        return pathParameters;
    }

    @Override
    public HttpMethod method() {
        if (method == null) {
            try {
                method = HttpMethod.valueOf(nativeRequest.method().name());
            } catch (IllegalArgumentException iae) {
                method = HttpMethod.OTHER;
            }
        }

        return method;
    }

    @Override
    public String scheme() {
        if (scheme == null) {
            scheme = nativeRequest.scheme();
        }

        return scheme;
    }

    @Override
    public HttpVersion version() {
        if (version == null) {
            version = HttpVersion.valueOf(nativeRequest.version().name());
        }

        return version;
    }

    @Override
    public String remoteAddress() {
        if (remoteAddress == null) {
            SocketAddress nativeRemoteAddress = nativeRequest.remoteAddress();
            this.remoteAddress = extractAddress(nativeRemoteAddress);
        }
        return remoteAddress;
    }

    @Override
    public String localAddress() {
        if (localAddress == null) {
            this.localAddress = extractAddress(nativeRequest.localAddress());
        }
        return localAddress;
    }

    private String extractAddress(SocketAddress address) {
        if (address != null) {
            //TODO Could be improve to a better compatibility with geoIP
            int ipv6Idx = address.host().indexOf("%");
            return (ipv6Idx != -1) ? address.host().substring(0, ipv6Idx) : address.host();
        }
        return null;
    }

    @Override
    public SSLSession sslSession() {
        if (sslSession == null) {
            sslSession = nativeRequest.sslSession();
        }

        return sslSession;
    }

    @Override
    public TlsSession tlsSession() {
        if (tlsSession == null) {
            tlsSession = new DefaultTlsSession(nativeRequest.sslSession(), headers, options.clientAuthHeaderName());
        }
        return tlsSession;
    }

    @Override
    public boolean ended() {
        return nativeRequest.isEnded();
    }

    @Override
    public String host() {
        return this.nativeRequest.host();
    }

    /**
     * Pauses the current request.
     * <b>WARN: use with caution</b>
     */
    public void pause() {
        this.nativeRequest.pause();
    }

    /**
     * Resumes the current request.
     * <b>WARN: use with caution</b>
     */
    public void resume() {
        this.nativeRequest.resume();
    }

    public boolean isStreaming() {
        if (isStreaming == null) {
            isStreaming = RequestUtils.isStreaming(this);
        }
        return isStreaming;
    }

    @Override
    public boolean isWebSocket() {
        if (isWebSocket == null) {
            isWebSocket = RequestUtils.isWebSocket(nativeRequest);
        }
        return isWebSocket;
    }

    @Override
    public WebSocket webSocket() {
        if (isWebSocket() && webSocket == null) {
            webSocket = new VertxWebSocket(nativeRequest);
        }
        return webSocket;
    }

    /**
     * Indicates if the request is a websocket request and the connection has been upgraded (meaning, a websocket connection has been created).
     *
     * @return <code>true</code> if the connection has been upgraded to websocket, <code>false</code> else.
     * @see #webSocket()
     */
    public boolean isWebSocketUpgraded() {
        return webSocket != null && webSocket.upgraded();
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        super.messages(messages);

        // If message flow is set up, make sure any access to chunk buffers will not be possible anymore and returns empty.
        chunks(Flowable.empty());
    }

    public record VertxHttpServerRequestOptions(String clientAuthHeaderName) {
        public VertxHttpServerRequestOptions() {
            this(null);
        }
    }
}
