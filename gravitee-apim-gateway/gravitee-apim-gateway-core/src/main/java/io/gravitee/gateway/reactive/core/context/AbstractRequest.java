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
package io.gravitee.gateway.reactive.core.context;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.TlsSession;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.gateway.reactive.core.BufferFlow;
import io.gravitee.gateway.reactive.core.MessageFlow;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.MaybeTransformer;
import io.reactivex.rxjava3.core.Single;
import java.util.function.Function;
import javax.net.ssl.SSLSession;

/**
 * Base implementation of {@link Request} that does nothing in particular and <b>can be</b> used to avoid reimplementing all methods.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractRequest implements MutableRequest {

    protected BufferFlow bufferFlow;
    protected MessageFlow messageFlow;
    protected String id;
    protected String transactionId;
    protected String clientIdentifier;
    protected String uri;
    protected String host;
    protected String originalHost;
    protected String path;
    protected String pathInfo;
    protected String contextPath;
    protected MultiValueMap<String, String> parameters;
    protected MultiValueMap<String, String> pathParameters;
    protected HttpHeaders headers;
    protected HttpMethod method;
    protected String scheme;
    protected HttpVersion version;
    protected long timestamp;
    protected String remoteAddress;
    protected String localAddress;
    protected SSLSession sslSession;
    protected TlsSession tlsSession;
    protected boolean ended;
    protected WebSocket webSocket;

    @Override
    public String id() {
        return id;
    }

    @Override
    public String transactionId() {
        return transactionId;
    }

    @Override
    public String clientIdentifier() {
        return clientIdentifier;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public String originalHost() {
        return originalHost;
    }

    @Override
    public String path() {
        return path;
    }

    @Override
    public String pathInfo() {
        return pathInfo;
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        return parameters;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        return pathParameters;
    }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public HttpMethod method() {
        return method;
    }

    @Override
    public void method(HttpMethod method) {
        if (method == null) {
            throw new IllegalStateException("Http method should not be null");
        }
        this.method = method;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public HttpVersion version() {
        return version;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String remoteAddress() {
        return remoteAddress;
    }

    @Override
    public String localAddress() {
        return localAddress;
    }

    @Override
    public SSLSession sslSession() {
        return sslSession;
    }

    @Override
    public TlsSession tlsSession() {
        return tlsSession;
    }

    @Override
    public boolean ended() {
        return ended;
    }

    public boolean isStreaming() {
        return false;
    }

    @Override
    public boolean isWebSocket() {
        return webSocket != null;
    }

    @Override
    public WebSocket webSocket() {
        return webSocket;
    }

    @Override
    public MutableRequest contextPath(String contextPath) {
        if (this.contextPath != null) {
            this.path = this.path.replaceFirst(this.contextPath, contextPath);
        }
        this.contextPath = contextPath;
        if (contextPath == null || contextPath.isEmpty()) {
            this.pathInfo = path();
        } else {
            this.pathInfo = path().substring(contextPath.length() - 1);
        }
        return this;
    }

    @Override
    public MutableRequest pathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
        return this;
    }

    @Override
    public MutableRequest transactionId(String transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    @Override
    public MutableRequest clientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
        return this;
    }

    @Override
    public MutableRequest remoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    @Override
    public void setMessagesInterceptor(Function<FlowableTransformer<Message, Message>, FlowableTransformer<Message, Message>> interceptor) {
        lazyMessageFlow().setOnMessagesInterceptor(interceptor);
    }

    @Override
    public void unsetMessagesInterceptor() {
        lazyMessageFlow().unsetOnMessagesInterceptor();
    }

    @Override
    public Maybe<Buffer> body() {
        return lazyBufferFlow().body();
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        return lazyBufferFlow().bodyOrEmpty();
    }

    @Override
    public void body(final Buffer buffer) {
        lazyBufferFlow().body(buffer);
    }

    @Override
    public Completable onBody(final MaybeTransformer<Buffer, Buffer> onBody) {
        return lazyBufferFlow().onBody(onBody);
    }

    @Override
    public Flowable<Buffer> chunks() {
        return lazyBufferFlow().chunks();
    }

    @Override
    public void chunks(final Flowable<Buffer> chunks) {
        lazyBufferFlow().chunks(chunks);
    }

    @Override
    public Completable onChunks(final FlowableTransformer<Buffer, Buffer> onChunks) {
        return lazyBufferFlow().onChunks(onChunks);
    }

    @Override
    public void contentLength(long contentLength) {
        headers.set(io.vertx.core.http.HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
        headers.remove(io.vertx.core.http.HttpHeaders.TRANSFER_ENCODING);
    }

    @Override
    public void messages(final Flowable<Message> messages) {
        lazyMessageFlow().messages(messages);
    }

    @Override
    public Flowable<Message> messages() {
        return lazyMessageFlow().messages();
    }

    @Override
    public Completable onMessages(final FlowableTransformer<Message, Message> onMessages) {
        return Completable.fromRunnable(() -> lazyMessageFlow().onMessages(onMessages));
    }

    @Override
    public void pipeUpstream(Completable pipe) {
        // no op implementation to avoid breaking subtypes
    }

    protected final BufferFlow lazyBufferFlow() {
        if (bufferFlow == null) {
            bufferFlow = new BufferFlow(this::isStreaming);
        }

        return this.bufferFlow;
    }

    protected final MessageFlow lazyMessageFlow() {
        if (messageFlow == null) {
            messageFlow = new MessageFlow();
        }

        return this.messageFlow;
    }
}
