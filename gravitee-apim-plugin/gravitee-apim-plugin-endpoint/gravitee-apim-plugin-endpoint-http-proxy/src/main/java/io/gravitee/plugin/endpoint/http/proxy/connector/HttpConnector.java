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
package io.gravitee.plugin.endpoint.http.proxy.connector;

import static io.gravitee.gateway.api.http.HttpHeaderNames.CONNECTION;
import static io.gravitee.gateway.api.http.HttpHeaderNames.HOST;
import static io.gravitee.gateway.api.http.HttpHeaderNames.KEEP_ALIVE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_AUTHENTICATE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static io.gravitee.gateway.api.http.HttpHeaderNames.PROXY_CONNECTION;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TE;
import static io.gravitee.gateway.api.http.HttpHeaderNames.TRAILER;
import static io.gravitee.gateway.api.http.HttpHeaderNames.UPGRADE;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT;
import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_REQUEST_ENDPOINT_OVERRIDE;
import static io.gravitee.plugin.endpoint.http.proxy.client.UriHelper.URI_QUERY_DELIMITER_CHAR;
import static io.gravitee.plugin.endpoint.http.proxy.client.UriHelper.URI_QUERY_DELIMITER_CHAR_SEQUENCE;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpRequest;
import io.gravitee.gateway.reactive.api.context.http.HttpResponse;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerResponse;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientRequest;
import io.gravitee.node.api.opentelemetry.http.ObservableHttpClientResponse;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.UriHelper;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.StreamResetException;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import io.vertx.rxjava3.core.http.HttpClientResponse;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpConnector implements ProxyConnector {

    /**
     * Headers that are not allowed to be propagated to the endpoint.
     */
    static final Set<CharSequence> HOP_HEADERS = Set.of(
        CONNECTION,
        KEEP_ALIVE,
        PROXY_AUTHORIZATION,
        PROXY_AUTHENTICATE,
        PROXY_CONNECTION,
        TE,
        TRAILER,
        UPGRADE
    );
    private final String relativeTarget;
    protected final String defaultHost;
    protected final int defaultPort;
    private final boolean defaultSsl;
    private final MultiValueMap<String, String> targetParameters;
    protected final HttpProxyEndpointConnectorConfiguration configuration;
    protected final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration;
    protected final HttpClientFactory httpClientFactory;

    public HttpConnector(
        final HttpProxyEndpointConnectorConfiguration configuration,
        final HttpProxyEndpointConnectorSharedConfiguration sharedConfiguration,
        final HttpClientFactory httpClientFactory
    ) {
        this.configuration = configuration;
        this.sharedConfiguration = sharedConfiguration;
        this.httpClientFactory = httpClientFactory;
        final URL targetUrl = VertxHttpClientFactory.buildUrl(configuration.getTarget());
        this.relativeTarget = targetUrl.getPath();
        this.defaultHost = targetUrl.getHost();
        this.defaultPort = targetUrl.getPort() != -1 ? targetUrl.getPort() : targetUrl.getDefaultPort();
        this.defaultSsl = VertxHttpClientFactory.isSecureProtocol(targetUrl.getProtocol());
        if (targetUrl.getQuery() == null) {
            targetParameters = null;
        } else {
            targetParameters = URIUtils.parameters(URI_QUERY_DELIMITER_CHAR_SEQUENCE + targetUrl.getQuery());
        }
    }

    @Override
    public Completable connect(final HttpExecutionContext ctx) {
        try {
            final HttpRequest request = ctx.request();
            final HttpResponse response = ctx.response();

            final RequestOptions options = buildRequestOptions(ctx);
            String absoluteUri = VertxHttpClientFactory.toAbsoluteUri(options, defaultHost, defaultPort);
            options.setAbsoluteURI(absoluteUri);

            // setAbsoluteURI sets options.host from the endpoint target URL, which Vert.x uses as the
            // HTTP Host header — overriding any custom Host header configured on the endpoint or HC.
            // If a custom Host was explicitly set, pin the TCP connection to the actual backend via
            // setServer() so the custom value only affects the HTTP Host header, not the connection address.
            final String customHost = options.getHeaders() != null ? options.getHeaders().get(io.vertx.core.http.HttpHeaders.HOST) : null;
            if (customHost != null && !customHost.isBlank()) {
                options.setServer(io.vertx.core.net.SocketAddress.inetSocketAddress(options.getPort(), defaultHost));
                options.setHost(customHost);
            }

            ctx.metrics().setEndpoint(absoluteUri);
            ObservableHttpClientRequest observableHttpClientRequest = new ObservableHttpClientRequest(options);
            Span httpRequestSpan = ctx.getTracer().startSpanFrom(observableHttpClientRequest);
            // Tracks the upstream request between pool acquisition and the response head so that disposing the chain
            // (typically a client abort) releases the pooled connection instead of holding it until the request
            // timeout fires. From the response head onward, cancellation is handled by the response chunks'
            // doOnCancel (see getEndpointResponseChunks).
            final AtomicReference<HttpClientRequest> pendingUpstreamRequest = new AtomicReference<>();
            return acquireUpstreamRequest(ctx, options, pendingUpstreamRequest, absoluteUri)
                .map(this::customizeHttpClientRequest)
                .flatMap(httpClientRequest -> {
                    observableHttpClientRequest.httpClientRequest(httpClientRequest.getDelegate());
                    return sendEndpointRequestChunks(httpClientRequest, request);
                })
                .doOnSuccess(endpointResponse -> {
                    // The response chunks' doOnCancel owns cancellation from here; reset() being a no-op on a
                    // completed stream makes the race with a concurrent disposal benign.
                    pendingUpstreamRequest.set(null);

                    response.status(endpointResponse.statusCode());

                    copyHeaders(endpointResponse.headers(), response.headers());

                    if (endpointResponse.version() == HttpVersion.HTTP_2) {
                        endpointResponse.customFrameHandler(frame ->
                            ((VertxHttpServerResponse) response).getNativeResponse().writeCustomFrame(frame)
                        );
                    }

                    // Assign the response chunks from the endpoint's response to the gateway response.
                    response.chunks(getEndpointResponseChunks(endpointResponse, response, absoluteUri));

                    ObservableHttpClientResponse observableHttpClientResponse = new ObservableHttpClientResponse(
                        endpointResponse.getDelegate()
                    );
                    ctx.getTracer().endWithResponse(httpRequestSpan, observableHttpClientResponse);
                })
                .doOnError(throwable -> ctx.getTracer().endOnError(httpRequestSpan, throwable))
                .ignoreElement()
                .doOnDispose(() -> resetPendingUpstreamRequest(ctx, pendingUpstreamRequest, absoluteUri));
        } catch (Exception e) {
            return Completable.error(e);
        }
    }

    /**
     * Acquires an upstream request from the client's connection pool, releasing it immediately when the chain has
     * already been disposed (client abort while the request was queued in the pool). The rx bridge cannot do this by
     * itself: disposing {@code rxRequest}'s Single only drops the observer, so a connection granted afterwards would
     * silently hold its pool slot until the request timeout fires — under sustained abort/retry load this keeps the
     * pool collapsed long after the backend has recovered.
     */
    private Single<HttpClientRequest> acquireUpstreamRequest(
        final HttpExecutionContext ctx,
        final RequestOptions options,
        final AtomicReference<HttpClientRequest> pendingUpstreamRequest,
        final String absoluteUri
    ) {
        return Single.create(emitter ->
            httpClientFactory
                .getOrBuildHttpClient(ctx, configuration, sharedConfiguration)
                .getDelegate()
                .request(options)
                .onComplete(asyncRequest -> {
                    if (asyncRequest.succeeded()) {
                        final HttpClientRequest upstreamRequest = HttpClientRequest.newInstance(asyncRequest.result());
                        // Publish the request before checking for disposal so a concurrent disposal either sees it
                        // (doOnDispose resets it) or is detected right below; getAndSet(null) makes the double reset
                        // a no-op.
                        pendingUpstreamRequest.set(upstreamRequest);
                        if (emitter.isDisposed()) {
                            resetPendingUpstreamRequest(ctx, pendingUpstreamRequest, absoluteUri);
                        } else {
                            emitter.onSuccess(upstreamRequest);
                        }
                    } else {
                        emitter.tryOnError(asyncRequest.cause());
                    }
                })
        );
    }

    private void resetPendingUpstreamRequest(
        final HttpExecutionContext ctx,
        final AtomicReference<HttpClientRequest> pendingUpstreamRequest,
        final String absoluteUri
    ) {
        final HttpClientRequest upstreamRequest = pendingUpstreamRequest.getAndSet(null);
        if (upstreamRequest != null) {
            try {
                log.debug("Downstream request has been disposed, releasing upstream request to [{}]", absoluteUri);
                // Reset closes the upstream connection/stream so its pool slot is released right away instead of
                // being held until the request timeout; no-op if the request already completed.
                upstreamRequest.reset();
            } catch (Exception e) {
                log.debug("Can't properly reset endpoint request to backend [{}]", absoluteUri, e);
            }
        }
    }

    private Single<HttpClientResponse> sendEndpointRequestChunks(HttpClientRequest httpClientRequest, HttpRequest request) {
        if (hasContentLength(request) && isChunked(request)) {
            // A request-phase policy (e.g. Assign Content) can rewrite the body, set Content-Length, and
            // leave the client's Transfer-Encoding: chunked in place. Forwarding both violates RFC 9112
            // §6.1 and strict backends (NGINX, Jetty) return 400. The body is already de-chunked, so send
            // it fixed-length: drop Transfer-Encoding and set Content-Length to its actual length. Only
            // chunked is dropped — a content-coding like gzip must stay, or the backend can't decode the body.
            httpClientRequest.headers().remove(HttpHeaders.TRANSFER_ENCODING);
            return request
                .bodyOrEmpty()
                .flatMap(body -> {
                    httpClientRequest.headers().set(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.length()));
                    return httpClientRequest.rxSend(new io.vertx.rxjava3.core.buffer.Buffer(BufferImpl.buffer(body.getNativeBuffer())));
                });
        }
        if (hasBodyHeaders(request) || request.version() == io.gravitee.common.http.HttpVersion.HTTP_2) {
            // For HTTP1.1, the presence of a message body in a request is signaled by a Content-Length or Transfer-Encoding header (https://www.rfc-editor.org/rfc/rfc9112#section-6-4).
            // For HTTP2, Data frames are used (https://www.rfc-editor.org/rfc/rfc9113#section-8.1-7).
            return httpClientRequest.rxSend(
                request.chunks().map(buffer -> new io.vertx.rxjava3.core.buffer.Buffer(BufferImpl.buffer(buffer.getNativeBuffer())))
            );
        } else {
            // Always consume the request body, even when empty, to ensure resources are released and metrics are updated correctly.
            return request.chunks().ignoreElements().andThen(httpClientRequest.rxSend());
        }
    }

    private @NonNull Flowable<Buffer> getEndpointResponseChunks(
        HttpClientResponse endpointResponse,
        HttpResponse response,
        String absoluteUri
    ) {
        return endpointResponse
            .toFlowable()
            .map(Buffer::buffer)
            .doOnComplete(() ->
                // Write trailers when chunks are completed
                copyHeaders(endpointResponse.trailers(), response.trailers())
            )
            .onErrorResumeNext(throwable -> {
                if (throwable instanceof StreamResetException) {
                    // Means that we have manually reset the stream because the downstream request has been cancelled (see doOnCancel).
                    log.debug("Stream reset to the backend [{}]", absoluteUri);
                } else {
                    log.error("Exception occurred while handling response chunk from upstream [{}]", absoluteUri, throwable);
                }
                return Flowable.empty();
            })
            .doOnCancel(() -> {
                try {
                    log.debug("Downstream request has been cancelled, cancelling upstream request to [{}]", absoluteUri);

                    // Reset forces the upstream connection to be closed and avoid consuming the response while downstream is already gone.
                    endpointResponse.request().reset();
                } catch (Exception e) {
                    log.debug("Can't properly reset endpoint request to backend [{}]", absoluteUri, e);
                }
            });
    }

    protected HttpClientRequest customizeHttpClientRequest(final HttpClientRequest httpClientRequest) {
        return httpClientRequest;
    }

    protected void copyHeaders(io.vertx.rxjava3.core.MultiMap sourceHeaders, io.gravitee.gateway.api.http.HttpHeaders targetHeaders) {
        if (sourceHeaders != null && !sourceHeaders.isEmpty()) {
            if (targetHeaders instanceof VertxHttpHeaders) {
                // Optimize header copy by relying on delegate.
                ((VertxHttpHeaders) targetHeaders).getDelegate().addAll(sourceHeaders.getDelegate());
            } else {
                // Use regular copy by iterating on headers.
                sourceHeaders.forEach(entry -> targetHeaders.add(entry.getKey(), entry.getValue()));
            }
        }
    }

    protected RequestOptions buildRequestOptions(HttpExecutionContext ctx) {
        final RequestOptions requestOptions = new RequestOptions();
        final HttpRequest request = ctx.request();
        final io.gravitee.gateway.api.http.HttpHeaders requestHeaders = request.headers();
        final String originalHost = request.originalHost();
        final String currentRequestHost = request.host();

        // Remove HOP-by-HOP headers
        for (CharSequence header : hopHeaders()) {
            requestHeaders.remove(header.toString());
        }

        if (currentRequestHost != null) {
            if (sharedConfiguration.getHttpOptions().isPropagateClientHost() || !Objects.equals(originalHost, currentRequestHost)) {
                // 'Host' header must be removed unless it has been set during the request flow (non-null and different from original request's host).
                // If PropagateClientHost is enabled, we set the 'Host' header to the current request's host.
                requestHeaders.set(HOST, currentRequestHost);
            } else {
                requestHeaders.remove(HOST);
            }
        }
        // else: currentRequestHost is null (e.g. health check context); preserve any Host already in requestHeaders from configuration.

        if (!sharedConfiguration.getHttpOptions().isPropagateClientAcceptEncoding()) {
            // Let the API owner choose the Accept-Encoding between the gateway and the backend.
            requestHeaders.remove(HttpHeaders.ACCEPT_ENCODING);
        }

        prepareUriAndQueryParameters(ctx, requestOptions);

        // Override any request headers that are configured at endpoint level.
        final List<HttpHeader> configHeaders = sharedConfiguration.getHeaders();
        if (configHeaders != null && !configHeaders.isEmpty()) {
            configHeaders.forEach(header -> requestHeaders.set(header.getName(), header.getValue()));
        }

        if (requestHeaders instanceof VertxHttpHeaders) {
            // Avoid copying headers by reusing the original ones.
            requestOptions.setHeaders(((VertxHttpHeaders) requestHeaders).getDelegate());
        } else {
            final MultiMap headers = new HeadersMultiMap();
            requestHeaders.names().forEach(name -> headers.add(name, requestHeaders.getAll(name)));
            requestOptions.setHeaders(headers);
        }

        return requestOptions
            .setMethod(HttpMethod.valueOf(request.method().name()))
            .setTimeout(sharedConfiguration.getHttpOptions().getReadTimeout())
            .setFollowRedirects(sharedConfiguration.getHttpOptions().isFollowRedirects());
    }

    protected Set<CharSequence> hopHeaders() {
        return HOP_HEADERS;
    }

    private void prepareUriAndQueryParameters(final HttpExecutionContext ctx, final RequestOptions requestOptions) {
        final HttpRequest request = ctx.request();
        final MultiValueMap<String, String> requestParameters = new LinkedMultiValueMap<>();
        request.parameters().forEach((k, v) -> v.forEach(value -> requestParameters.add(k, value)));
        addParameters(requestParameters, targetParameters);

        String customEndpointTarget = ctx.getAttribute(ATTR_REQUEST_ENDPOINT);
        String uri = relativeTarget;
        boolean isRelative = true;

        if (customEndpointTarget != null) {
            final boolean endpointOverride = Boolean.TRUE.equals(ctx.getAttribute(ATTR_REQUEST_ENDPOINT_OVERRIDE));
            final MultiValueMap<String, String> customEndpointParameters = URIUtils.parameters(customEndpointTarget);

            if (!customEndpointParameters.isEmpty()) {
                customEndpointTarget = customEndpointTarget.substring(0, customEndpointTarget.indexOf(URI_QUERY_DELIMITER_CHAR));
                addParameters(requestParameters, customEndpointParameters);
            }

            if (URIUtils.isAbsolute(customEndpointTarget)) {
                uri = customEndpointTarget;
                isRelative = false;
            } else {
                uri = endpointOverride ? customEndpointTarget : relativeTarget + customEndpointTarget;
            }
        } else {
            // Just append the current request path.
            uri = uri + request.pathInfo();
        }

        if (isRelative) {
            UriHelper.configureRelativeUri(requestOptions, uri, requestParameters);
            requestOptions.setSsl(defaultSsl);
        } else {
            UriHelper.configureAbsoluteUri(requestOptions, uri, requestParameters);
        }
    }

    private void addParameters(MultiValueMap<String, String> parameters, MultiValueMap<String, String> parametersToAdd) {
        if (parametersToAdd != null && !parametersToAdd.isEmpty()) {
            parametersToAdd.forEach((key, values) -> parameters.computeIfAbsent(key, k -> new ArrayList<>()).addAll(values));
        }
    }

    private static boolean hasBodyHeaders(HttpRequest request) {
        return hasTransferEncoding(request) || hasContentLength(request);
    }

    private static boolean hasContentLength(HttpRequest request) {
        return request.headers().get(HttpHeaderNames.CONTENT_LENGTH) != null;
    }

    private static boolean hasTransferEncoding(HttpRequest request) {
        return request.headers().get(HttpHeaderNames.TRANSFER_ENCODING) != null;
    }

    private static boolean isChunked(HttpRequest request) {
        final String transferEncoding = request.headers().get(HttpHeaderNames.TRANSFER_ENCODING);
        return transferEncoding != null && HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding.trim());
    }
}
