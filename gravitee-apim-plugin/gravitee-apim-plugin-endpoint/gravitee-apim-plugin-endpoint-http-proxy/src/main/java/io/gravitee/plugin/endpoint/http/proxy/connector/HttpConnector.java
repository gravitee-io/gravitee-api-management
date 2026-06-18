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
import io.gravitee.plugin.endpoint.http.proxy.failure.ConnectionFailureClassifier;
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
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class HttpConnector implements ProxyConnector {

    /**
     * Internal-context flag set once a usable upstream connection/stream has been acquired for the current request.
     * Read by the failure classifier to tell a connection (acquisition) timeout apart from a backend read timeout
     * without parsing the timeout's message. Absent/false means the request never reached the backend.
     */
    public static final String ATTR_INTERNAL_UPSTREAM_CONNECTION_ACQUIRED = "http-proxy.upstream.connection.acquired";

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
                options.setServer(io.vertx.core.net.SocketAddress.inetSocketAddress(options.getPort(), options.getHost()));
                options.setHost(customHost);
            }

            ctx.metrics().setEndpoint(absoluteUri);
            ObservableHttpClientRequest observableHttpClientRequest = new ObservableHttpClientRequest(options);
            Span httpRequestSpan = ctx.getTracer().startSpanFrom(observableHttpClientRequest);
            return httpClientFactory
                .getOrBuildHttpClient(ctx, configuration, sharedConfiguration)
                .rxRequest(options)
                // rxRequest resolves once a connection/stream is acquired from the pool: from here on, a request-level
                // timeout is a backend read timeout, not a connection-acquisition timeout (APIM-12769).
                .doOnSuccess(acquiredRequest -> ctx.setInternalAttribute(ATTR_INTERNAL_UPSTREAM_CONNECTION_ACQUIRED, Boolean.TRUE))
                .map(this::customizeHttpClientRequest)
                .flatMap(httpClientRequest -> {
                    observableHttpClientRequest.httpClientRequest(httpClientRequest.getDelegate());
                    return sendEndpointRequestChunks(httpClientRequest, request);
                })
                .doOnSuccess(endpointResponse -> {
                    response.status(endpointResponse.statusCode());

                    copyHeaders(endpointResponse.headers(), response.headers());

                    if (endpointResponse.version() == HttpVersion.HTTP_2) {
                        endpointResponse.customFrameHandler(frame ->
                            ((VertxHttpServerResponse) response).getNativeResponse().writeCustomFrame(frame)
                        );
                    }

                    // Assign the response chunks from the endpoint's response to the gateway response.
                    response.chunks(getEndpointResponseChunks(ctx, endpointResponse, response, absoluteUri));

                    ObservableHttpClientResponse observableHttpClientResponse = new ObservableHttpClientResponse(
                        endpointResponse.getDelegate()
                    );
                    ctx.getTracer().endWithResponse(httpRequestSpan, observableHttpClientResponse);
                })
                .doOnError(throwable -> ctx.getTracer().endOnError(httpRequestSpan, throwable))
                .ignoreElement();
        } catch (Exception e) {
            return Completable.error(e);
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
        HttpExecutionContext ctx,
        HttpClientResponse endpointResponse,
        HttpResponse response,
        String absoluteUri
    ) {
        // A backend failure while streaming the response body (after status/headers were committed) is recorded on
        // the metrics below so the truncated response is observable instead of reported as a success.
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
                    ctx.withLogger(log).debug("Stream reset to the backend [{}]", absoluteUri);
                } else {
                    ctx
                        .withLogger(log)
                        .error("Exception occurred while handling response chunk from upstream [{}]", absoluteUri, throwable);
                    // The response status/headers are already committed to the client, so we cannot change them; record
                    // the backend failure on the metrics so the (otherwise silent) truncated response is observable.
                    recordBackendResponseStreamFailure(ctx, throwable);
                }
                return Flowable.empty();
            })
            .doOnCancel(() -> {
                try {
                    ctx.withLogger(log).debug("Downstream request has been cancelled, cancelling upstream request to [{}]", absoluteUri);

                    // Reset forces the upstream connection to be closed and avoid consuming the response while downstream is already gone.
                    endpointResponse.request().reset();
                } catch (Exception e) {
                    ctx.withLogger(log).debug("Can't properly reset endpoint request to backend [{}]", absoluteUri, e);
                }
            });
    }

    /**
     * Record a backend failure that happened while streaming the response body, i.e. after the response status and
     * headers were already committed to the client. The committed response is left untouched; only the metrics are
     * enriched (and later translated to a diagnostic) so the truncated response is no longer silently reported as a
     * success. An already-recorded error (e.g. a client abort) is preserved.
     */
    void recordBackendResponseStreamFailure(final HttpExecutionContext ctx, final Throwable throwable) {
        final var metrics = ctx.metrics();
        // No metrics yet, or an earlier error (e.g. a client abort) already recorded — preserve it.
        if (metrics == null || metrics.getErrorKey() != null) {
            return;
        }
        ConnectionFailureClassifier.Classification classification = ConnectionFailureClassifier.classify(throwable);
        metrics.setErrorKey(classification.key());
        metrics.setErrorMessage(
            throwable.getMessage() != null ? throwable.getMessage() : "Connection error while streaming the backend response"
        );
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
