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
import static io.gravitee.gateway.reactive.http.vertx.client.VertxHttpClient.buildUrl;
import static io.gravitee.plugin.endpoint.http.proxy.client.UriHelper.URI_QUERY_DELIMITER_CHAR;
import static io.gravitee.plugin.endpoint.http.proxy.client.UriHelper.URI_QUERY_DELIMITER_CHAR_SEQUENCE;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerResponse;
import io.gravitee.gateway.reactive.http.vertx.client.VertxHttpClient;
import io.gravitee.plugin.endpoint.http.proxy.client.HttpClientFactory;
import io.gravitee.plugin.endpoint.http.proxy.client.UriHelper;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.http.HttpClientRequest;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
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
    private final String defaultHost;
    private final int defaultPort;
    private final boolean defaultSsl;
    private final MultiValueMap<String, String> targetParameters;
    protected final HttpProxyEndpointConnectorConfiguration configuration;
    protected final HttpClientFactory httpClientFactory;

    public HttpConnector(final HttpProxyEndpointConnectorConfiguration configuration, final HttpClientFactory httpClientFactory) {
        this.configuration = configuration;
        this.httpClientFactory = httpClientFactory;
        final URL targetUrl = buildUrl(configuration.getTarget());
        this.relativeTarget = targetUrl.getPath();
        this.defaultHost = targetUrl.getHost();
        this.defaultPort = targetUrl.getPort() != -1 ? targetUrl.getPort() : targetUrl.getDefaultPort();
        this.defaultSsl = VertxHttpClient.isSecureProtocol(targetUrl.getProtocol());
        if (targetUrl.getQuery() == null) {
            targetParameters = null;
        } else {
            targetParameters = URIUtils.parameters(URI_QUERY_DELIMITER_CHAR_SEQUENCE + targetUrl.getQuery());
        }
    }

    @Override
    public Completable connect(final ExecutionContext ctx) {
        return Completable.defer(() -> {
            final Request request = ctx.request();
            final Response response = ctx.response();

            final RequestOptions options = buildRequestOptions(ctx);

            ctx.metrics().setEndpoint(VertxHttpClient.toAbsoluteUri(options, defaultHost, defaultPort));

            return httpClientFactory
                .getOrBuildHttpClient(ctx, configuration)
                .rxRequest(options)
                .map(this::customizeHttpClientRequest)
                .flatMap(httpClientRequest ->
                    httpClientRequest.rxSend(
                        request.chunks().map(buffer -> io.vertx.rxjava3.core.buffer.Buffer.buffer(buffer.getNativeBuffer()))
                    )
                )
                .doOnSuccess(endpointResponse -> {
                    response.status(endpointResponse.statusCode());

                    copyHeaders(endpointResponse.headers(), response.headers());

                    if (endpointResponse.version() == HttpVersion.HTTP_2) {
                        endpointResponse.customFrameHandler(frame ->
                            ((VertxHttpServerResponse) response).getNativeResponse().writeCustomFrame(frame)
                        );
                    }
                    response.chunks(
                        endpointResponse
                            .toFlowable()
                            .map(Buffer::buffer)
                            .doOnComplete(() ->
                                // Write trailers when chunks are completed
                                copyHeaders(endpointResponse.trailers(), response.trailers())
                            )
                    );
                })
                .ignoreElement();
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

    protected RequestOptions buildRequestOptions(ExecutionContext ctx) {
        final RequestOptions requestOptions = new RequestOptions();
        final Request request = ctx.request();
        final io.gravitee.gateway.api.http.HttpHeaders requestHeaders = request.headers();
        final String originalHost = request.originalHost();
        final String currentRequestHost = request.host();

        // Remove HOP-by-HOP headers
        for (CharSequence header : hopHeaders()) {
            requestHeaders.remove(header.toString());
        }

        if (currentRequestHost != null && !Objects.equals(originalHost, currentRequestHost)) {
            // 'Host' header must be removed unless it has been set during the request flow (non-null and different from original request's host).
            requestHeaders.set(HOST, currentRequestHost);
        } else {
            requestHeaders.remove(HOST);
        }

        if (!configuration.getHttpOptions().isPropagateClientAcceptEncoding()) {
            // Let the API owner choose the Accept-Encoding between the gateway and the backend.
            requestHeaders.remove(HttpHeaders.ACCEPT_ENCODING);
        }

        prepareUriAndQueryParameters(ctx, requestOptions);

        // Override any request headers that are configured at endpoint level.
        final List<HttpHeader> configHeaders = configuration.getHeaders();
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
            .setTimeout(configuration.getHttpOptions().getReadTimeout())
            .setFollowRedirects(configuration.getHttpOptions().isFollowRedirects());
    }

    protected Set<CharSequence> hopHeaders() {
        return HOP_HEADERS;
    }

    private void prepareUriAndQueryParameters(final ExecutionContext ctx, final RequestOptions requestOptions) {
        final Request request = ctx.request();
        final MultiValueMap<String, String> requestParameters = request.parameters();
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
}
