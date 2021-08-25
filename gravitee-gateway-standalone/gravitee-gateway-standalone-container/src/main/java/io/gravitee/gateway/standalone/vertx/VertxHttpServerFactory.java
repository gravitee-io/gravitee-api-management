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
package io.gravitee.gateway.standalone.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.*;
import io.vertx.core.tracing.TracingPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerFactory implements FactoryBean<HttpServer> {

    private final Logger logger = LoggerFactory.getLogger(VertxHttpServerFactory.class);

    private static final String CERTIFICATE_FORMAT_JKS = "JKS";
    private static final String CERTIFICATE_FORMAT_PEM = "PEM";
    private static final String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";
    private static final String CERTIFICATE_FORMAT_SELF_SIGNED = "SELF-SIGNED";

    @Autowired
    private Vertx vertx;

    @Autowired
    private VertxHttpServerConfiguration httpServerConfiguration;

    @Override
    public HttpServer getObject() throws Exception {
        HttpServerOptions options = new HttpServerOptions();

        options.setTracingPolicy(TracingPolicy.ALWAYS);

        // Binding port
        options.setPort(httpServerConfiguration.getPort());
        options.setHost(httpServerConfiguration.getHost());

        if (httpServerConfiguration.isSecured()) {
            if (httpServerConfiguration.isOpenssl()) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }

            options.setSsl(httpServerConfiguration.isSecured());
            options.setUseAlpn(httpServerConfiguration.isAlpn());
            options.setSni(httpServerConfiguration.isSni());

            // TLS protocol support
            if (httpServerConfiguration.getTlsProtocols() != null) {
                options.setEnabledSecureTransportProtocols(
                    new HashSet<>(Arrays.asList(httpServerConfiguration.getTlsProtocols().split("\\s*,\\s*")))
                );
            }

            // TLS ciphers support
            if (httpServerConfiguration.getTlsCiphers() != null) {
                String[] incomingCipherSuites = httpServerConfiguration.getTlsCiphers().split("\\s*,\\s*");
                options.getEnabledCipherSuites().clear();
                for (String cipherSuite : incomingCipherSuites) {
                    options.addEnabledCipherSuite(cipherSuite);
                }
            }

            if (httpServerConfiguration.isClientAuth() == VertxHttpServerConfiguration.ClientAuthMode.NONE) {
                options.setClientAuth(ClientAuth.NONE);
            } else if (httpServerConfiguration.isClientAuth() == VertxHttpServerConfiguration.ClientAuthMode.REQUEST) {
                options.setClientAuth(ClientAuth.REQUEST);
            } else if (httpServerConfiguration.isClientAuth() == VertxHttpServerConfiguration.ClientAuthMode.REQUIRED) {
                options.setClientAuth(ClientAuth.REQUIRED);
            }

            if (httpServerConfiguration.getTrustStorePaths() != null && !httpServerConfiguration.getTrustStorePaths().isEmpty()) {
                if (
                    httpServerConfiguration.getTrustStoreType() == null ||
                    httpServerConfiguration.getTrustStoreType().isEmpty() ||
                    httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
                ) {
                    options.setTrustStoreOptions(
                        new JksOptions()
                            .setPath(httpServerConfiguration.getTrustStorePaths().get(0))
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    final PemTrustOptions pemTrustOptions = new PemTrustOptions();
                    httpServerConfiguration.getTrustStorePaths().forEach(pemTrustOptions::addCertPath);
                    options.setPemTrustOptions(pemTrustOptions);
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(
                        new PfxOptions()
                            .setPath(httpServerConfiguration.getTrustStorePaths().get(0))
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_SELF_SIGNED)) {
                    options.setPemTrustOptions(SelfSignedCertificate.create().trustOptions());
                }
            }

            if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)) {
                if (httpServerConfiguration.getKeyStorePath() == null || httpServerConfiguration.getKeyStorePath().isEmpty()) {
                    logger.error("A JKS Keystore is missing. Skipping SSL keystore configuration...");
                } else {
                    options.setKeyStoreOptions(
                        new JksOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword())
                    );
                }
            } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                if (
                    httpServerConfiguration.getKeyStoreCertificates() == null || httpServerConfiguration.getKeyStoreCertificates().isEmpty()
                ) {
                    logger.error("A PEM Keystore is missing. Skipping SSL keystore configuration...");
                } else {
                    final PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();

                    httpServerConfiguration
                        .getKeyStoreCertificates()
                        .forEach(
                            certificate ->
                                pemKeyCertOptions.addCertPath(certificate.getCertificate()).addKeyPath(certificate.getPrivateKey())
                        );

                    options.setPemKeyCertOptions(pemKeyCertOptions);
                }
            } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                if (httpServerConfiguration.getKeyStorePath() == null || httpServerConfiguration.getKeyStorePath().isEmpty()) {
                    logger.error("A PKCS#12 Keystore is missing. Skipping SSL keystore configuration...");
                } else {
                    options.setPfxKeyCertOptions(
                        new PfxOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword())
                    );
                }
            } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_SELF_SIGNED)) {
                options.setPemKeyCertOptions(SelfSignedCertificate.create().keyCertOptions());
            }
        }

        if (httpServerConfiguration.isProxyProtocol()) {
            options.setUseProxyProtocol(true).setProxyProtocolTimeout(httpServerConfiguration.getProxyProtocolTimeout());
        }

        options.setHandle100ContinueAutomatically(true);

        // Customizable configuration
        options.setCompressionSupported(httpServerConfiguration.isCompressionSupported());
        options.setIdleTimeout(httpServerConfiguration.getIdleTimeout());
        options.setTcpKeepAlive(httpServerConfiguration.isTcpKeepAlive());
        options.setMaxChunkSize(httpServerConfiguration.getMaxChunkSize());
        options.setMaxHeaderSize(httpServerConfiguration.getMaxHeaderSize());
        options.setMaxInitialLineLength(httpServerConfiguration.getMaxInitialLineLength());

        // Configure websocket
        System.setProperty("vertx.disableWebsockets", Boolean.toString(!httpServerConfiguration.isWebsocketEnabled()));
        if (httpServerConfiguration.isWebsocketEnabled() && httpServerConfiguration.getWebsocketSubProtocols() != null) {
            options.setWebSocketSubProtocols(
                new ArrayList<>(Arrays.asList(httpServerConfiguration.getWebsocketSubProtocols().split("\\s*,\\s*")))
            );
            options.setPerMessageWebSocketCompressionSupported(httpServerConfiguration.isPerMessageWebSocketCompressionSupported());
            options.setPerFrameWebSocketCompressionSupported(httpServerConfiguration.isPerFrameWebSocketCompressionSupported());
        }

        return vertx.createHttpServer(options);
    }

    @Override
    public Class<?> getObjectType() {
        return HttpServer.class;
    }

    @Override
    public boolean isSingleton() {
        // Scope is managed indirectly by Vertx verticle.
        return false;
    }
}
