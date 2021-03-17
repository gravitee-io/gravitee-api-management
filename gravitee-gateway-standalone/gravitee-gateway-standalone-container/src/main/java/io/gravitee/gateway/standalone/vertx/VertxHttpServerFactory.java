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
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxHttpServerFactory implements FactoryBean<HttpServer> {

    private static final String CERTIFICATE_FORMAT_JKS = "JKS";
    private static final String CERTIFICATE_FORMAT_PEM = "PEM";
    private static final String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";

    @Autowired
    private Vertx vertx;

    @Autowired
    private VertxHttpServerConfiguration httpServerConfiguration;

    @Override
    public HttpServer getObject() throws Exception {
        HttpServerOptions options = new HttpServerOptions();

        // Binding port
        options.setPort(httpServerConfiguration.getPort());
        options.setHost(httpServerConfiguration.getHost());

        // Netty pool buffers must be enabled by default
        options.setUsePooledBuffers(true);

        if (httpServerConfiguration.isSecured()) {
            options.setSsl(httpServerConfiguration.isSecured());
            options.setUseAlpn(httpServerConfiguration.isAlpn());
            options.setSni(httpServerConfiguration.isSni());

            // TLS protocol support
            if (httpServerConfiguration.getTlsProtocols() != null) {
                options.setEnabledSecureTransportProtocols(new HashSet<>(Arrays.asList(httpServerConfiguration.getTlsProtocols().split("\\s*,\\s*"))));
            }

            // TLS ciphers support
            if (httpServerConfiguration.getTlsCiphers() != null) {
                String[] incomingCipherSuites = httpServerConfiguration.getTlsCiphers().split("\\s*,\\s*");
                options.getEnabledCipherSuites().clear();
                for (String cipherSuite: incomingCipherSuites) {
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

            if (httpServerConfiguration.getTrustStorePath() != null) {
                if (httpServerConfiguration.getTrustStoreType() == null || httpServerConfiguration.getTrustStoreType().isEmpty() ||
                        httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)) {
                    options.setTrustStoreOptions(new JksOptions()
                            .setPath(httpServerConfiguration.getTrustStorePath())
                            .setPassword(httpServerConfiguration.getTrustStorePassword()));
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemTrustOptions(new PemTrustOptions()
                            .addCertPath(httpServerConfiguration.getTrustStorePath()));
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(new PfxOptions()
                            .setPath(httpServerConfiguration.getTrustStorePath())
                            .setPassword(httpServerConfiguration.getTrustStorePassword()));
                }
            }

            if (httpServerConfiguration.getKeyStorePath() != null) {
                if (httpServerConfiguration.getKeyStoreType() == null || httpServerConfiguration.getKeyStoreType().isEmpty() ||
                        httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)) {
                    options.setKeyStoreOptions(new JksOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword()));
                } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemKeyCertOptions(new PemKeyCertOptions()
                            .addCertPath(httpServerConfiguration.getKeyStorePath()));
                } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxKeyCertOptions(new PfxOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword()));
                }
            }
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
            options.setWebSocketSubProtocols(new ArrayList<>(Arrays.asList(httpServerConfiguration.getWebsocketSubProtocols().split("\\s*,\\s*"))));
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
