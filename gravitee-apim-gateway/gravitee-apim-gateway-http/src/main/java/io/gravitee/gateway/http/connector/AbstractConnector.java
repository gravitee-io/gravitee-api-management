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

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.gateway.api.Connector;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.*;
import io.vertx.core.tracing.TracingPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractConnector<T extends HttpEndpoint> extends AbstractLifecycleComponent<Connector> implements Connector {

    private static final String URI_PARAM_SEPARATOR = "&";
    private static final char URI_PARAM_SEPARATOR_CHAR = '&';
    private static final char URI_PARAM_VALUE_SEPARATOR_CHAR = '=';
    private static final char URI_QUERY_DELIMITER_CHAR = '?';
    private static final CharSequence URI_QUERY_DELIMITER_CHAR_SEQUENCE = "?";

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractConnector.class);

    protected static final int UNSECURE_PORT = 80;
    protected static final int SECURE_PORT = 443;

    @Autowired
    private Vertx vertx;

    @Autowired
    private Environment environment;

    protected final T endpoint;

    private HttpClientOptions options;

    /**
     * Dummy {@link URLStreamHandler} implementation to avoid unknown protocol issue with default implementation
     * (which knows how to handle only http and https protocol).
     */
    private final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) {
            return null;
        }
    };

    @Autowired
    public AbstractConnector(T endpoint) {
        this.endpoint = endpoint;
    }

    private final Map<Thread, HttpClient> httpClients = new ConcurrentHashMap<>();

    private final AtomicInteger requestTracker = new AtomicInteger(0);

    @Override
    public void request(ProxyRequest proxyRequest, Handler<ProxyConnection> proxyConnectionHandler) {
        // For Vertx HTTP client query parameters have to be passed along the URI
        final String uri = appendQueryParameters(proxyRequest.uri(), proxyRequest.parameters());

        // Add the endpoint reference in metrics to know which endpoint has been invoked while serving the request
        proxyRequest.metrics().setEndpoint(uri);

        try {
            final URL url = new URL(null, uri, URL_HANDLER);

            final String protocol = url.getProtocol();

            final int port = url.getPort() != -1
                ? url.getPort()
                : protocol.charAt(protocol.length() - 1) == 's' ? SECURE_PORT : UNSECURE_PORT;

            final String host = (port == UNSECURE_PORT || port == SECURE_PORT) ? url.getHost() : url.getHost() + ':' + port;

            convertHeadersForHttpVersion(url, proxyRequest, host);

            // Enhance proxy request with endpoint configuration
            if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
                endpoint.getHeaders().forEach(proxyRequest.headers()::set);
            }

            // Create the connector to the upstream
            final AbstractHttpProxyConnection connection = create(proxyRequest);

            // Grab an instance of the HTTP client
            final HttpClient client = httpClients.computeIfAbsent(Thread.currentThread(), createHttpClient());

            requestTracker.incrementAndGet();

            // Connect to the upstream
            connection.connect(
                client,
                port,
                url.getHost(),
                (url.getQuery() == null) ? url.getPath() : url.getPath() + URI_QUERY_DELIMITER_CHAR + url.getQuery(),
                connect -> proxyConnectionHandler.handle(connection),
                result -> requestTracker.decrementAndGet()
            );
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException();
        }
    }

    protected abstract void convertHeadersForHttpVersion(URL url, ProxyRequest request, String host);

    protected abstract AbstractHttpProxyConnection create(ProxyRequest proxyRequest);

    @Override
    protected void doStart() throws Exception {
        this.options = this.getOptions();
        printHttpClientConfiguration();
    }

    private String appendQueryParameters(String uri, MultiValueMap<String, String> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            StringJoiner parametersAsString = new StringJoiner(URI_PARAM_SEPARATOR);
            parameters.forEach(
                (paramName, paramValues) -> {
                    if (paramValues != null) {
                        for (String paramValue : paramValues) {
                            if (paramValue == null) {
                                parametersAsString.add(paramName);
                            } else {
                                parametersAsString.add(paramName + URI_PARAM_VALUE_SEPARATOR_CHAR + paramValue);
                            }
                        }
                    }
                }
            );

            if (uri.contains(URI_QUERY_DELIMITER_CHAR_SEQUENCE)) {
                return uri + URI_PARAM_SEPARATOR_CHAR + parametersAsString;
            } else {
                return uri + URI_QUERY_DELIMITER_CHAR + parametersAsString;
            }
        } else {
            return uri;
        }
    }

    protected HttpClientOptions getOptions() throws EndpointException {
        HttpClientOptions options = new HttpClientOptions();

        options.setTracingPolicy(TracingPolicy.ALWAYS);
        options.setPipelining(endpoint.getHttpClientOptions().isPipelining());
        options.setKeepAlive(endpoint.getHttpClientOptions().isKeepAlive());
        options.setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000));
        options.setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout());
        options.setMaxPoolSize(endpoint.getHttpClientOptions().getMaxConcurrentConnections());
        options.setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());

        if (endpoint.getHttpClientOptions().getVersion() == ProtocolVersion.HTTP_2) {
            options.setProtocolVersion(HttpVersion.HTTP_2);
            options.setHttp2ClearTextUpgrade(endpoint.getHttpClientOptions().isClearTextUpgrade());
            options.setHttp2MaxPoolSize(endpoint.getHttpClientOptions().getMaxConcurrentConnections());
        }

        URL target;
        try {
            target = new URL(null, endpoint.getTarget(), URL_HANDLER);
        } catch (MalformedURLException e) {
            throw new EndpointException("Endpoint target is not valid " + endpoint.getTarget());
        }

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
            options.setProxyOptions(proxyOptions);
        }

        HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();

        final String protocol = target.getProtocol();

        if (protocol.charAt(protocol.length() - 1) == 's') {
            // Configure SSL
            options.setSsl(true).setUseAlpn(true);

            if (environment.getProperty("http.ssl.openssl", Boolean.class, false)) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            if (sslOptions != null) {
                options.setVerifyHost(sslOptions.isHostnameVerifier()).setTrustAll(sslOptions.isTrustAll());

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
                            options.setPemTrustOptions(pemTrustOptions);
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
                            options.setPfxTrustOptions(pfxOptions);
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
                            options.setTrustStoreOptions(jksOptions);
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
                            options.setPemKeyCertOptions(pemKeyCertOptions);
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
                            options.setPfxKeyCertOptions(pfxOptions);
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
                            options.setKeyStoreOptions(jksOptions);
                            break;
                    }
                }
            }
        }

        return options;
    }

    @Override
    protected void doStop() throws Exception {
        LOGGER.debug(
            "Graceful shutdown of HTTP Client for endpoint[{}] target[{}] requests[{}]",
            endpoint.getName(),
            endpoint.getTarget(),
            requestTracker.get()
        );
        long shouldEndAt = System.currentTimeMillis() + endpoint.getHttpClientOptions().getReadTimeout();

        while (requestTracker.get() > 0 && System.currentTimeMillis() <= shouldEndAt) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        if (requestTracker.get() > 0) {
            LOGGER.warn("Cancel requests[{}] for endpoint[{}] target[{}]", requestTracker.get(), endpoint.getName(), endpoint.getTarget());
        }

        httpClients
            .values()
            .forEach(
                httpClient -> {
                    try {
                        httpClient.close();
                    } catch (IllegalStateException ise) {
                        LOGGER.warn(ise.getMessage());
                    }
                }
            );
    }

    private Function<Thread, HttpClient> createHttpClient() {
        return thread -> vertx.createHttpClient(options);
    }

    private void printHttpClientConfiguration() {
        LOGGER.debug("Create HTTP connector with configuration: ");
        LOGGER.debug(
            "\t" +
            options.getProtocolVersion() +
            " {" +
            "ConnectTimeout='" +
            options.getConnectTimeout() +
            '\'' +
            ", KeepAlive='" +
            options.isKeepAlive() +
            '\'' +
            ", IdleTimeout='" +
            options.getIdleTimeout() +
            '\'' +
            ", MaxChunkSize='" +
            options.getMaxChunkSize() +
            '\'' +
            ", MaxPoolSize='" +
            options.getMaxPoolSize() +
            '\'' +
            ", MaxWaitQueueSize='" +
            options.getMaxWaitQueueSize() +
            '\'' +
            ", Pipelining='" +
            options.isPipelining() +
            '\'' +
            ", PipeliningLimit='" +
            options.getPipeliningLimit() +
            '\'' +
            ", TryUseCompression='" +
            options.isTryUseCompression() +
            '\'' +
            '}'
        );

        if (options.isSsl()) {
            LOGGER.debug("\tSSL {" + "TrustAll='" + options.isTrustAll() + '\'' + ", VerifyHost='" + options.isVerifyHost() + '\'' + '}');
        }

        if (options.getProxyOptions() != null) {
            LOGGER.debug(
                "\tProxy {" +
                "Type='" +
                options.getProxyOptions().getType() +
                ", Host='" +
                options.getProxyOptions().getHost() +
                '\'' +
                ", Port='" +
                options.getProxyOptions().getPort() +
                '\'' +
                '}'
            );
        }
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
            LOGGER.warn(
                "An api endpoint (name[{}] type[{}] target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}",
                endpoint.getName(),
                endpoint.getType(),
                endpoint.getTarget(),
                errors
            );
            LOGGER.warn("Ignoring system proxy");
            return null;
        }
    }
}
