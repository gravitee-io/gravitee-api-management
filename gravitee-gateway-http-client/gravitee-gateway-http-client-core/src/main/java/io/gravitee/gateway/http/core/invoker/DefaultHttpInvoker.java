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

import io.gravitee.common.http.*;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.http.core.endpoint.HttpEndpoint;
import io.gravitee.gateway.http.core.logger.LoggableProxyConnection;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultHttpInvoker implements Invoker {

	// Pattern reuse for duplicate slash removal
	private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");

    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private static final Set<String> HOP_HEADERS;

    private static final URLCodec URL_ENCODER = new URLCodec(StandardCharsets.UTF_8.name());

    private final static String TARGET_URI_ATTRIBUTE = ExecutionContext.ATTR_PREFIX + "target.uri";

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

        // Gravitee HTTP headers
        hopHeaders.add(GraviteeHttpHeader.X_GRAVITEE_API_NAME);

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    @Autowired
    protected Api api;

    @Autowired
    protected EndpointManager<HttpClient> endpointManager;

    @Override
    public Request invoke(ExecutionContext executionContext, Request serverRequest, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        HttpEndpoint endpoint = selectEndpoint(serverRequest, executionContext);
        String targetUri = (String) executionContext.getAttribute(TARGET_URI_ATTRIBUTE);

        // No endpoint has been selected by load-balancer strategy nor overridden value
        if (targetUri == null || endpoint == null || endpoint.definition().getStatus() == Endpoint.Status.DOWN) {
            ServiceUnavailableConnection serviceUnavailableConnection = new ServiceUnavailableConnection();
            connectionHandler.handle(serviceUnavailableConnection);
            serviceUnavailableConnection.sendServerUnavailableResponse();
        }

        // Remove duplicate slash
        targetUri = DUPLICATE_SLASH_REMOVER.matcher(targetUri).replaceAll("/");

        URI requestUri;
        try {
            requestUri = encodeQueryParameters(serverRequest, targetUri);
        } catch (EncoderException e) {
            throw new IllegalStateException("Query is not well-formed");
        }

        String uri = requestUri.toString();

        // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
        serverRequest.metrics().setEndpoint(uri);

        final boolean enableHttpDump = api.getProxy().isDumpRequest();
        final HttpMethod httpMethod = extractHttpMethod(executionContext, serverRequest);

        final int port = requestUri.getPort() != -1 ? requestUri.getPort() :
                (HTTPS_SCHEME.equals(requestUri.getScheme()) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT);
        final String host = (port == DEFAULT_HTTP_PORT || port == DEFAULT_HTTPS_PORT) ?
                requestUri.getHost() : requestUri.getHost() + ':' + port;

        ProxyRequest proxyRequest = ProxyRequestBuilder.from(serverRequest)
                .uri(requestUri)
                .method(httpMethod)
                .headers(proxyRequestHeaders(serverRequest.headers(), host, endpoint.definition()))
                .build();

        // Create a copy of request headers send by the gateway to the backend
        serverRequest.metrics().setProxyRequestHeaders(proxyRequest.headers());

        ProxyConnection proxyConnection = endpoint.connector().request(proxyRequest);

        if (enableHttpDump) {
            proxyConnection = new LoggableProxyConnection(proxyConnection, proxyRequest);
        }

        connectionHandler.handle(proxyConnection);

        // Plug underlying stream to connection stream
        ProxyConnection finalProxyConnection = proxyConnection;
        stream
                .bodyHandler(finalProxyConnection::write)
                .endHandler(aVoid -> finalProxyConnection.end());

        // Resume the incoming request to handle content and end
        serverRequest.resume();

        return serverRequest;
    }

    private HttpHeaders proxyRequestHeaders(HttpHeaders serverHeaders, String host, Endpoint endpoint) {
        HttpHeaders proxyRequestHeaders = new HttpHeaders();
        for (Map.Entry<String, List<String>> headerValues : serverHeaders.entrySet()) {
            String headerName = headerValues.getKey();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers.
            if (HOP_HEADERS.contains(lowerHeaderName)) {
                continue;
            }

            proxyRequestHeaders.put(headerName, headerValues.getValue());
        }

        if (endpoint.getHostHeader() != null && !endpoint.getHostHeader().isEmpty()) {
            proxyRequestHeaders.set(HttpHeaders.HOST, endpoint.getHostHeader());
        } else {
            proxyRequestHeaders.set(HttpHeaders.HOST, host);
        }

        return proxyRequestHeaders;
    }

    private HttpEndpoint selectEndpoint(Request serverRequest, ExecutionContext executionContext) {
        // Get target if overridden by a policy
        String targetUri = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);
        HttpEndpoint endpoint;

        // If not defined, use the one provided by the underlying load-balancer
        if (targetUri == null) {
            String endpointName = endpointManager.loadbalancer().next();
            endpoint = (HttpEndpoint) endpointManager.get(endpointName);

            targetUri = (endpoint != null) ? rewriteURI(serverRequest, endpoint.target()) : null;

            // Set the final target URI invoked
            executionContext.setAttribute(TARGET_URI_ATTRIBUTE, targetUri);
        } else {
            // Select a matching endpoint according to the URL
            // If none, select the first (non-backup) from the endpoint list.
            String finalTargetUri = targetUri;
            executionContext.setAttribute(TARGET_URI_ATTRIBUTE, finalTargetUri);

            Optional<String> endpointName = endpointManager.targetByEndpoint()
                    .entrySet()
                    .stream()
                    .filter(endpointEntry -> finalTargetUri.startsWith(endpointEntry.getValue()))
                    .map(Map.Entry::getKey)
                    .findFirst();

            endpoint = (HttpEndpoint) endpointManager.getOrDefault(endpointName.orElse(null));
        }

        return endpoint;
    }

    private String rewriteURI(Request request, String endpointUri) {
        return endpointUri + request.pathInfo();
    }

    private URI encodeQueryParameters(Request request, String endpointUri) throws EncoderException {
        final StringBuilder requestURI = new StringBuilder(endpointUri);

        if (request.parameters() != null && !request.parameters().isEmpty()) {
            StringBuilder query = new StringBuilder();
            query.append('?');

            for (Map.Entry<String, List<String>> queryParam : request.parameters().entrySet()) {
                if (queryParam.getValue() != null) {
                    for (String value : queryParam.getValue()) {
                        query.append(URL_ENCODER.encode(queryParam.getKey()));
                        if (value != null && !value.isEmpty()) {
                            query
                                    .append('=')
                                    .append(URL_ENCODER.encode(value));
                        }

                        query.append('&');
                    }
                }
            }

            // Removing latest & separator and encode query parameters
            requestURI.append(query.deleteCharAt(query.length() - 1).toString());
        }

        return URI.create(requestURI.toString());
    }

    private HttpMethod extractHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }

    private class ServiceUnavailableConnection implements ProxyConnection {

        private Handler<ProxyResponse> responseHandler;

        @Override
        public WriteStream<Buffer> write(Buffer content) {
            return null;
        }

        @Override
        public void end() {

        }

        @Override
        public ProxyConnection responseHandler(Handler<ProxyResponse> responseHandler) {
            this.responseHandler = responseHandler;
            return this;
        }

        private void sendServerUnavailableResponse() {
            ServiceUnavailableResponse response = new ServiceUnavailableResponse();
            this.responseHandler.handle(response);
            response.endHandler().handle(null);
        }
    }

    private class ServiceUnavailableResponse implements ProxyResponse {

        private Handler<Buffer> bodyHandler;
        private Handler<Void> endHandler;

        private final HttpHeaders httpHeaders = new HttpHeaders();

        public ServiceUnavailableResponse() {
            httpHeaders.set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
        }

        @Override
        public int status() {
            return HttpStatusCode.SERVICE_UNAVAILABLE_503;
        }

        @Override
        public HttpHeaders headers() {
            return httpHeaders;
        }

        @Override
        public ProxyResponse bodyHandler(Handler<Buffer> bodyHandler) {
            this.bodyHandler = bodyHandler;
            return this;
        }

        Handler<Buffer> bodyHandler() {
            return this.bodyHandler;
        }

        @Override
        public ProxyResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        Handler<Void> endHandler() {
            return this.endHandler;
        }

        @Override
        public ReadStream<Buffer> resume() {
            endHandler.handle(null);
            return this;
        }
    }
}
