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
package io.gravitee.gateway.reactive.http.vertx;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.reporter.api.http.Metrics;
import io.reactivex.*;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.net.SocketAddress;
import javax.net.ssl.SSLSession;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerRequest implements Request {

    protected final String id;
    protected final long timestamp;
    protected final Metrics metrics;
    protected final String contextPath;
    protected final String pathInfo;
    protected final HttpServerRequest nativeRequest;
    protected MultiValueMap<String, String> queryParameters = null;
    protected MultiValueMap<String, String> pathParameters = null;
    protected HttpHeaders headers;
    protected Flowable<Buffer> chunks;

    public VertxHttpServerRequest(HttpServerRequest nativeRequest, String contextPath, IdGenerator idGenerator) {
        this.nativeRequest = nativeRequest;
        this.timestamp = System.currentTimeMillis();
        this.id = idGenerator.randomString();
        this.headers = new VertxHttpHeaders(nativeRequest.headers().getDelegate());
        this.metrics = Metrics.on(timestamp).build();
        this.metrics.setRequestId(id());
        this.metrics.setHttpMethod(method());
        this.metrics.setLocalAddress(localAddress());
        this.metrics.setRemoteAddress(remoteAddress());
        this.metrics.setHost(nativeRequest.host());
        this.metrics.setUri(uri());
        this.metrics.setUserAgent(nativeRequest.getHeader(io.vertx.reactivex.core.http.HttpHeaders.USER_AGENT));
        this.contextPath = contextPath;
        this.pathInfo = path().substring((contextPath.length() == 1) ? 0 : contextPath.length() - 1);
        // Make sure that any subscription to the request body will be cached to avoid multiple consumptions.
        this.chunks =
            nativeRequest
                .toFlowable()
                .doOnNext(buffer -> metrics.setRequestContentLength(metrics.getRequestContentLength() + buffer.length()))
                .map(Buffer::buffer)
                .cache();
    }

    public VertxHttpServerResponse response() {
        return new VertxHttpServerResponse(this);
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
        return nativeRequest.uri();
    }

    @Override
    public String path() {
        return nativeRequest.path();
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
        if (queryParameters == null) {
            queryParameters = URIUtils.parameters(nativeRequest.uri());
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
        return headers;
    }

    @Override
    public HttpMethod method() {
        try {
            return HttpMethod.valueOf(nativeRequest.method().name());
        } catch (IllegalArgumentException iae) {
            return HttpMethod.OTHER;
        }
    }

    @Override
    public String scheme() {
        return nativeRequest.scheme();
    }

    @Override
    public HttpVersion version() {
        return HttpVersion.valueOf(nativeRequest.version().name());
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String remoteAddress() {
        SocketAddress address = nativeRequest.remoteAddress();
        if (address == null) {
            return null;
        }

        //TODO: To be removed
        int ipv6Idx = address.host().indexOf("%");

        return (ipv6Idx != -1) ? address.host().substring(0, ipv6Idx) : address.host();
    }

    @Override
    public String localAddress() {
        SocketAddress address = nativeRequest.localAddress();
        if (address == null) {
            return null;
        }

        //TODO: To be removed
        int ipv6Idx = address.host().indexOf("%");

        return (ipv6Idx != -1) ? address.host().substring(0, ipv6Idx) : address.host();
    }

    @Override
    public SSLSession sslSession() {
        return nativeRequest.sslSession();
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }

    @Override
    public boolean ended() {
        return nativeRequest.isEnded();
    }

    @Override
    public String host() {
        return this.nativeRequest.host();
    }

    @Override
    public Maybe<Buffer> body() {
        // Reduce all the chunks to create a unique buffer containing all the content.
        final Maybe<Buffer> buffer = chunks().reduce(Buffer::appendBuffer);
        this.chunks = buffer.toFlowable();
        return buffer;
    }

    @Override
    public Single<Buffer> bodyOrEmpty() {
        // Reduce all the chunks to create a unique buffer containing all the content.
        return body().switchIfEmpty(Single.just(Buffer.buffer()));
    }

    @Override
    public Flowable<Buffer> chunks() {
        return chunks;
    }

    @Override
    public Completable onChunk(FlowableTransformer<Buffer, Buffer> chunkTransformer) {
        chunks = chunks.compose(chunkTransformer);

        return Completable.complete();
    }

    @Override
    public Completable onBody(MaybeTransformer<Buffer, Buffer> bodyTransformer) {
        return body(body().compose(bodyTransformer));
    }

    @Override
    public Completable body(Maybe<Buffer> buffer) {
        return setChunks(buffer.toFlowable());
    }

    @Override
    public Completable body(Buffer buffer) {
        return setChunks(Flowable.just(buffer));
    }

    @Override
    public Completable chunks(final Flowable<Buffer> chunks) {
        return setChunks(chunks);
    }

    private Completable setChunks(Flowable<Buffer> chunks) {
        return onChunk(upstream -> chunks);
    }
}
