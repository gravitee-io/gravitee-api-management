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
package io.gravitee.gateway.http.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.ClientRequest;
import io.gravitee.gateway.api.ClientResponse;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.BodyPart;
import io.gravitee.gateway.http.core.endpoint.EndpointResolver;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxHttpClientInvoker extends AbstractHttpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(VertxHttpClientInvoker.class);

    private HttpClient httpClient;

    @Resource
    private Vertx vertx;

    @Resource
    private EndpointResolver endpointResolver;

    @Override
    public ClientRequest invoke(Request serverRequest, Handler<ClientResponse> clientResponseHandler) {
        // Resolve target endpoint
        URI endpoint = endpointResolver.resolve(serverRequest);

        // TODO: how to pass this to the response metrics
        // serverResponse.metrics().setEndpoint(endpoint.toString());


        URI rewrittenURI = rewriteURI(serverRequest, endpoint);
        String url = rewrittenURI.toString();
        LOGGER.debug("{} rewriting: {} -> {}", serverRequest.id(), serverRequest.uri(), url);


        HttpClientRequest clientRequest = httpClient.request(
                convert(serverRequest.method()),
                endpoint.getPort() == -1 ? 80 : endpoint.getPort(),
                endpoint.getHost(),
                url,
                clientResponse -> handleClientResponse(clientResponse, clientResponseHandler));

        ClientRequest invokerRequest = new ClientRequest() {
            @Override
            public ClientRequest write(BodyPart bodyPart) {
                ByteBuffer byteBuffer = bodyPart.getBodyPartAsByteBuffer();
                LOGGER.debug("{} proxying content to upstream: {} bytes", serverRequest.id(), byteBuffer.remaining());
                clientRequest.write(byteBuffer.toString());

                return this;
            }

            @Override
            public void end() {
                LOGGER.debug("{} proxying complete", serverRequest.id());
                clientRequest.end();
            }
        };

        clientRequest.exceptionHandler(event -> {
            LOGGER.error(serverRequest.id() + " server proxying failed", event);

            VertxClientResponse clientResponse = new VertxClientResponse((event instanceof TimeoutException) ?
                    HttpStatusCode.GATEWAY_TIMEOUT_504 : HttpStatusCode.BAD_GATEWAY_502);

            clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);

            clientResponseHandler.handle(clientResponse);

            clientResponse.endHandler().handle(null);
        });

        // Copy headers to final API
        copyRequestHeaders(serverRequest, clientRequest);

        // Check chuncked flag on the request if there are some content to push and if transfer_encoding is set
        // with chunked value
        if (hasContent(serverRequest)) {
            String transferEncoding = serverRequest.headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
            if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                clientRequest.setChunked(true);
            }
        }

        return invokerRequest;
    }

    private void handleClientResponse(HttpClientResponse clientResponse,
                                      Handler<ClientResponse> clientResponseHandler) {
        VertxClientResponse proxyClientResponse = new VertxClientResponse(
                clientResponse.statusCode());

        // Copy HTTP headers
        clientResponse.headers().forEach(header ->
                proxyClientResponse.headers().add(header.getKey(), header.getValue()));

        // Copy body content
        clientResponse.handler(event -> proxyClientResponse.bodyHandler().handle(new VertxBufferBodyPart(event)));

        // Signal end of the response
        clientResponse.endHandler(v -> proxyClientResponse.endHandler().handle(null));

        clientResponseHandler.handle(proxyClientResponse);
    }

    protected void copyRequestHeaders(Request clientRequest, HttpClientRequest httpClientRequest) {
        for (Map.Entry<String, List<String>> headerValues : clientRequest.headers().entrySet()) {
            String headerName = headerValues.getKey();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers.
            if (HOP_HEADERS.contains(lowerHeaderName)) {
                continue;
            }

            headerValues.getValue().forEach(headerValue -> httpClientRequest.putHeader(headerName, headerValue));
        }

//        httpClientRequest.putHeader(HttpHeaders.HOST, api.getProxy().getTarget().getHost());
    }

    @Override
    protected void doStart() throws Exception {
        LOGGER.info("Starting HTTP Client for API {}", api);

        initialize(api.getProxy());
    }

    private HttpMethod convert(io.gravitee.common.http.HttpMethod httpMethod) {
        switch (httpMethod) {
            case CONNECT:
                return HttpMethod.CONNECT;
            case DELETE:
                return HttpMethod.DELETE;
            case GET:
                return HttpMethod.GET;
            case HEAD:
                return HttpMethod.HEAD;
            case OPTIONS:
                return HttpMethod.OPTIONS;
            case PATCH:
                return HttpMethod.PATCH;
            case POST:
                return HttpMethod.POST;
            case PUT:
                return HttpMethod.PUT;
            case TRACE:
                return HttpMethod.TRACE;
        }

        return null;
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("Close Vert.x HTTP Client for {}", api);
        httpClient.close();
    }

    private void initialize(Proxy proxyDefinition) {
        Objects.requireNonNull(proxyDefinition, "Proxy must not be null");

        LOGGER.info("Initializing Vert.x HTTP Client with {}", proxyDefinition.getHttpClient());

        HttpClientOptions options = new HttpClientOptions();
        options.setKeepAlive(true);
        options.setUsePooledBuffers(true);
        options.setIdleTimeout(10);
        options.setConnectTimeout(5000);
        options.setMaxPoolSize(100);

        httpClient = vertx.createHttpClient(options);
        LOGGER.info("Vert.x HTTP Client created {}", httpClient);
    }
}
