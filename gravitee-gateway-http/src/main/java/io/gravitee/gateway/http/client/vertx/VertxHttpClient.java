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
package io.gravitee.gateway.http.client.vertx;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.endpoint.Endpoint;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
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
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpClient extends AbstractLifecycleComponent<Connector> implements Connector {

    private final Logger LOGGER = LoggerFactory.getLogger(VertxHttpClient.class);

    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final Set<String> HOP_HEADERS;

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

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
    }

    @Resource
    private Vertx vertx;

    private final HttpEndpoint endpoint;

    private HttpClientOptions httpClientOptions;

    @Autowired
    public VertxHttpClient(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private final Map<Context, HttpClient> httpClients = new HashMap<>();

    @Override
    public ProxyConnection request(ProxyRequest proxyRequest) {
        HttpClient httpClient = httpClients.computeIfAbsent(Vertx.currentContext(), createHttpClient());

        // Remove hop-by-hop headers.
        for (String header : HOP_HEADERS) {
            proxyRequest.headers().remove(header);
        }

        final URI uri = proxyRequest.uri();
        final int port = uri.getPort() != -1 ? uri.getPort() :
                (HTTPS_SCHEME.equals(uri.getScheme()) ? 443 : 80);

        // Override with default headers defined for endpoint
        if (endpoint.getHostHeader() != null && !endpoint.getHostHeader().isEmpty()) {
            proxyRequest.headers().set(HttpHeaders.HOST, endpoint.getHostHeader());
        } else {
            final String host = (port == DEFAULT_HTTP_PORT || port == DEFAULT_HTTPS_PORT) ?
                    uri.getHost() : uri.getHost() + ':' + port;

            proxyRequest.headers().set(HttpHeaders.HOST, host);
        }

        String relativeUri = (uri.getRawQuery() == null) ? uri.getRawPath() : uri.getRawPath() + '?' + uri.getRawQuery();

        // Prepare request
        HttpClientRequest clientRequest = httpClient.request(
                convert(proxyRequest.method()), port, uri.getHost(), relativeUri);
        clientRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());
        clientRequest.setFollowRedirects(endpoint.getHttpClientOptions().isFollowRedirects());

        if (proxyRequest.method() == io.gravitee.common.http.HttpMethod.OTHER) {
            clientRequest.setRawMethod(proxyRequest.rawMethod());
        }

        VertxProxyConnection proxyConnection = new VertxProxyConnection(proxyRequest, clientRequest);
        clientRequest.handler(clientResponse -> handleClientResponse(proxyConnection, clientResponse));

        clientRequest.connectionHandler(connection -> {
            connection.exceptionHandler(ex -> {
                // I don't want to fill my logs with error
            });
        });

        clientRequest.exceptionHandler(event -> {
            if (! proxyConnection.isCanceled() && ! proxyConnection.isTransmitted()) {
                proxyRequest.metrics().setMessage(event.getMessage());

                if (proxyConnection.timeoutHandler() != null
                        && (event instanceof ConnectException ||
                        event instanceof TimeoutException ||
                        event instanceof NoRouteToHostException ||
                        event instanceof UnknownHostException)) {
                    proxyConnection.handleConnectTimeout(event);
                } else {
                    VertxProxyResponse clientResponse = new VertxProxyResponse(
                            ((event instanceof ConnectTimeoutException) || (event instanceof TimeoutException)) ?
                                    HttpStatusCode.GATEWAY_TIMEOUT_504 : HttpStatusCode.BAD_GATEWAY_502);

                    clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                    proxyConnection.handleResponse(clientResponse);
                }
            }
        });

        return proxyConnection;
    }

    private void handleClientResponse(VertxProxyConnection proxyConnection, HttpClientResponse clientResponse) {
        VertxProxyResponse proxyClientResponse = new VertxProxyResponse(clientResponse);
        proxyConnection.setProxyResponse(proxyClientResponse);

        // Copy HTTP headers
        clientResponse.headers().names().forEach(headerName ->
                proxyClientResponse.headers().put(headerName, clientResponse.headers().getAll(headerName)));

        proxyClientResponse.pause();

        // Copy body content
        clientResponse.handler(event -> proxyClientResponse.bodyHandler().handle(Buffer.buffer(event.getBytes())));

        // Signal end of the response
        clientResponse.endHandler(v -> proxyClientResponse.endHandler().handle(null));

        proxyConnection.handleResponse(proxyClientResponse);
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
            case OTHER:
                return HttpMethod.OTHER;
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

        httpClients.values().forEach(httpClient -> {
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
