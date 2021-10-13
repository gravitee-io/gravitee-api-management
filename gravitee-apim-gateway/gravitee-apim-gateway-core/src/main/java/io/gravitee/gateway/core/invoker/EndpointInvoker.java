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
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.resolver.EndpointResolver;
import io.gravitee.gateway.api.endpoint.resolver.ProxyEndpoint;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.core.logging.LoggableProxyConnectionDecorator;
import io.gravitee.gateway.core.proxy.DirectProxyConnection;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EndpointInvoker implements Invoker {

    private final EndpointResolver endpointResolver;

    public EndpointInvoker(final EndpointResolver endpointResolver) {
        this.endpointResolver = endpointResolver;
    }

    @Override
    public void invoke(ExecutionContext context, ReadStream<Buffer> stream, Handler<ProxyConnection> connectionHandler) {
        // Look at the request context attribute to retrieve the final target (which could be overridden by a policy)
        final ProxyEndpoint endpoint = endpointResolver.resolve((String) context.getAttribute(ExecutionContext.ATTR_REQUEST_ENDPOINT));

        // Endpoint can be null if none endpoint can be selected or if the selected endpoint is unavailable
        if (endpoint == null || !endpoint.available()) {
            DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.SERVICE_UNAVAILABLE_503);
            connectionHandler.handle(statusOnlyConnection);
            statusOnlyConnection.sendResponse();
        } else {
            try {
                final ProxyRequest proxyRequest = endpoint.createProxyRequest(
                    context.request(),
                    proxyRequestBuilder -> proxyRequestBuilder.method(getHttpMethod(context))
                );

                endpoint
                    .connector()
                    .request(
                        proxyRequest,
                        context,
                        proxyConnection -> {
                            final ProxyConnection decoratedProxyConnection = LoggableProxyConnectionDecorator.decorate(
                                proxyConnection,
                                proxyRequest,
                                context
                            );

                            connectionHandler.handle(decoratedProxyConnection);

                            // Plug underlying stream to connection stream
                            stream
                                .bodyHandler(
                                    buffer -> {
                                        decoratedProxyConnection.write(buffer);

                                        if (decoratedProxyConnection.writeQueueFull()) {
                                            context.request().pause();
                                            decoratedProxyConnection.drainHandler(aVoid -> context.request().resume());
                                        }
                                    }
                                )
                                .endHandler(aVoid -> decoratedProxyConnection.end());

                            // Inbound request could only be resume once we succeed to connect to the underlying backend
                            // otherwise, the endHandler will be called first (seems to be a breaking change since Vert.x 4.x)
                            context.request().resume();
                        }
                    );
            } catch (Exception ex) {
                context.request().metrics().setMessage(getStackTraceAsString(ex));

                // Request URI is not correct nor correctly encoded, returning a bad request
                DirectProxyConnection statusOnlyConnection = new DirectProxyConnection(HttpStatusCode.BAD_REQUEST_400);
                connectionHandler.handle(statusOnlyConnection);
                statusOnlyConnection.sendResponse();
            }
        }
    }

    private HttpMethod getHttpMethod(ExecutionContext context) {
        io.gravitee.common.http.HttpMethod overrideMethod = (io.gravitee.common.http.HttpMethod) context.getAttribute(
            ExecutionContext.ATTR_REQUEST_METHOD
        );
        return (overrideMethod == null) ? context.request().method() : overrideMethod;
    }

    private static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}
