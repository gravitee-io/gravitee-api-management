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
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.http.core.client.AbstractHttpClient;
import io.netty.channel.ConnectTimeoutException;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpClient extends AbstractHttpClient {

    private final Logger LOGGER = LoggerFactory.getLogger(VertxHttpClient.class);

    private static final String HTTPS_SCHEME = "https";

    @Resource
    private Vertx vertx;

    private final Endpoint endpoint;

    private HttpClientOptions httpClientOptions;

    @Autowired
    public VertxHttpClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    private final Map<Context, HttpClient> httpClients = new HashMap<>();

    @Override
    public ProxyConnection request(ProxyRequest proxyRequest, Handler<ProxyResponse> responseHandler) {
        HttpClient httpClient = httpClients.computeIfAbsent(Vertx.currentContext(), createHttpClient());

        final URI uri = proxyRequest.uri();
        final int port = uri.getPort() != -1 ? uri.getPort() :
                (HTTPS_SCHEME.equals(uri.getScheme()) ? 443 : 80);

        String relativeUri = (uri.getRawQuery() == null) ? uri.getRawPath() : uri.getRawPath() + '?' + uri.getRawQuery();

        // Prepare request
        HttpClientRequest clientRequest = httpClient.request(
                convert(proxyRequest.method()), port, uri.getHost(), relativeUri);
        clientRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());

        VertxProxyConnection proxyConnection = new VertxProxyConnection(clientRequest);
        clientRequest.handler(clientResponse -> handleClientResponse(proxyConnection, clientResponse, responseHandler));

        clientRequest.exceptionHandler(event -> {
            if (! proxyConnection.isCanceled()) {
                LOGGER.error("Server proxying failed: {}", event.getMessage());
                proxyRequest.request().metrics().setMessage(event.getMessage());

                if (proxyConnection.connectTimeoutHandler() != null && event instanceof ConnectTimeoutException) {
                    proxyConnection.connectTimeoutHandler().handle(event);
                } else {
                    VertxProxyResponse clientResponse = new VertxProxyResponse(
                            ((event instanceof ConnectTimeoutException) || (event instanceof TimeoutException)) ?
                                    HttpStatusCode.GATEWAY_TIMEOUT_504 : HttpStatusCode.BAD_GATEWAY_502);

                    clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                    responseHandler.handle(clientResponse);

                    clientResponse.endHandler().handle(null);
                }
            }
        });

        // Copy headers to upstream
        copyRequestHeaders(proxyRequest.headers(), clientRequest);

        // Check chunk flag on the request if there are some content to push and if transfer_encoding is set
        // with chunk value
        if (hasContent(proxyRequest.headers())) {
            String transferEncoding = proxyRequest.headers().getFirst(HttpHeaders.TRANSFER_ENCODING);
            if (HttpHeadersValues.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
                clientRequest.setChunked(true);
            }
        }

        return proxyConnection;
    }

    private void handleClientResponse(VertxProxyConnection proxyConnection, HttpClientResponse clientResponse,
                                      Handler<ProxyResponse> proxyResponseHandler) {
        VertxProxyResponse proxyClientResponse = new VertxProxyResponse(
                clientResponse.statusCode());

        proxyConnection.setProxyResponse(proxyClientResponse);

        // Copy HTTP headers
        clientResponse.headers().names().forEach(headerName ->
                proxyClientResponse.headers().put(headerName, clientResponse.headers().getAll(headerName)));

        // Copy body content
        clientResponse.handler(event -> proxyClientResponse.bodyHandler().handle(Buffer.buffer(event.getBytes())));

        // Signal end of the response
        clientResponse.endHandler(v -> proxyClientResponse.endHandler().handle(null));

        proxyResponseHandler.handle(proxyClientResponse);
    }

    private void copyRequestHeaders(HttpHeaders headers, HttpClientRequest httpClientRequest) {
        for (Map.Entry<String, List<String>> headerValues : headers.entrySet()) {
            httpClientRequest.putHeader(headerValues.getKey(), headerValues.getValue());
        }
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
    protected void doStart() throws Exception {
        // TODO: Prepare HttpClientOptions according to the endpoint to improve performance when creating a new
        // instance of the Vertx client
        httpClientOptions = new HttpClientOptions();

        httpClientOptions.setPipelining(endpoint.getHttpClientOptions().isPipelining());
        httpClientOptions.setKeepAlive(endpoint.getHttpClientOptions().isKeepAlive());
        httpClientOptions.setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000));
        httpClientOptions.setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout());
        httpClientOptions.setUsePooledBuffers(true);
        httpClientOptions.setMaxPoolSize(endpoint.getHttpClientOptions().getMaxConcurrentConnections());
        httpClientOptions.setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());

        // Configure proxy
        HttpProxy proxy = endpoint.getHttpProxy();
        if (proxy != null && proxy.isEnabled()) {
            ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setHost(proxy.getHost());
            proxyOptions.setPort(proxy.getPort());
            proxyOptions.setUsername(proxy.getUsername());
            proxyOptions.setPassword(proxy.getPassword());
            proxyOptions.setType(ProxyType.valueOf(proxy.getType().name()));

            httpClientOptions.setProxyOptions(proxyOptions);
        }

        URI target = URI.create(endpoint.getTarget());
        // Configure SSL
        HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();
        if (sslOptions != null && sslOptions.isEnabled()) {
            httpClientOptions
                    .setSsl(sslOptions.isEnabled())
                    .setVerifyHost(sslOptions.isHostnameVerifier())
                    .setTrustAll(sslOptions.isTrustAll());

            if (sslOptions.getPem() != null && ! sslOptions.getPem().isEmpty()) {
                httpClientOptions.setPemTrustOptions(
                        new PemTrustOptions().addCertValue(
                                io.vertx.core.buffer.Buffer.buffer(sslOptions.getPem())));
            }
        } else if(HTTPS_SCHEME.equalsIgnoreCase(target.getScheme())) {
            // SSL is not configured but the endpoint scheme is HTTPS so let's enable the SSL on Vert.x HTTP client
            // automatically
            httpClientOptions.setSsl(true).setTrustAll(true);
        }

        printHttpClientConfiguration(httpClientOptions);
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("Closing HTTP Client for '{}' endpoint [{}]", endpoint.getName(), endpoint.getTarget());

        httpClients.values().stream().forEach(httpClient -> {
            try {
                httpClient.close();
            } catch (IllegalStateException ise) {
                LOGGER.warn(ise.getMessage());
            }
        });
    }

    private Function<Context, HttpClient> createHttpClient() {
        return context -> vertx.createHttpClient(httpClientOptions);
    }

    private void printHttpClientConfiguration(HttpClientOptions httpClientOptions) {
        LOGGER.info("Create HTTP Client with configuration: ");
        LOGGER.info("\tHTTP {" +
                "ConnectTimeout='" + httpClientOptions.getConnectTimeout() + '\'' +
                ", KeepAlive='" + httpClientOptions.isKeepAlive() + '\'' +
                ", IdleTimeout='" + httpClientOptions.getIdleTimeout() + '\'' +
                ", MaxChunkSize='" + httpClientOptions.getMaxChunkSize() + '\'' +
                ", MaxPoolSize='" + httpClientOptions.getMaxPoolSize() + '\'' +
                ", MaxWaitQueueSize='" + httpClientOptions.getMaxWaitQueueSize() + '\'' +
                ", Pipelining='" + httpClientOptions.isPipelining() + '\'' +
                ", PipeliningLimit='" + httpClientOptions.getPipeliningLimit() + '\'' +
                ", TryUseCompression='" + httpClientOptions.isTryUseCompression() + '\'' +
                '}');

        if (httpClientOptions.isSsl()) {
            LOGGER.info("\tSSL {" +
                    "TrustAll='" + httpClientOptions.isTrustAll() + '\'' +
                    ", VerifyHost='" + httpClientOptions.isVerifyHost() + '\'' +
                    '}');
        }

        if (httpClientOptions.getProxyOptions() != null) {
            LOGGER.info("\tProxy {" +
                    "Type='" + httpClientOptions.getProxyOptions().getType() +
                    ", Host='" + httpClientOptions.getProxyOptions().getHost() + '\'' +
                    ", Port='" + httpClientOptions.getProxyOptions().getPort() + '\'' +
                    ", Username='" + httpClientOptions.getProxyOptions().getUsername() + '\'' +
                    '}');
        }
    }
}
