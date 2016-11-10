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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.*;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancerStrategy;
import io.gravitee.gateway.http.core.logger.HttpDump;
import io.gravitee.gateway.http.core.logger.LoggableClientRequest;
import io.gravitee.gateway.http.core.logger.LoggableClientResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultHttpInvoker implements Invoker {

	// Pattern reuse for duplicate slash removal
	private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");
	
    @Autowired
    protected Api api;

    @Autowired
    protected EndpointManager<HttpClient> endpointManager;

    @Autowired
    protected LoadBalancerStrategy loadBalancer;

    @Override
    public ClientRequest invoke(ExecutionContext executionContext, Request serverRequest, Handler<ClientResponse> result) {
        // Get target if overridden by a policy
        String targetUri = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);
        Endpoint<HttpClient> endpoint;

        // If not defined, use the one provided by the underlying load-balancer
        if (targetUri == null) {
            String endpointName = nextEndpoint(executionContext);
            endpoint = endpointManager.get(endpointName);

            targetUri = (endpoint != null) ? rewriteURI(serverRequest, endpoint.target()) : null;

            // Set the final target URI invoked
            executionContext.setAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT, targetUri);
        } else {
            // Select a matching endpoint according to the URL
            // If none, select the first (non-backup) from the endpoint list.
            String finalTargetUri = targetUri;

            Optional<String> endpointName = endpointManager.targetByEndpoint()
                    .entrySet()
                    .stream()
                    .filter(endpointEntry -> finalTargetUri.startsWith(endpointEntry.getValue()))
                    .map(Map.Entry::getValue)
                    .findFirst();

            endpoint = endpointManager.getOrDefault(endpointName.isPresent() ? endpointName.get() : null);
        }

        // No endpoint has been selected by load-balancer strategy nor overridden value
        if (targetUri == null) {
            ServiceUnavailableResponse clientResponse = new ServiceUnavailableResponse();
            result.handle(clientResponse);
            clientResponse.endHandler().handle(null);
            return null;
        }

        // Remove duplicate slash
        targetUri = DUPLICATE_SLASH_REMOVER.matcher(targetUri).replaceAll("/");

        URI requestUri = encodeQueryParameters(serverRequest, targetUri);
        String uri = requestUri.toString();

        // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
        serverRequest.metrics().setEndpoint(uri);

        final boolean enableHttpDump = api.getProxy().isDumpRequest();
        final HttpMethod httpMethod = extractHttpMethod(executionContext, serverRequest);

        ClientRequest clientRequest = invoke0(endpoint.connector(), httpMethod,
                requestUri, serverRequest, executionContext,
                (enableHttpDump) ? loggableClientResponse(result, serverRequest) : result);

        if (enableHttpDump) {
            HttpDump.logger.info("{}/{} >> Rewriting: {} -> {}", serverRequest.id(), serverRequest.transactionId(), serverRequest.uri(), uri);
            HttpDump.logger.info("{}/{} >> {} {}", serverRequest.id(), serverRequest.transactionId(), httpMethod, uri);

            serverRequest.headers().forEach((headerName, headerValues) -> HttpDump.logger.info("{}/{} >> {}: {}",
                    serverRequest.id(), serverRequest.transactionId(), headerName, headerValues.stream().collect(Collectors.joining(","))));

            clientRequest = new LoggableClientRequest(clientRequest, serverRequest);
        }

        return clientRequest;
    }

    private Handler<ClientResponse> loggableClientResponse(Handler<ClientResponse> clientResponseHandler, Request request) {
        return result -> clientResponseHandler.handle(new LoggableClientResponse(result, request));
    }

    protected String nextEndpoint(ExecutionContext executionContext) {
        return loadBalancer.next();
    }

    protected ClientRequest invoke0(HttpClient httpClient, HttpMethod method, URI uri, Request request,
                                    ExecutionContext executionContext, Handler<ClientResponse> response) {
        return httpClient.request(method, uri, request.headers(), response);
    }

    private String rewriteURI(Request request, String endpointUri) {
        return endpointUri + request.pathInfo();
    }

    private URI encodeQueryParameters(Request request, String endpointUri) {
        final StringBuilder requestURI =
                new StringBuilder(endpointUri);

        if (request.parameters() != null && !request.parameters().isEmpty()) {
            StringBuilder query = new StringBuilder();
            query.append('?');

            for (Map.Entry<String, String> queryParam : request.parameters().entrySet()) {
                query.append(queryParam.getKey());
                if (queryParam.getValue() != null && !queryParam.getValue().isEmpty()) {
                    try {
                        query.append('=').append(URLEncoder.encode(queryParam.getValue(), StandardCharsets.UTF_8.name()));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                query.append('&');
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

    private class ServiceUnavailableResponse implements ClientResponse {

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
        public ClientResponse bodyHandler(Handler<Buffer> bodyHandler) {
            this.bodyHandler = bodyHandler;
            return this;
        }

        Handler<Buffer> bodyHandler() {
            return this.bodyHandler;
        }

        @Override
        public ClientResponse endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }

        Handler<Void> endHandler() {
            return this.endHandler;
        }
    }
}
