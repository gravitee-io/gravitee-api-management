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
package io.gravitee.repository.bridge.server.http.spring;

import io.gravitee.repository.bridge.server.http.auth.BasicAuthProvider;
import io.gravitee.repository.bridge.server.http.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.web.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HttpServerSpringConfiguration {

    private static final String CERTIFICATE_FORMAT_JKS = "JKS";
    private static final String CERTIFICATE_FORMAT_PEM = "PEM";
    private static final String CERTIFICATE_FORMAT_PKCS12 = "PKCS12";

    @Bean("bridgeRouter")
    public Router router(Vertx vertx) {
        return Router.router(vertx);
    }

    @Bean("vertxBridgeHttpServer")
    public HttpServer httpServer(Vertx vertx, HttpServerConfiguration httpServerConfiguration) {
        HttpServerOptions options = new HttpServerOptions()
            .setPort(httpServerConfiguration.getPort())
            .setHost(httpServerConfiguration.getHost())
            .setCompressionSupported(true);

        if (httpServerConfiguration.isSecured()) {
            options.setSsl(httpServerConfiguration.isSecured());
            options.setUseAlpn(httpServerConfiguration.isAlpn());

            if (httpServerConfiguration.isClientAuth()) {
                options.setClientAuth(ClientAuth.REQUIRED);
            }

            if (httpServerConfiguration.getTrustStorePath() != null) {
                if (
                    httpServerConfiguration.getTrustStoreType() == null ||
                    httpServerConfiguration.getTrustStoreType().isEmpty() ||
                    httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
                ) {
                    options.setTrustStoreOptions(
                        new JksOptions()
                            .setPath(httpServerConfiguration.getTrustStorePath())
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemTrustOptions(new PemTrustOptions().addCertPath(httpServerConfiguration.getTrustStorePath()));
                } else if (httpServerConfiguration.getTrustStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxTrustOptions(
                        new PfxOptions()
                            .setPath(httpServerConfiguration.getTrustStorePath())
                            .setPassword(httpServerConfiguration.getTrustStorePassword())
                    );
                }
            }

            if (httpServerConfiguration.getKeyStorePath() != null) {
                if (
                    httpServerConfiguration.getKeyStoreType() == null ||
                    httpServerConfiguration.getKeyStoreType().isEmpty() ||
                    httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_JKS)
                ) {
                    options.setKeyStoreOptions(
                        new JksOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword())
                    );
                } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PEM)) {
                    options.setPemKeyCertOptions(new PemKeyCertOptions().addCertPath(httpServerConfiguration.getKeyStorePath()));
                } else if (httpServerConfiguration.getKeyStoreType().equalsIgnoreCase(CERTIFICATE_FORMAT_PKCS12)) {
                    options.setPfxKeyCertOptions(
                        new PfxOptions()
                            .setPath(httpServerConfiguration.getKeyStorePath())
                            .setPassword(httpServerConfiguration.getKeyStorePassword())
                    );
                }
            }
        }

        options.setHandle100ContinueAutomatically(true);

        // Customizable configuration
        options.setIdleTimeout(httpServerConfiguration.getIdleTimeout());
        options.setTcpKeepAlive(httpServerConfiguration.isTcpKeepAlive());
        options.setMaxChunkSize(httpServerConfiguration.getMaxChunkSize());
        options.setMaxHeaderSize(httpServerConfiguration.getMaxHeaderSize());

        return vertx.createHttpServer(options);
    }

    @Bean("bridgeAuthProvider")
    public AuthenticationProvider nodeAuthProvider() {
        return new BasicAuthProvider();
    }

    @Bean
    public HttpServerConfiguration nodeHttpServerConfiguration() {
        return new HttpServerConfiguration();
    }
}
