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
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.client.HttpClient;
import io.gravitee.gateway.api.http.loadbalancer.LoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Map;

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
    protected LoadBalancer loadBalancer;

    @Override
    public ClientRequest invoke(ExecutionContext executionContext, Request serverRequest, Handler<ClientResponse> result) {
        // Get endpoint
        String sEndpoint = nextEndpoint(executionContext, serverRequest);

        // Endpoint URI
        URI endpoint = URI.create(sEndpoint);

        // TODO: how to pass this to the response metrics
        // serverResponse.metrics().setEndpoint(endpoint.toString());

        URI rewrittenURI = rewriteURI(serverRequest, endpoint);

        String uri = rewrittenURI.getPath();
        if (rewrittenURI.getQuery() != null)
            uri += '?' + rewrittenURI.getQuery();

        final int port = endpoint.getPort() != -1 ? endpoint.getPort() :
                (endpoint.getScheme().equals("https") ? 443 : 80);

        return invoke0(endpoint.getHost(), port, extractHttpMethod(executionContext, serverRequest),
                uri, serverRequest, executionContext, result);
    }

    protected String nextEndpoint(ExecutionContext executionContext, Request serverRequest) {
        return loadBalancer.chooseEndpoint(serverRequest);
    }

    protected abstract ClientRequest invoke0(String host, int port, HttpMethod method, String requestUri, Request serverRequest, ExecutionContext executionContext, Handler<ClientResponse> response);

    private URI rewriteURI(Request request, URI endpointUri) {
        final StringBuilder requestURI =
                new StringBuilder(request.path())
                        .delete(0, api.getProxy().getContextPath().length())
                        .insert(0, endpointUri.toString());

        if (request.parameters() != null && ! request.parameters().isEmpty()) {
            requestURI.append('?');

            for(Map.Entry<String, String> queryParam : request.parameters().entrySet()) {
                requestURI.append(queryParam.getKey());
                if (queryParam.getValue() != null && !queryParam.getValue().isEmpty()) {
                    requestURI.append('=').append(queryParam.getValue());
                }

                requestURI.append('&');
            }

            // Removing latest & separator
            requestURI.deleteCharAt(requestURI.length() - 1);
        }

        return URI.create(requestURI.toString());
    }

    private HttpMethod extractHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }
}
