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
package io.gravitee.gateway.core.invoker;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.builder.ProxyRequestBuilder;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.core.logging.LoggableProxyConnection;
import io.gravitee.gateway.core.proxy.DirectProxyConnection;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultInvoker implements Invoker {

    @Autowired
    protected Api api;

    @Autowired
    private EndpointResolver endpointResolver;

    @Override
    public Request invoke(ExecutionContext executionContext, Request serverRequest, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        EndpointResolver.ResolvedEndpoint endpoint = endpointResolver.resolve(serverRequest, executionContext);

        if (endpoint == null) {
            DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.SERVICE_UNAVAILABLE_503);
            connectionHandler.handle(statusOnlyConnection);
            statusOnlyConnection.sendResponse();
        } else {
            URI uri = null;
            try {
                uri = encodeQueryParameters(endpoint.getUri(), serverRequest.parameters());
            } catch (Exception ex) {
                serverRequest.metrics().setMessage(getStackTraceAsString(ex));

                // Request URI is not correct nor correctly encoded, returning a bad request
                DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.BAD_REQUEST_400);
                connectionHandler.handle(statusOnlyConnection);
                statusOnlyConnection.sendResponse();
            }

            if (uri != null) {
                // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
                serverRequest.metrics().setEndpoint(uri.toString());

                ProxyRequest proxyRequest = ProxyRequestBuilder.from(serverRequest)
                        .uri(uri)
                        .method(setHttpMethod(executionContext, serverRequest))
                        .rawMethod(serverRequest.rawMethod())
                        .headers(serverRequest.headers())
                        .build();

                ProxyConnection proxyConnection = endpoint.getConnector().request(proxyRequest);

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

    private URI encodeQueryParameters(String uri, MultiValueMap<String, String> parameters) throws MalformedURLException, URISyntaxException {
        if (parameters != null && !parameters.isEmpty()) {
            QueryStringEncoder encoder = new QueryStringEncoder(uri);

            for (Map.Entry<String, List<String>> queryParam : parameters.entrySet()) {
                if (queryParam.getValue() != null) {
                    for (String value : queryParam.getValue()) {
                        encoder.addParam(queryParam.getKey(), (value != null && !value.isEmpty()) ? value : null);
                    }
                }
            }

            return encoder.toUri();
        }

        return URI.create(uri);
    }

    private HttpMethod setHttpMethod(ExecutionContext executionContext, Request request) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod)
                executionContext.getAttribute(ExecutionContext.ATTR_REQUEST_METHOD);
        return (overrideMethod == null) ? request.method() : overrideMethod;
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}