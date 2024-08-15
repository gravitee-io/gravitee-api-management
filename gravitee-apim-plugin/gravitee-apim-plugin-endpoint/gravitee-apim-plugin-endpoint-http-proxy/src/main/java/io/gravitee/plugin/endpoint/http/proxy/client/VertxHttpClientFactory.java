/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion;
import io.gravitee.node.vertx.client.http.VertxHttpProxyOptions;
import io.gravitee.node.vertx.client.ssl.KeyStore;
import io.gravitee.node.vertx.client.ssl.SslOptions;
import io.gravitee.node.vertx.client.ssl.TrustStore;
import io.gravitee.node.vertx.proxy.VertxProxyOptionsUtils;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * This is a terrible hack that must not be committed!
 */
@Slf4j
@Builder
public class VertxHttpClientFactory {

    public static final int UNSECURE_PORT = 80;
    public static final int SECURE_PORT = 443;

    // Dummy {@link URLStreamHandler} implementation to avoid unknown protocol issue with default implementation (which knows how to handle only http and https protocol).
    public static final URLStreamHandler URL_HANDLER = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u) {
            return null;
        }
    };
    protected static final String HTTP_SSL_OPENSSL_CONFIGURATION = "http.ssl.openssl";

    @NonNull
    private final Vertx vertx;

    @NonNull
    private final Configuration nodeConfiguration;

    private String name;
    private boolean shared;
    private String defaultTarget;
    private VertxHttpProxyOptions proxyOptions;
    private VertxHttpClientOptions httpOptions;
    private SslOptions sslOptions;

    public HttpClient createHttpClient() {
        if (httpOptions == null) {
            httpOptions = new VertxHttpClientOptions();
        }
        return vertx.createHttpClient(createHttpClientOptions());
    }

    public static boolean isSecureProtocol(String protocol) {
        return protocol.charAt(protocol.length() - 1) == 's' && protocol.length() > 2;
    }

    public static URL buildUrl(String uri) {
        try {
            return new URL(null, uri, URL_HANDLER);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Target [" + uri + "] is not valid");
        }
    }

    public static int getPort(URL target, boolean isSecured) {
        final int defaultPort = isSecured ? SECURE_PORT : UNSECURE_PORT;
        return target.getPort() != -1 ? target.getPort() : defaultPort;
    }

    public static String toAbsoluteUri(RequestOptions requestOptions, String defaultHost, int defaultPort) {
        return (
            (Boolean.TRUE.equals(requestOptions.isSsl()) ? "https://" : "http://") +
            (
                (requestOptions.getHost() != null ? requestOptions.getHost() : defaultHost) +
                (requestOptions.getPort() != null ? ":" + requestOptions.getPort() : (defaultPort != -1 ? ":" + defaultPort : "")) +
                requestOptions.getURI()
            )
        );
    }

    private io.vertx.core.http.HttpClientOptions createHttpClientOptions() {
        io.vertx.core.http.HttpClientOptions options = new io.vertx.core.http.HttpClientOptions();

        options
            .setPipelining(httpOptions.isPipelining())
            .setKeepAlive(httpOptions.isKeepAlive())
            .setIdleTimeout((int) (httpOptions.getIdleTimeout() / 1000))
            .setKeepAliveTimeout((int) (httpOptions.getKeepAliveTimeout() / 1000))
            .setConnectTimeout((int) httpOptions.getConnectTimeout())
            .setMaxPoolSize(httpOptions.getMaxConcurrentConnections())
            .setDecompressionSupported(httpOptions.isUseCompression())
            .setTryUsePerFrameWebSocketCompression(httpOptions.isUseCompression())
            .setTryUsePerMessageWebSocketCompression(httpOptions.isUseCompression())
            .setWebSocketCompressionAllowClientNoContext(httpOptions.isUseCompression())
            .setWebSocketCompressionRequestServerNoContext(httpOptions.isUseCompression());

        if (httpOptions.getVersion() == VertxHttpProtocolVersion.HTTP_2) {
            options
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(httpOptions.isClearTextUpgrade())
                .setHttp2MaxPoolSize(httpOptions.getMaxConcurrentConnections())
                .setHttp2MultiplexingLimit(httpOptions.getHttp2MultiplexingLimit());
        }

        final URL target = buildUrl(defaultTarget);

        configureHttpProxy(options);
        configureSsl(options, target);

        if (name != null) {
            options.setName(name);
        }

        return options
            .setShared(shared)
            .setDefaultPort(getPort(target, isSecureProtocol(target.getProtocol())))
            .setDefaultHost(target.getHost());
    }

    private void configureHttpProxy(final io.vertx.core.http.HttpClientOptions options) {
        if (proxyOptions != null && proxyOptions.isEnabled()) {
            if (proxyOptions.isUseSystemProxy()) {
                setSystemProxy(options);
            } else {
                ProxyOptions vertxProxyOptions;
                vertxProxyOptions = new ProxyOptions();
                vertxProxyOptions.setHost(this.proxyOptions.getHost());
                vertxProxyOptions.setPort(this.proxyOptions.getPort());
                vertxProxyOptions.setUsername(this.proxyOptions.getUsername());
                vertxProxyOptions.setPassword(this.proxyOptions.getPassword());
                vertxProxyOptions.setType(ProxyType.valueOf(this.proxyOptions.getType().name()));
                options.setProxyOptions(vertxProxyOptions);
            }
        }
    }

    private void configureSsl(final io.vertx.core.http.HttpClientOptions options, final URL target) {
        if (isSecureProtocol(target.getProtocol())) {
            // Configure SSL.
            options.setSsl(true);

            if (Boolean.TRUE.equals(nodeConfiguration.getProperty(HTTP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false))) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            if (sslOptions != null) {
                options.setVerifyHost(sslOptions.isHostnameVerifier()).setTrustAll(sslOptions.isTrustAll());

                try {
                    // Client truststore configuration (trust server certificate).
                    sslOptions.trustStore().flatMap(TrustStore::trustOptions).ifPresent(options::setTrustOptions);

                    // Client keystore configuration (client certificate for mtls).
                    sslOptions.keyStore().flatMap(KeyStore::keyCertOptions).ifPresent(options::setKeyCertOptions);
                } catch (KeyStore.KeyStoreCertOptionsException | TrustStore.TrustOptionsException e) {
                    throw new IllegalArgumentException(e.getMessage() + " for " + target);
                }
            }
        }

        options.setUseAlpn(true);
    }

    private void setSystemProxy(final io.vertx.core.http.HttpClientOptions options) {
        try {
            options.setProxyOptions(VertxProxyOptionsUtils.buildProxyOptions(nodeConfiguration));
        } catch (Exception e) {
            log.warn(
                "HttpClient (name[{}] target[{}]) requires a system proxy to be defined but some configurations are missing or not well defined: {}",
                name,
                defaultTarget,
                e.getMessage()
            );
            log.warn("Ignoring system proxy");
        }
    }
}
