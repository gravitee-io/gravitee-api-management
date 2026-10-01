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

import static io.netty.handler.codec.http.HttpHeaderValues.CHUNKED;
import static io.netty.handler.codec.http.HttpHeaderValues.CONTINUE;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.EXPECT;
import static io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.utils.RequestUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.gateway.reactive.core.BufferFlow;
import io.gravitee.gateway.reactive.core.HttpTlsSession;
import io.gravitee.gateway.reactive.core.context.AbstractRequest;
import io.gravitee.gateway.reactive.http.vertx.ws.VertxWebSocket;
import io.netty.util.AttributeKey;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.core.Vertx;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.core.net.HostAndPort;
import io.vertx.core.net.SocketAddress;
import io.vertx.rxjava3.core.http.HttpConnection;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.net.ssl.SSLSession;
import lombok.Builder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest extends AbstractRequest {

    public static final String NETTY_ATTR_CONNECTION_TIME = "connectionTime";
    static final long MAX_DISCARDED_BODY_BYTES = 1024L * 1024;
    static final long MAX_BODY_DISCARD_DELAY_MS = 5_000;
    private static final long UNKNOWN_LENGTH = -1;
    protected final HttpServerRequest nativeRequest;
    private Boolean isWebSocket = null;
    private Boolean isStreaming = null;
    private final VertxHttpServerRequestOptions options;
    private final long announcedBodyLength;
    private final boolean expectsContinue;
    private volatile boolean bodySubscribed;

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator) {
        this(nativeRequest, idGenerator, new VertxHttpServerRequestOptions());
    }

    public VertxHttpServerRequest(final HttpServerRequest nativeRequest, IdGenerator idGenerator, VertxHttpServerRequestOptions options) {
        this.nativeRequest = nativeRequest;
        this.originalHost = hostWithPort(this.nativeRequest.authority());
        this.timestamp = options.timestamp() != null ? options.timestamp() : System.currentTimeMillis();
        if (options.timestampNs() != null) {
            this.timestampNs = options.timestampNs();
        }
        this.path = options.path();
        this.id = idGenerator.randomString();
        this.headers = new VertxHttpHeaders(nativeRequest.headers());
        // Captured on arrival: policies may rewrite these headers for the backend.
        this.announcedBodyLength = announcedBodyLength(headers);
        this.expectsContinue = CONTINUE.contentEqualsIgnoreCase(headers.get(EXPECT));
        this.bufferFlow = new BufferFlow(
            nativeRequest
                .toFlowable()
                .doOnSubscribe(subscription -> bodySubscribed = true)
                .map(Buffer::buffer),
            this::isStreaming
        );
        this.messageFlow = null;
        this.options = options;
        // In Vert.x 5, channel() was removed; use channelHandlerContext().channel() instead
        this.connectionTimestamp = (Long) ((HttpServerConnection) nativeRequest.connection().getDelegate()).channelHandlerContext()
            .channel()
            .attr(AttributeKey.valueOf(NETTY_ATTR_CONNECTION_TIME))
            .get();
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
            // TODO Could be improve to a better compatibility with geoIP
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
            tlsSession = new HttpTlsSession(nativeRequest.sslSession(), headers, options.clientAuthHeaderName());
        }
        return tlsSession;
    }

    @Override
    public boolean ended() {
        return nativeRequest.isEnded();
    }

    /**
     * What must be done, once the response has been sent, with a request body nobody has consumed.
     * <p>
     * On HTTP/1.x, Vert.x only handles the next request of a connection once the current one has been fully read,
     * which never happens to a body left paused: it must be discarded. When it cannot be discarded within
     * {@link #MAX_DISCARDED_BODY_BYTES} — length unknown or too large — or when the client waits for a
     * {@code 100 Continue} it will never get, the connection cannot be reused and must be closed.
     */
    UnconsumedBody unconsumedBody() {
        final HttpVersion version = version();
        final boolean http1 = version == HttpVersion.HTTP_1_1 || version == HttpVersion.HTTP_1_0;
        if (!http1 || !hasUnconsumedBody()) {
            return UnconsumedBody.NONE;
        }
        if (expectsContinue || announcedBodyLength == UNKNOWN_LENGTH || announcedBodyLength > MAX_DISCARDED_BODY_BYTES) {
            return UnconsumedBody.DISCARD_THEN_CLOSE;
        }
        return UnconsumedBody.DISCARD;
    }

    /**
     * Reads and throws away the request body nobody consumed, then closes the connection if asked to.
     * <p>
     * The connection is closed anyway, without waiting for the end of the body, after
     * {@link #MAX_DISCARDED_BODY_BYTES} or {@link #MAX_BODY_DISCARD_DELAY_MS}. Discarding before closing matters:
     * closing a socket whose receive buffer holds data makes the kernel answer with a TCP reset, which can destroy the
     * response before the client reads it.
     */
    void discardUnconsumedBody(boolean closeConnectionWhenDone) {
        final HttpConnection connection = nativeRequest.connection();
        if (!hasUnconsumedBody()) {
            if (closeConnectionWhenDone) {
                connection.close();
            }
            return;
        }

        final Vertx vertx = Vertx.currentContext().owner();
        final AtomicBoolean done = new AtomicBoolean();
        final AtomicLong timerId = new AtomicLong();
        final AtomicLong discardedBytes = new AtomicLong();
        final Consumer<Boolean> finish = closeConnection -> {
            if (done.compareAndSet(false, true)) {
                vertx.cancelTimer(timerId.get());
                if (Boolean.TRUE.equals(closeConnection)) {
                    connection.close();
                }
            }
        };
        timerId.set(vertx.setTimer(MAX_BODY_DISCARD_DELAY_MS, id -> finish.accept(true)));

        nativeRequest.exceptionHandler(throwable -> finish.accept(true));
        nativeRequest.handler(buffer -> {
            if (discardedBytes.addAndGet(buffer.length()) > MAX_DISCARDED_BODY_BYTES) {
                finish.accept(true);
            }
        });
        nativeRequest.endHandler(v -> finish.accept(closeConnectionWhenDone));
        nativeRequest.resume();
    }

    /**
     * Once subscribed, the body belongs to its consumer, which reads it to the end or releases it to Vert.x.
     * A paused request is not flagged as ended although there may be nothing left to read: the announced length
     * tells whether there is a body at all.
     */
    private boolean hasUnconsumedBody() {
        return announcedBodyLength != 0 && !bodySubscribed && !nativeRequest.isEnded();
    }

    private static long announcedBodyLength(HttpHeaders headers) {
        final String contentLength = headers.get(CONTENT_LENGTH);
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength.trim());
            } catch (NumberFormatException e) {
                return UNKNOWN_LENGTH;
            }
        }
        final boolean chunked = headers
            .getAll(TRANSFER_ENCODING)
            .stream()
            .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(CHUNKED));
        return chunked ? UNKNOWN_LENGTH : 0;
    }

    enum UnconsumedBody {
        NONE,
        DISCARD,
        DISCARD_THEN_CLOSE,
    }

    @Override
    public String host() {
        return hostWithPort(this.nativeRequest.authority());
    }

    /**
     * Reconstructs the host with its port from a {@link HostAndPort}, replicating the behavior of Vert.x 4's
     * {@code HttpServerRequest.host()} which returned the raw {@code Host} header value (e.g. {@code localhost:8082}).
     * In Vert.x 5, {@code authority().host()} returns only the hostname without the port, which would cause the port
     * to be silently dropped in URLs built from {@code originalHost()} (e.g. in {@code XForwardProcessor} when
     * computing {@code ContextAttributes.ATTR_REQUEST_ORIGINAL_URL}).
     * <p>
     * The authority is {@code null} when the request carries neither a {@code Host} header nor an HTTP/2
     * {@code :authority} pseudo-header. Vert.x 4 returned {@code null} in that case as well.
     */
    private static String hostWithPort(HostAndPort authority) {
        if (authority == null) {
            return null;
        }
        int port = authority.port();
        return port > 0 ? authority.host() + ":" + port : authority.host();
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

    /**
     * What the dispatcher knows about a request before this wrapper exists.
     *
     * <p>Build it rather than calling the canonical constructor: a record's canonical constructor is
     * positional, so adding a component to it moves a signature — the very thing this type exists to
     * avoid. Through the builder a new field costs no call site anything.
     *
     * @param clientAuthHeaderName header the client certificate is extracted from, when a front
     *     proxy terminates TLS.
     * @param path the path the request reports, so that everything derived from it — starting with
     *     the {@code pathInfo} a contextualized request computes — matches the path the gateway
     *     resolved rather than the one received. {@code null} reports the native path, which is the
     *     historical behaviour. {@code uri()} reports the untouched native value either way.
     * @param timestamp when the gateway started handling this request, in milliseconds since the
     *     epoch, taken before any work is done on it. {@code null} stamps the clock at construction,
     *     which excludes everything the dispatcher did beforehand from every latency it reports.
     * @param timestampNs the same instant on the monotonic clock, and the origin every reported
     *     duration is measured from. Travels with {@code timestamp} rather than being derived from
     *     it: the two clocks share no origin. {@code null} keeps the construction-time default,
     *     with the same blind spot as above.
     */
    @Builder
    public record VertxHttpServerRequestOptions(String clientAuthHeaderName, String path, Long timestamp, Long timestampNs) {
        public VertxHttpServerRequestOptions() {
            this(null, null, null, null);
        }

        public VertxHttpServerRequestOptions(String clientAuthHeaderName) {
            this(clientAuthHeaderName, null, null, null);
        }
    }
}
