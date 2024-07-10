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
package io.gravitee.gateway.services.healthcheck.http;

import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.ProtocolVersion;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.rule.EndpointRuleHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.*;
import java.net.URL;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointRuleHandler<T extends HttpEndpoint> extends EndpointRuleHandler<T> {

    private static final String HTTPS_SCHEME = "https";
    private static final String WSS_SCHEME = "wss";
    private static final String GRPCS_SCHEME = "grpcs";

    public HttpEndpointRuleHandler(Vertx vertx, EndpointRule<T> rule, TemplateEngine templateEngine, Environment environment)
        throws Exception {
        super(vertx, rule, templateEngine, environment);
    }

    @Override
    protected RequestOptions prepareHttpClientRequest(URL request, HealthCheckStep step) {
        RequestOptions options = super.prepareHttpClientRequest(request, step);

        // Set timeout on request
        if (rule.endpoint().getHttpClientOptions() != null) {
            options.setTimeout(rule.endpoint().getHttpClientOptions().getReadTimeout());
        }

        return options;
    }

    @Override
    protected HttpClientOptions createHttpClientOptions(final HttpEndpoint endpoint, final URL requestUrl) throws Exception {
        // Prepare HTTP client
        HttpClientOptions httpClientOptions = new HttpClientOptions() // The queue size can contain only a single inflight request for HC
            .setMaxWaitQueueSize(1)
            .setMaxPoolSize(1);

        if (endpoint.getHttpClientOptions() != null) {
            if (environment.getProperty("http.ssl.openssl", Boolean.class, false)) {
                httpClientOptions.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            httpClientOptions
                .setKeepAlive(endpoint.getHttpClientOptions().isKeepAlive())
                .setTcpKeepAlive(endpoint.getHttpClientOptions().isKeepAlive())
                .setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000))
                .setKeepAliveTimeout((int) (endpoint.getHttpClientOptions().getKeepAliveTimeout() / 1000))
                .setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout())
                .setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());

            if (endpoint.getHttpClientOptions().getVersion() == ProtocolVersion.HTTP_2) {
                httpClientOptions.setProtocolVersion(HttpVersion.HTTP_2);
                httpClientOptions.setHttp2ClearTextUpgrade(endpoint.getHttpClientOptions().isClearTextUpgrade());
                httpClientOptions.setHttp2MaxPoolSize(endpoint.getHttpClientOptions().getMaxConcurrentConnections());
            }
        }

        // Configure HTTP proxy
        HttpProxy proxy = endpoint.getHttpProxy();
        if (proxy != null && proxy.isEnabled()) {
            ProxyOptions proxyOptions = null;
            if (proxy.isUseSystemProxy()) {
                proxyOptions = this.systemProxyOptions;
            } else {
                proxyOptions =
                    new ProxyOptions()
                        .setHost(proxy.getHost())
                        .setPort(proxy.getPort())
                        .setUsername(proxy.getUsername())
                        .setPassword(proxy.getPassword())
                        .setType(ProxyType.valueOf(proxy.getType().name()));
            }
            httpClientOptions.setProxyOptions(proxyOptions);
        }

        HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();

        if (
            HTTPS_SCHEME.equalsIgnoreCase(requestUrl.getProtocol()) ||
            WSS_SCHEME.equalsIgnoreCase(requestUrl.getProtocol()) ||
            GRPCS_SCHEME.equalsIgnoreCase(requestUrl.getProtocol())
        ) {
            // Configure SSL
            httpClientOptions.setSsl(true).setUseAlpn(true);

            if (sslOptions != null) {
                httpClientOptions.setVerifyHost(sslOptions.isHostnameVerifier()).setTrustAll(sslOptions.isTrustAll());

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
                            httpClientOptions.setPemTrustOptions(pemTrustOptions);
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
                            httpClientOptions.setPfxTrustOptions(pfxOptions);
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
                            httpClientOptions.setTrustStoreOptions(jksOptions);
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
                            httpClientOptions.setPemKeyCertOptions(pemKeyCertOptions);
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
                            httpClientOptions.setPfxKeyCertOptions(pfxOptions);
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
                            httpClientOptions.setKeyStoreOptions(jksOptions);
                            break;
                    }
                }
            }
        }

        return httpClientOptions;
    }
}
