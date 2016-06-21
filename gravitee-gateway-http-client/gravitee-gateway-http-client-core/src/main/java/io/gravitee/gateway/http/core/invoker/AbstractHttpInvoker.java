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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.*;
import io.gravitee.gateway.api.buffer.Buffer;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractHttpInvoker implements Invoker {

    @Autowired
    protected Api api;

    @Autowired
    protected HttpClient httpClient;

    @Autowired
    protected LoadBalancerStrategy loadBalancer;

    @Override
    public ClientRequest invoke(ExecutionContext executionContext, Request serverRequest, Handler<ClientResponse> result) {
        // Get overriden endpoint
        String sEndpoint = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT);

        // If not defined, use the one provided by load-balancer
        if (sEndpoint == null) {
            sEndpoint = loadBalancer.chooseEndpoint(serverRequest);
            sEndpoint = rewriteURI(serverRequest, sEndpoint);
        }

        // Remove duplicate slash
        sEndpoint = sEndpoint.replaceAll("(?<!(http:|https:))[//]+", "/");

        URI endpoint = encodeQueryParameters(serverRequest, sEndpoint);

        String uri = endpoint.getPath();

        // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the
        // initial request
        serverRequest.metrics().setEndpoint(uri);

        if (endpoint.getQuery() != null)
            uri += '?' + endpoint.getQuery();

        final int port = endpoint.getPort() != -1 ? endpoint.getPort() :
                (endpoint.getScheme().equals("https") ? 443 : 80);

        final boolean enableHttpDump = api.getProxy().getHttpClient().getOptions().isDumpRequest();
        final HttpMethod httpMethod = extractHttpMethod(executionContext, serverRequest);

        ClientRequest clientRequest = invoke0(endpoint.getHost(), port, httpMethod,
                uri, serverRequest, executionContext,
                (enableHttpDump) ? loggableClientResponse(result, serverRequest) : result);

        if (enableHttpDump) {
            HttpDump.logger.info("{} >> Rewriting: {} -> {}", serverRequest.id(), serverRequest.uri(), uri);
            HttpDump.logger.info("{} >> {} {}", serverRequest.id(), httpMethod, endpoint.toString());

            serverRequest.headers().forEach((headerName, headerValues) -> HttpDump.logger.info("{} >> {}: {}",
                    serverRequest.id(), headerName, headerValues.stream().collect(Collectors.joining(","))));

            clientRequest = new LoggableClientRequest(clientRequest, serverRequest);
        }

        if (executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_BODY_CONTENT) == null) {
            final ClientRequest finalClientRequest = clientRequest;
            serverRequest
                    .bodyHandler(clientRequest::write)
                    .endHandler(endResult -> finalClientRequest.end());
        } else {
            String content = (String) executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_BODY_CONTENT);
            clientRequest.write(Buffer.buffer(content));
            clientRequest.end();
        }

        return clientRequest;
    }

    private Handler<ClientResponse> loggableClientResponse(Handler<ClientResponse> clientResponseHandler, Request request) {
        return result -> clientResponseHandler.handle(new LoggableClientResponse(result, request));
    }

    protected String nextEndpoint(ExecutionContext executionContext, Request serverRequest) {
        return loadBalancer.chooseEndpoint(serverRequest);
    }

    protected abstract ClientRequest invoke0(String host, int port, HttpMethod method, String requestUri, Request serverRequest, ExecutionContext executionContext, Handler<ClientResponse> response);

    private String rewriteURI(Request request, String endpointUri) {
        final StringBuilder requestURI =
                new StringBuilder(request.path())
                        .delete(0, api.getProxy().getContextPath().length())
                        .insert(0, endpointUri);

        return requestURI.toString();
    }

    private URI encodeQueryParameters(Request request, String endpointUri) {
        final StringBuilder requestURI =
                new StringBuilder(endpointUri);

        if (request.parameters() != null && ! request.parameters().isEmpty()) {
            StringBuilder query = new StringBuilder();
            query.append('?');

            for(Map.Entry<String, String> queryParam : request.parameters().entrySet()) {
                query.append(queryParam.getKey());
                if (queryParam.getValue() != null && !queryParam.getValue().isEmpty()) {
                    query.append('=').append(queryParam.getValue());
                }

                query.append('&');
            }

            // Removing latest & separator and encode query parameters
            try {
                requestURI.append(URLEncoder.encode(query.deleteCharAt(query.length() - 1).toString(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return URI.create(requestURI.toString());
    }

    private HttpMethod extractHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }
}
