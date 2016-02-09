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
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
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

    protected URI rewriteURI(Request request, URI endpointUri) {
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

    protected HttpMethod extractHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }
}
