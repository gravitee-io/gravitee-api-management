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
package io.gravitee.gateway.http.core.invoker;

import com.google.common.net.UrlEscapers;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.http.core.direct.DirectProxyConnection;
import io.gravitee.gateway.http.core.invoker.logging.LoggableProxyConnection;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class DefaultInvoker implements Invoker {

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");
    private static final String URI_PATH_SEPARATOR = "/";

    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private static final Set<String> HOP_HEADERS;

    static {
        Set<String> hopHeaders = new HashSet<>();

        // Standard HTTP headers
        hopHeaders.add(HttpHeaders.CONNECTION);
        hopHeaders.add(HttpHeaders.KEEP_ALIVE);
        hopHeaders.add(HttpHeaders.PROXY_AUTHORIZATION);
        hopHeaders.add(HttpHeaders.PROXY_AUTHENTICATE);
        hopHeaders.add(HttpHeaders.PROXY_CONNECTION);
        hopHeaders.add(HttpHeaders.TRANSFER_ENCODING);
        hopHeaders.add(HttpHeaders.TE);
        hopHeaders.add(HttpHeaders.TRAILER);
        hopHeaders.add(HttpHeaders.UPGRADE);

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    @Autowired
    protected Api api;

    @Autowired
    private EndpointManager endpointManager;

    @Override
    public Request invoke(ExecutionContext executionContext, Request serverRequest, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        TargetEndpoint targetEndpoint = selectEndpoint(serverRequest, executionContext);

        if (!targetEndpoint.isReachable()) {
            DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.SERVICE_UNAVAILABLE_503);
            connectionHandler.handle(statusOnlyConnection);
            statusOnlyConnection.sendResponse();
        } else {
            // Remove duplicate slash
            String uri = DUPLICATE_SLASH_REMOVER.matcher(targetEndpoint.uri).replaceAll(URI_PATH_SEPARATOR);

            URI requestUri = null;
            try {
                requestUri = encodeQueryParameters(serverRequest, uri);
            } catch (Exception ex) {
                serverRequest.metrics().setMessage(getStackTraceAsString(ex));

                // Request URI is not correct nor correctly encoded, returning a bad request
                DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.BAD_REQUEST_400);
                connectionHandler.handle(statusOnlyConnection);
                statusOnlyConnection.sendResponse();
            }

            if (requestUri != null) {
                uri = requestUri.toString();

                // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
                serverRequest.metrics().setEndpoint(uri);

                final HttpMethod httpMethod = extractHttpMethod(executionContext, serverRequest);

                ProxyRequest proxyRequest = ProxyRequestBuilder.from(serverRequest)
                        .uri(requestUri)
                        .method(httpMethod)
                        .rawMethod(serverRequest.rawMethod())
                        .headers(setProxyHeaders(serverRequest.headers(), requestUri, targetEndpoint.endpoint))
                        .build();

                ProxyConnection proxyConnection = targetEndpoint.endpoint.connector().request(proxyRequest);

                // Enable logging at proxy level
                if (api.getProxy().getLoggingMode().isProxyMode()) {
                    proxyConnection = new LoggableProxyConnection(proxyConnection, proxyRequest);
                }

                connectionHandler.handle(proxyConnection);

                // Plug underlying stream to connection stream
                ProxyConnection finalProxyConnection = proxyConnection;

                stream
                        .bodyHandler(buffer -> {
                            finalProxyConnection.write(buffer);

                            if (finalProxyConnection.writeQueueFull()) {
                                serverRequest.pause();
                                finalProxyConnection.drainHandler(aVoid -> serverRequest.resume());
                            }
                        })
                        .endHandler(aVoid -> finalProxyConnection.end());
            }
        }

        // Resume the incoming request to handle content and end
        serverRequest.resume();

        return serverRequest;
    }

    private HttpHeaders setProxyHeaders(HttpHeaders headers, URI requestUri, Endpoint endpoint) {
        // Remove hop-by-hop headers.
        for (String header : HOP_HEADERS) {
            headers.remove(header);
        }

        // Get HOST header
        final int port = requestUri.getPort() != -1 ? requestUri.getPort() :
                (HTTPS_SCHEME.equals(requestUri.getScheme()) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT);
        final String host = (port == DEFAULT_HTTP_PORT || port == DEFAULT_HTTPS_PORT) ?
                requestUri.getHost() : requestUri.getHost() + ':' + port;
        headers.set(HttpHeaders.HOST, host);

        // Override with default headers defined for endpoint
        if (!endpoint.headers().isEmpty()) {
            endpoint.headers().forEach(headers::put);
        }

        return headers;
    }

    private TargetEndpoint selectEndpoint(Request serverRequest, ExecutionContext executionContext) {
        // Get target if overridden by a policy
        String targetUri = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);

        return (targetUri != null)
                ? selectUserDefinedEndpoint(serverRequest, targetUri, executionContext)
                : selectLoadBalancedEndpoint(serverRequest, executionContext);
    }

    /**
     * Select an endpoint according to the underlying load_balancing system.
     */
    private TargetEndpoint selectLoadBalancedEndpoint(Request serverRequest, ExecutionContext executionContext) {
        Endpoint endpoint = nextEndpoint(serverRequest, executionContext);

        return new TargetEndpoint(
                endpoint,
                (endpoint != null) ? endpoint.target() + serverRequest.pathInfo() : null);
    }

    /**
     * Select an endpoint according to the URI passed in the execution request attribute.
     */
    private TargetEndpoint selectUserDefinedEndpoint(Request serverRequest, String target, ExecutionContext executionContext) {
        boolean trailingSlash = target.charAt(target.length() - 1) == 47;
        QueryStringDecoder decoder = new QueryStringDecoder(target);
        Map<String, List<String>> queryParameters = decoder.parameters();

        // Merge query parameters from user target into incoming request query parameters
        for(Map.Entry<String, List<String>> param : queryParameters.entrySet()) {
            serverRequest.parameters().put(param.getKey(), param.getValue());
        }

        // Path segments must be encoded to avoid bad URI syntax
        String [] segments = decoder.path().split(URI_PATH_SEPARATOR);
        StringBuilder builder = new StringBuilder();

        for(String pathSeg : segments) {
            builder.append(UrlEscapers.urlPathSegmentEscaper().escape(pathSeg)).append(URI_PATH_SEPARATOR);
        }

        String encodedTarget = builder.substring(0, builder.length() - (trailingSlash ? 0 : 1));

        // Do we have a single path or a plain URI ?
        if (encodedTarget.startsWith("/")) {
            Endpoint endpoint = nextEndpoint(serverRequest, executionContext);

            return new TargetEndpoint(
                    endpoint,
                    (endpoint != null) ? endpoint.target() + encodedTarget : null);
        } else {
            Endpoint endpoint = endpointManager.endpoints()
                    .stream()
                    .filter(endpointEntry -> encodedTarget.startsWith(endpointEntry.target()))
                    .findFirst()
                    .orElse(endpointManager.endpoints().iterator().next());

            return new TargetEndpoint(endpoint, encodedTarget);
        }
    }

    public abstract Endpoint nextEndpoint(Request serverRequest, ExecutionContext executionContext);

    private URI encodeQueryParameters(Request request, String endpointUri) throws MalformedURLException, URISyntaxException {
        if (request.parameters() != null && !request.parameters().isEmpty()) {
            QueryStringEncoder encoder = new QueryStringEncoder(endpointUri);

            for (Map.Entry<String, List<String>> queryParam : request.parameters().entrySet()) {
                if (queryParam.getValue() != null) {
                    for (String value : queryParam.getValue()) {
                        encoder.addParam(queryParam.getKey(), (value != null && !value.isEmpty()) ? value : null);
                    }
                }
            }

            return encoder.toUri();
        }

        return URI.create(endpointUri);
    }

    private HttpMethod extractHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private final class TargetEndpoint {
        private final String uri;
        private final Endpoint endpoint;

        TargetEndpoint(final Endpoint endpoint, final String uri) {
            this.endpoint = endpoint;
            this.uri = uri;
        }

        boolean isReachable() {
            return this.uri != null && this.endpoint != null && this.endpoint.available();
        }
    }
}