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
package io.gravitee.gateway.http.connector;

import com.google.common.net.UrlEscapers;
import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.api.proxy.ws.WebSocketProxyRequest;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.core.proxy.EmptyProxyResponse;
import io.gravitee.gateway.core.proxy.ws.SwitchProtocolProxyResponse;
import io.gravitee.gateway.http.connector.ws.VertxWebSocketFrame;
import io.gravitee.gateway.http.connector.ws.VertxWebSocketProxyConnection;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpClient extends AbstractLifecycleComponent<Connector> implements Connector {

    private final Logger LOGGER = LoggerFactory.getLogger(VertxHttpClient.class);

    private static final String HTTPS_SCHEME = "https";
    private static final String WSS_SCHEME = "wss";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private static final String URI_PATH_SEPARATOR = "/";
    private static final char URI_PATH_SEPARATOR_CHAR = '/';
    private static final Set<CharSequence> HOP_HEADERS;

    private static final Set<CharSequence> WS_HOP_HEADERS;

    static {
        Set<CharSequence> hopHeaders = new HashSet<>();
        Set<CharSequence> wsHopHeaders = new HashSet<>();

        // Hop-by-hop headers
        hopHeaders.add(HttpHeaderNames.CONNECTION);
        hopHeaders.add(HttpHeaderNames.KEEP_ALIVE);
        hopHeaders.add(HttpHeaderNames.PROXY_AUTHORIZATION);
        hopHeaders.add(HttpHeaderNames.PROXY_AUTHENTICATE);
        hopHeaders.add(HttpHeaderNames.PROXY_CONNECTION);
        hopHeaders.add(HttpHeaderNames.TE);
        hopHeaders.add(HttpHeaderNames.TRAILER);
        hopHeaders.add(HttpHeaderNames.UPGRADE);

        // Hop-by-hop headers Websocket
        wsHopHeaders.add(HttpHeaderNames.KEEP_ALIVE);
        wsHopHeaders.add(HttpHeaderNames.PROXY_AUTHORIZATION);
        wsHopHeaders.add(HttpHeaderNames.PROXY_AUTHENTICATE);
        wsHopHeaders.add(HttpHeaderNames.PROXY_CONNECTION);
        wsHopHeaders.add(HttpHeaderNames.TE);
        wsHopHeaders.add(HttpHeaderNames.TRAILER);

        HOP_HEADERS = Collections.unmodifiableSet(hopHeaders);
        WS_HOP_HEADERS = Collections.unmodifiableSet(wsHopHeaders);
    }

    @Autowired
    private Vertx vertx;

    @Autowired
    private Environment environment;

    private final HttpEndpoint endpoint;

    private HttpClientOptions httpClientOptions;

    private AtomicInteger runningRequests = new AtomicInteger(0);

    @Autowired
    public VertxHttpClient(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private final Map<Context, HttpClient> httpClients = new ConcurrentHashMap<>();

    @Override
    public ProxyConnection request(ProxyRequest proxyRequest) {
        HttpClient httpClient = httpClients.computeIfAbsent(Vertx.currentContext(), createHttpClient());

        runningRequests.incrementAndGet();

        // Remove hop-by-hop headers.
        if (! proxyRequest.isWebSocket()) {
            for (CharSequence header : HOP_HEADERS) {
                proxyRequest.headers().remove(header);
            }
        } else {
            for (CharSequence header : WS_HOP_HEADERS) {
                proxyRequest.headers().remove(header);
            }
        }

        final URI uri ;
        if (endpoint.getHttpClientOptions().isEncodeURI()) {
            uri = getEncodedURI(proxyRequest.uri());
        }else {
            uri = proxyRequest.uri();
        }
        final int port = uri.getPort() != -1 ? uri.getPort() :
                (HTTPS_SCHEME.equals(uri.getScheme()) || WSS_SCHEME.equals(uri.getScheme()) ? 443 : 80);

        final String host = (port == DEFAULT_HTTP_PORT || port == DEFAULT_HTTPS_PORT) ?
                uri.getHost() : uri.getHost() + ':' + port;

        proxyRequest.headers().set(HttpHeaders.HOST, host);

        // Apply headers from endpoint
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            endpoint.getHeaders().forEach(proxyRequest.headers()::set);
        }

        String relativeUri = (uri.getRawQuery() == null) ? uri.getRawPath() : uri.getRawPath() + '?' + uri.getRawQuery();

        // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
        proxyRequest.metrics().setEndpoint(uri.toString());

        if (proxyRequest.isWebSocket()) {
            VertxWebSocketProxyConnection webSocketProxyConnection = new VertxWebSocketProxyConnection();
            WebSocketProxyRequest wsProxyRequest = (WebSocketProxyRequest) proxyRequest;

            httpClient.websocket(port, uri.getHost(), relativeUri, new Handler<WebSocket>() {
                @Override
                public void handle(WebSocket event) {
                    // The client -> gateway connection must be upgraded now that the one between gateway -> upstream
                    // has been accepted
                    wsProxyRequest.upgrade();

                    // From server to client
                    wsProxyRequest.frameHandler(frame -> {
                        if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.BINARY) {
                            event.writeBinaryMessage(io.vertx.core.buffer.Buffer.buffer(frame.data().getBytes()));
                        } else if (frame.type() == io.gravitee.gateway.api.ws.WebSocketFrame.Type.TEXT) {
                            event.writeTextMessage(frame.data().toString());
                        }
                    });

                    wsProxyRequest.closeHandler(result -> event.close());

                    // From client to server
                    event.frameHandler(frame -> wsProxyRequest.write(new VertxWebSocketFrame(frame)));

                    event.closeHandler(event1 -> {
                        wsProxyRequest.close();
                        runningRequests.decrementAndGet();
                    });

                    event.exceptionHandler(new Handler<Throwable>() {
                        @Override
                        public void handle(Throwable throwable) {
                            wsProxyRequest.reject(HttpStatusCode.BAD_REQUEST_400);
                            ProxyResponse clientResponse = new EmptyProxyResponse(HttpStatusCode.BAD_REQUEST_400);

                            clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                            webSocketProxyConnection.handleResponse(clientResponse);

                            runningRequests.decrementAndGet();
                        }
                    });

                    // Tell the reactor that the request has been handled by the HTTP client
                    webSocketProxyConnection.handleResponse(new SwitchProtocolProxyResponse());
                }
            }, throwable -> {
                if (throwable instanceof WebsocketRejectedException) {
                    wsProxyRequest.reject(((WebsocketRejectedException) throwable).getStatus());
                    ProxyResponse clientResponse = new EmptyProxyResponse(((WebsocketRejectedException) throwable).getStatus());

                    clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                    webSocketProxyConnection.handleResponse(clientResponse);
                } else {
                    wsProxyRequest.reject(HttpStatusCode.BAD_GATEWAY_502);
                    ProxyResponse clientResponse = new EmptyProxyResponse(HttpStatusCode.BAD_GATEWAY_502);

                    clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                    webSocketProxyConnection.handleResponse(clientResponse);
                }

                runningRequests.decrementAndGet();
            });

            return webSocketProxyConnection;
        } else {
            // Prepare HTTP request
            HttpClientRequest clientRequest = httpClient.request(
                    HttpMethod.valueOf(proxyRequest.method().name()), port, uri.getHost(), relativeUri);
            clientRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());
            clientRequest.setFollowRedirects(endpoint.getHttpClientOptions().isFollowRedirects());

            if (proxyRequest.method() == io.gravitee.common.http.HttpMethod.OTHER) {
                clientRequest.setRawMethod(proxyRequest.rawMethod());
            }

            VertxProxyConnection proxyConnection = new VertxProxyConnection(proxyRequest, clientRequest);
            clientRequest.handler(clientResponse -> handleClientResponse(proxyConnection, clientResponse, clientRequest));

            clientRequest.connectionHandler(connection -> {
                connection.exceptionHandler(ex -> {
                    // I don't want to fill my logs with error
                });
            });

            clientRequest.exceptionHandler(event -> {
                if (!proxyConnection.isCanceled() && !proxyConnection.isTransmitted()) {
                    proxyRequest.metrics().setMessage(event.getMessage());

                    if (proxyConnection.timeoutHandler() != null
                            && (event instanceof ConnectException ||
                            event instanceof TimeoutException ||
                            event instanceof NoRouteToHostException ||
                            event instanceof UnknownHostException)) {
                        proxyConnection.handleConnectTimeout(event);
                    } else {
                        ProxyResponse clientResponse = new EmptyProxyResponse(
                                ((event instanceof ConnectTimeoutException) || (event instanceof TimeoutException)) ?
                                        HttpStatusCode.GATEWAY_TIMEOUT_504 : HttpStatusCode.BAD_GATEWAY_502);

                        clientResponse.headers().set(HttpHeaders.CONNECTION, HttpHeadersValues.CONNECTION_CLOSE);
                        proxyConnection.handleResponse(clientResponse);
                    }
                }

                runningRequests.decrementAndGet();
            });

            return proxyConnection;
        }
    }

    private void handleClientResponse(final VertxProxyConnection proxyConnection,
                                      final HttpClientResponse clientResponse, final HttpClientRequest clientRequest) {
        VertxProxyResponse proxyClientResponse = new VertxProxyResponse(clientResponse);
        proxyConnection.setProxyResponse(proxyClientResponse);

        // Copy HTTP headers
        clientResponse.headers().names().forEach(headerName ->
                proxyClientResponse.headers().put(headerName, clientResponse.headers().getAll(headerName)));

        proxyClientResponse.pause();

        // Copy body content
        clientResponse.handler(event -> proxyClientResponse.bodyHandler().handle(Buffer.buffer(event.getBytes())));

        // Signal end of the response
        clientResponse.endHandler(v -> {
            proxyClientResponse.endHandler().handle(null);
            runningRequests.decrementAndGet();
        });

        clientResponse.exceptionHandler(throwable -> {
            LOGGER.error("Unexpected error while handling backend response for request {} {} - {}",
                    clientRequest.method(), clientRequest.absoluteURI(), throwable.getMessage());
            proxyClientResponse.endHandler().handle(null);
            runningRequests.decrementAndGet();
        });

        proxyConnection.handleResponse(proxyClientResponse);
    }

    @Override
    protected void doStart() throws Exception {
        httpClientOptions = new HttpClientOptions();

        httpClientOptions.setPipelining(endpoint.getHttpClientOptions().isPipelining());
        httpClientOptions.setKeepAlive(endpoint.getHttpClientOptions().isKeepAlive());
        httpClientOptions.setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000));
        httpClientOptions.setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout());
        httpClientOptions.setUsePooledBuffers(true);
        httpClientOptions.setMaxPoolSize(endpoint.getHttpClientOptions().getMaxConcurrentConnections());
        httpClientOptions.setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());

        URI target = URI.create(endpoint.getTarget());

        // Configure proxy
        HttpProxy proxy = endpoint.getHttpProxy();
        if (proxy != null && proxy.isEnabled()) {
            ProxyOptions proxyOptions;

            if (proxy.isUseSystemProxy()) {
                proxyOptions = getSystemProxyOptions();
            } else {
                proxyOptions = new ProxyOptions();
                proxyOptions.setHost(proxy.getHost());
                proxyOptions.setPort(proxy.getPort());
                proxyOptions.setUsername(proxy.getUsername());
                proxyOptions.setPassword(proxy.getPassword());
                proxyOptions.setType(ProxyType.valueOf(proxy.getType().name()));

            }
            httpClientOptions.setProxyOptions(proxyOptions);
        }

        HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();

        if (HTTPS_SCHEME.equalsIgnoreCase(target.getScheme()) || WSS_SCHEME.equalsIgnoreCase(target.getScheme())) {
            // Configure SSL
            httpClientOptions.setSsl(true);

            if (sslOptions != null) {
                httpClientOptions
                        .setVerifyHost(sslOptions.isHostnameVerifier())
                        .setTrustAll(sslOptions.isTrustAll());

                // Client trust configuration
                if (!sslOptions.isTrustAll() && sslOptions.getTrustStore() != null) {
                    switch (sslOptions.getTrustStore().getType()) {
                        case PEM:
                            PEMTrustStore pemTrustStore = (PEMTrustStore) sslOptions.getTrustStore();
                            PemTrustOptions pemTrustOptions = new PemTrustOptions();
                            if (pemTrustStore.getPath() != null && !pemTrustStore.getPath().isEmpty()) {
                                pemTrustOptions.addCertPath(pemTrustStore.getPath());
                            } else if (pemTrustStore.getContent() != null && !pemTrustStore.getContent().isEmpty()) {
                                pemTrustOptions.addCertValue(io.vertx.core.buffer.Buffer.buffer(pemTrustStore.getContent()));
                            } else {
                                throw new EndpointException("Missing PEM certificate value for endpoint " + endpoint.getName());
                            }
                            this.httpClientOptions.setPemTrustOptions(pemTrustOptions);
                            break;
                        case PKCS12:
                            PKCS12TrustStore pkcs12TrustStore = (PKCS12TrustStore) sslOptions.getTrustStore();
                            PfxOptions pfxOptions = new PfxOptions();
                            pfxOptions.setPassword(pkcs12TrustStore.getPassword());
                            if (pkcs12TrustStore.getPath() != null && !pkcs12TrustStore.getPath().isEmpty()) {
                                pfxOptions.setPath(pkcs12TrustStore.getPath());
                            } else if (pkcs12TrustStore.getContent() != null && !pkcs12TrustStore.getContent().isEmpty()) {
                                pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(pkcs12TrustStore.getContent()));
                            } else {
                                throw new EndpointException("Missing PKCS12 value for endpoint " + endpoint.getName());
                            }
                            this.httpClientOptions.setPfxTrustOptions(pfxOptions);
                            break;
                        case JKS:
                            JKSTrustStore jksTrustStore = (JKSTrustStore) sslOptions.getTrustStore();
                            JksOptions jksOptions = new JksOptions();
                            jksOptions.setPassword(jksTrustStore.getPassword());
                            if (jksTrustStore.getPath() != null && !jksTrustStore.getPath().isEmpty()) {
                                jksOptions.setPath(jksTrustStore.getPath());
                            } else if (jksTrustStore.getContent() != null && !jksTrustStore.getContent().isEmpty()) {
                                jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(jksTrustStore.getContent()));
                            } else {
                                throw new EndpointException("Missing JKS value for endpoint " + endpoint.getName());
                            }
                            this.httpClientOptions.setTrustStoreOptions(jksOptions);
                            break;
                    }
                }

                // Client authentication configuration
                if (sslOptions.getKeyStore() != null) {
                    switch (sslOptions.getKeyStore().getType()) {
                        case PEM:
                            PEMKeyStore pemKeyStore = (PEMKeyStore) sslOptions.getKeyStore();
                            PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
                            if (pemKeyStore.getCertPath() != null && !pemKeyStore.getCertPath().isEmpty()) {
                                pemKeyCertOptions.setCertPath(pemKeyStore.getCertPath());
                            } else if (pemKeyStore.getCertContent() != null && !pemKeyStore.getCertContent().isEmpty()) {
                                pemKeyCertOptions.setCertValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getCertContent()));
                            }
                            if (pemKeyStore.getKeyPath() != null && !pemKeyStore.getKeyPath().isEmpty()) {
                                pemKeyCertOptions.setKeyPath(pemKeyStore.getKeyPath());
                            } else if (pemKeyStore.getKeyContent() != null && !pemKeyStore.getKeyContent().isEmpty()) {
                                pemKeyCertOptions.setKeyValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getKeyContent()));
                            }
                            this.httpClientOptions.setPemKeyCertOptions(pemKeyCertOptions);
                            break;
                        case PKCS12:
                            PKCS12KeyStore pkcs12KeyStore = (PKCS12KeyStore) sslOptions.getKeyStore();
                            PfxOptions pfxOptions = new PfxOptions();
                            pfxOptions.setPassword(pkcs12KeyStore.getPassword());
                            if (pkcs12KeyStore.getPath() != null && !pkcs12KeyStore.getPath().isEmpty()) {
                                pfxOptions.setPath(pkcs12KeyStore.getPath());
                            } else if (pkcs12KeyStore.getContent() != null && !pkcs12KeyStore.getContent().isEmpty()) {
                                pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(pkcs12KeyStore.getContent()));
                            }
                            this.httpClientOptions.setPfxKeyCertOptions(pfxOptions);
                            break;
                        case JKS:
                            JKSKeyStore jksKeyStore = (JKSKeyStore) sslOptions.getKeyStore();
                            JksOptions jksOptions = new JksOptions();
                            jksOptions.setPassword(jksKeyStore.getPassword());
                            if (jksKeyStore.getPath() != null && !jksKeyStore.getPath().isEmpty()) {
                                jksOptions.setPath(jksKeyStore.getPath());
                            } else if (jksKeyStore.getContent() != null && !jksKeyStore.getContent().isEmpty()) {
                                jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(jksKeyStore.getContent()));
                            }
                            this.httpClientOptions.setKeyStoreOptions(jksOptions);
                            break;
                    }
                }
            }
        }

        printHttpClientConfiguration(httpClientOptions);
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.info("Graceful shutdown of HTTP Client for endpoint[{}] target[{}] requests[{}]", endpoint.getName(), endpoint.getTarget(), runningRequests.get());
        long shouldEndAt = System.currentTimeMillis() + endpoint.getHttpClientOptions().getReadTimeout();

        while (runningRequests.get() != 0 && System.currentTimeMillis() <= shouldEndAt) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        if (runningRequests.get() > 0) {
            LOGGER.warn("Cancel requests[{}] for endpoint[{}] target[{}]", runningRequests.get(), endpoint.getName(), endpoint.getTarget());
        }

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

    private URI getEncodedURI(URI uri) {

        // Path segments must be encoded to avoid bad URI syntax
        String [] segments = uri.getRawPath().split(URI_PATH_SEPARATOR);
        StringJoiner pathBuilder = new StringJoiner(URI_PATH_SEPARATOR);

        for(String pathSeg : segments) {
            pathBuilder.add(UrlEscapers.urlPathSegmentEscaper().escape(pathSeg));
        }

        String encodedPath = pathBuilder.toString();
        String encodedQuery = null;
        if(uri.getRawQuery() != null) {
            StringJoiner queryBuilder = new StringJoiner("&");
            String query = uri.getRawQuery();
            //EndpointInvoker.buildURI use only the '&' as a query parameter separator. No need to test ';'
            String[] params = query.split("&");
            for (String param: params) {
                try {
                    int equalIndex = param.indexOf('=');
                    if (equalIndex >= 0) {
                        String encodeKey = URLEncoder.encode(param.substring(0, equalIndex), Charset.defaultCharset().name());
                        String encodeValue = URLEncoder.encode(param.substring(equalIndex+1), Charset.defaultCharset().name());
                        queryBuilder.add(encodeKey + '=' + encodeValue);

                    } else {
                        queryBuilder.add(URLEncoder.encode(param, Charset.defaultCharset().name()));
                    }
                } catch (UnsupportedEncodingException e) {
                    LOGGER.error("An error occurs when trying to encode " + param + ". Add the unencoded value.", e);
                    queryBuilder.add(param);
                }
            }
            encodedQuery = queryBuilder.toString();
        }

        return URI.create(
                uri.getScheme() + "://" +
                        uri.getHost()+
                        (uri.getPort()==-1?"":(':' + uri.getPort()))+
                        encodedPath+
                        ((encodedQuery==null) ? "" : '?' + encodedQuery));
    }

    private ProxyOptions getSystemProxyOptions() {

        StringBuilder errors = new StringBuilder();
        ProxyOptions proxyOptions = new ProxyOptions();

        // System proxy must be well configured. Check that this is the case.
        if (environment.containsProperty("system.proxy.host")) {
            proxyOptions.setHost(environment.getProperty("system.proxy.host"));
        } else {
            errors.append("'system.proxy.host' ");
        }

        try {
            proxyOptions.setPort(Integer.parseInt(Objects.requireNonNull(environment.getProperty("system.proxy.port"))));
        } catch (Exception e) {
            errors.append("'system.proxy.port' [").append(environment.getProperty("system.proxy.port")).append("] ");
        }

        try {
            proxyOptions.setType(ProxyType.valueOf(environment.getProperty("system.proxy.type")));
        } catch (Exception e) {
            errors.append("'system.proxy.type' [").append(environment.getProperty("system.proxy.type")).append("] ");
        }

        proxyOptions.setUsername(environment.getProperty("system.proxy.username"));
        proxyOptions.setPassword(environment.getProperty("system.proxy.password"));

        if (errors.length() == 0) {
            return proxyOptions;
        } else {
            LOGGER.warn("An api endpoint (name[{}] type[{}] target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}", endpoint.getName(), endpoint.getType(), endpoint.getTarget(), errors);
            LOGGER.warn("Ignoring system proxy");
            return null;
        }
    }
}
