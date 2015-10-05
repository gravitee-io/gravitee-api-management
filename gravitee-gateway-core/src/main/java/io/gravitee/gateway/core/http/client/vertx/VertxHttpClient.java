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
package io.gravitee.gateway.core.http.client.vertx;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.definition.ProxyDefinition;
import io.gravitee.gateway.core.http.client.AbstractHttpClient;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class VertxHttpClient extends AbstractHttpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(VertxHttpClient.class);

    private HttpClient httpClient;

    public VertxHttpClient(ApiDefinition apiDefinition) {
        super(apiDefinition);
    }

    @Override
    public void invoke(Request request, Response response, Handler handler) {
        URI rewrittenURI = rewriteURI(request);
        String url = rewrittenURI.toString();
        LOGGER.debug("{} rewriting: {} -> {}", request.id(), request.uri(), url);

        HttpClientRequest clientRequest = httpClient.request(convert(request.method()), url,
                clientResponse -> handleClientResponse(request, response, clientResponse, handler));

        clientRequest.exceptionHandler(event -> {
            LOGGER.error(request.id() + " server proxying failed", event);

            if (event instanceof TimeoutException) {
                response.status(HttpStatusCode.GATEWAY_TIMEOUT_504);
            } else {
                response.status(HttpStatusCode.BAD_GATEWAY_502);
            }

            response.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
            response.end();
        });

        copyRequestHeaders(request, clientRequest);

        if (hasContent(request)) {
            String transferEncoding = request.headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
            if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                clientRequest.setChunked(true);
            }

            request.bodyHandler(buffer -> {
                LOGGER.debug("{} proxying content to upstream: {} bytes", request.id(), buffer.remaining());
                clientRequest.write(buffer.toString());
            });
        }

        request.endHandler(result -> {
            LOGGER.debug("{} proxying complete", request.id());
            clientRequest.end();
        });

        handler.handle(response);
    }

    private void handleClientResponse(Request request, Response serverResponse, HttpClientResponse clientResponse, Handler handler) {
        // Copy HTTP status
        serverResponse.status(clientResponse.statusCode());

        // Copy HTTP headers
        LOGGER.debug("{} proxying response headers to downstream");
        clientResponse.headers().forEach(header ->
                serverResponse.headers().add(header.getKey(), header.getValue()));

        String transferEncoding = serverResponse.headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
        if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
            serverResponse.chunked(true);
        }

        // Copy body content
        clientResponse.handler(buffer -> {
            LOGGER.debug("{} proxying content to downstream: {} bytes", request.id(), buffer.length());
            serverResponse.write(buffer.getByteBuf().nioBuffer());
        });

        // Signal end of the response
        clientResponse.endHandler((v) -> serverResponse.end());
    }

    protected void copyRequestHeaders(Request clientRequest, HttpClientRequest httpClientRequest) {
        for (Map.Entry<String, List<String>> headerValues : clientRequest.headers().entrySet()) {
            String headerName = headerValues.getKey();
            String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

            // Remove hop-by-hop headers.
            if (HOP_HEADERS.contains(lowerHeaderName))
                continue;

            headerValues.getValue().forEach(headerValue -> httpClientRequest.putHeader(headerName, headerValue));
        }

        httpClientRequest.putHeader(HttpHeaders.HOST, apiDefinition.getProxy().getTarget().getHost());
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        LOGGER.info("Initializing Vert.x HTTP Client with {}", apiDefinition.getProxy().getHttpClient());

        initialize(apiDefinition.getProxy());
    }

    private io.vertx.core.http.HttpMethod convert(io.gravitee.common.http.HttpMethod httpMethod) {
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
        super.doStop();

        LOGGER.info("Close Vert.x HTTP Client for {}", apiDefinition);
        httpClient.close();
    }

    private void initialize(ProxyDefinition proxyDefinition) {
        HttpClientOptions options = new HttpClientOptions();
        options.setKeepAlive(false);
        options.setTcpNoDelay(true);
        options.setUsePooledBuffers(true);
        options.setIdleTimeout(10);
        options.setConnectTimeout(5000);
        options.setMaxPoolSize(100);
        options.setDefaultHost(proxyDefinition.getTarget().getHost());
        int port = proxyDefinition.getTarget().getPort();

        if (port != -1) {
            options.setDefaultPort(port);
        }

        httpClient = Vertx.vertx().createHttpClient(options);
    }
}
