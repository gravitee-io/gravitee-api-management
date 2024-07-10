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
package io.gravitee.gateway.jupiter.http.vertx.client;

import io.gravitee.common.util.VertxProxyOptionsUtils;
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.definition.model.v4.http.ProtocolVersion;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.definition.model.v4.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.v4.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.v4.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.v4.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.node.api.configuration.Configuration;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.*;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Base64;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class VertxHttpClient {

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
    private HttpProxyOptions proxyOptions;
    private HttpClientOptions httpOptions;
    private SslOptions sslOptions;

    public HttpClient createHttpClient() {
        if (httpOptions == null) {
            httpOptions = new HttpClientOptions();
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

    private io.vertx.core.http.HttpClientOptions createHttpClientOptions() {
        io.vertx.core.http.HttpClientOptions options = new io.vertx.core.http.HttpClientOptions();

        options
            .setPipelining(httpOptions.isPipelining())
            .setKeepAlive(httpOptions.isKeepAlive())
            .setIdleTimeout((int) (httpOptions.getIdleTimeout() / 1000))
            .setKeepAliveTimeout((int) (httpOptions.getKeepAliveTimeout() / 1000))
            .setConnectTimeout((int) httpOptions.getConnectTimeout())
            .setMaxPoolSize(httpOptions.getMaxConcurrentConnections())
            .setTryUseCompression(httpOptions.isUseCompression());

        if (httpOptions.getVersion() == ProtocolVersion.HTTP_2) {
            options
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(httpOptions.isClearTextUpgrade())
                .setHttp2MaxPoolSize(httpOptions.getMaxConcurrentConnections());
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
                ProxyOptions proxyOptions;
                proxyOptions = new ProxyOptions();
                proxyOptions.setHost(this.proxyOptions.getHost());
                proxyOptions.setPort(this.proxyOptions.getPort());
                proxyOptions.setUsername(this.proxyOptions.getUsername());
                proxyOptions.setPassword(this.proxyOptions.getPassword());
                proxyOptions.setType(ProxyType.valueOf(this.proxyOptions.getType().name()));
                options.setProxyOptions(proxyOptions);
            }
        }
    }

    private void configureSsl(final io.vertx.core.http.HttpClientOptions options, final URL target) {
        if (isSecureProtocol(target.getProtocol())) {
            // Configure SSL.
            options.setSsl(true).setUseAlpn(true);

            if (nodeConfiguration.getProperty(HTTP_SSL_OPENSSL_CONFIGURATION, Boolean.class, false)) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            if (sslOptions != null) {
                options.setVerifyHost(sslOptions.isHostnameVerifier()).setTrustAll(sslOptions.isTrustAll());

                // Client truststore configuration (trust server certificate).
                configureTrustStore(options);

                // Client keystore configuration (client certificate for mtls).
                configureKeyStore(options);
            }
        }
    }

    private void configureTrustStore(final io.vertx.core.http.HttpClientOptions options) {
        if (!sslOptions.isTrustAll() && sslOptions.getTrustStore() != null) {
            switch (sslOptions.getTrustStore().getType()) {
                case PEM:
                    final PEMTrustStore pemTrustStore = (PEMTrustStore) sslOptions.getTrustStore();
                    final PemTrustOptions pemTrustOptions = new PemTrustOptions();

                    if (pemTrustStore.getPath() != null && !pemTrustStore.getPath().isEmpty()) {
                        pemTrustOptions.addCertPath(pemTrustStore.getPath());
                    } else if (pemTrustStore.getContent() != null && !pemTrustStore.getContent().isEmpty()) {
                        pemTrustOptions.addCertValue(io.vertx.core.buffer.Buffer.buffer(pemTrustStore.getContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM certificate value for " + defaultTarget);
                    }

                    options.setPemTrustOptions(pemTrustOptions);
                    break;
                case PKCS12:
                    final PKCS12TrustStore pkcs12TrustStore = (PKCS12TrustStore) sslOptions.getTrustStore();
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (pkcs12TrustStore.getPath() != null && !pkcs12TrustStore.getPath().isEmpty()) {
                        pfxOptions.setPath(pkcs12TrustStore.getPath());
                    } else if (pkcs12TrustStore.getContent() != null && !pkcs12TrustStore.getContent().isEmpty()) {
                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(pkcs12TrustStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 truststore value for " + defaultTarget);
                    }

                    pfxOptions.setAlias(pkcs12TrustStore.getAlias());
                    pfxOptions.setPassword(pkcs12TrustStore.getPassword());
                    options.setPfxTrustOptions(pfxOptions);
                    break;
                case JKS:
                    final JKSTrustStore jksTrustStore = (JKSTrustStore) sslOptions.getTrustStore();
                    final JksOptions jksOptions = new JksOptions();

                    if (jksTrustStore.getPath() != null && !jksTrustStore.getPath().isEmpty()) {
                        jksOptions.setPath(jksTrustStore.getPath());
                    } else if (jksTrustStore.getContent() != null && !jksTrustStore.getContent().isEmpty()) {
                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(jksTrustStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing JKS truststore value for " + defaultTarget);
                    }

                    jksOptions.setAlias(jksTrustStore.getAlias());
                    jksOptions.setPassword(jksTrustStore.getPassword());
                    options.setTrustStoreOptions(jksOptions);
                    break;
            }
        }
    }

    private void configureKeyStore(io.vertx.core.http.HttpClientOptions options) {
        if (sslOptions.getKeyStore() != null) {
            switch (sslOptions.getKeyStore().getType()) {
                case PEM:
                    final PEMKeyStore pemKeyStore = (PEMKeyStore) sslOptions.getKeyStore();
                    final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

                    if (pemKeyStore.getCertPath() != null && !pemKeyStore.getCertPath().isEmpty()) {
                        pemKeyCertOptions.setCertPath(pemKeyStore.getCertPath());
                    } else if (pemKeyStore.getCertContent() != null && !pemKeyStore.getCertContent().isEmpty()) {
                        pemKeyCertOptions.setCertValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getCertContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM certificate value for " + defaultTarget);
                    }

                    if (pemKeyStore.getKeyPath() != null && !pemKeyStore.getKeyPath().isEmpty()) {
                        pemKeyCertOptions.setKeyPath(pemKeyStore.getKeyPath());
                    } else if (pemKeyStore.getKeyContent() != null && !pemKeyStore.getKeyContent().isEmpty()) {
                        pemKeyCertOptions.setKeyValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getKeyContent()));
                    } else {
                        throw new IllegalArgumentException("Missing PEM key value for " + defaultTarget);
                    }

                    options.setPemKeyCertOptions(pemKeyCertOptions);
                    break;
                case PKCS12:
                    final PKCS12KeyStore pkcs12KeyStore = (PKCS12KeyStore) sslOptions.getKeyStore();
                    final PfxOptions pfxOptions = new PfxOptions();

                    if (pkcs12KeyStore.getPath() != null && !pkcs12KeyStore.getPath().isEmpty()) {
                        pfxOptions.setPath(pkcs12KeyStore.getPath());
                    } else if (pkcs12KeyStore.getContent() != null && !pkcs12KeyStore.getContent().isEmpty()) {
                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(pkcs12KeyStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing PKCS12 keystore value for " + defaultTarget);
                    }

                    pfxOptions.setAlias(pkcs12KeyStore.getAlias());
                    pfxOptions.setAliasPassword(pkcs12KeyStore.getKeyPassword());
                    pfxOptions.setPassword(pkcs12KeyStore.getPassword());
                    options.setPfxKeyCertOptions(pfxOptions);
                    break;
                case JKS:
                    final JKSKeyStore jksKeyStore = (JKSKeyStore) sslOptions.getKeyStore();
                    final JksOptions jksOptions = new JksOptions();

                    if (jksKeyStore.getPath() != null && !jksKeyStore.getPath().isEmpty()) {
                        jksOptions.setPath(jksKeyStore.getPath());
                    } else if (jksKeyStore.getContent() != null && !jksKeyStore.getContent().isEmpty()) {
                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(Base64.getDecoder().decode(jksKeyStore.getContent())));
                    } else {
                        throw new IllegalArgumentException("Missing JKS keystore value for " + defaultTarget);
                    }

                    jksOptions.setAlias(jksKeyStore.getAlias());
                    jksOptions.setAliasPassword(jksKeyStore.getKeyPassword());
                    jksOptions.setPassword(jksKeyStore.getPassword());
                    options.setKeyStoreOptions(jksOptions);
                    break;
            }
        }
    }

    private void setSystemProxy(final io.vertx.core.http.HttpClientOptions options) {
        try {
            VertxProxyOptionsUtils.setSystemProxy(options, nodeConfiguration);
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
