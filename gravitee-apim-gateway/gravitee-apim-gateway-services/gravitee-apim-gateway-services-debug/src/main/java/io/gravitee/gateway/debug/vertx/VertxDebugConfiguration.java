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
package io.gravitee.gateway.debug.vertx;

import io.gravitee.gateway.reactive.debug.vertx.DebugHttpProtocolVerticle;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.node.api.certificate.KeyStoreLoaderFactoryRegistry;
import io.gravitee.node.api.certificate.KeyStoreLoaderOptions;
import io.gravitee.node.api.certificate.TrustStoreLoaderOptions;
import io.gravitee.node.vertx.server.http.VertxHttpServer;
import io.gravitee.node.vertx.server.http.VertxHttpServerFactory;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
import io.vertx.core.Verticle;
import io.vertx.rxjava3.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@Configuration
public class VertxDebugConfiguration {

    @Bean("debugHttpClientConfiguration")
    public VertxDebugHttpClientConfiguration debugHttpClientConfiguration(
        @Qualifier("debugServer") VertxHttpServer debugServer,
        Environment environment
    ) {
        final VertxHttpServerOptions options = debugServer.options();
        return VertxDebugHttpClientConfiguration.builder()
            .port(options.getPort())
            .host(options.getHost())
            .secured(options.isSecured())
            .openssl(options.isOpenssl())
            .compressionSupported(options.isCompressionSupported())
            .alpn(options.isAlpn())
            .connectTimeout(environment.getProperty("debug.timeout.connect", Integer.class, 5000))
            .requestTimeout(environment.getProperty("debug.timeout.request", Integer.class, 30000))
            .build();
    }

    @Bean("debugServerFactory")
    public VertxHttpServerFactory debugHttpServerFactory(
        Vertx vertx,
        KeyStoreLoaderFactoryRegistry<KeyStoreLoaderOptions> keyStoreLoaderFactoryRegistry,
        KeyStoreLoaderFactoryRegistry<TrustStoreLoaderOptions> truststoreLoaderFactoryRegistry
    ) {
        return new VertxHttpServerFactory(vertx, keyStoreLoaderFactoryRegistry, truststoreLoaderFactoryRegistry);
    }

    @Bean("debugServer")
    public VertxHttpServer debugServer(VertxHttpServerFactory debugHttpServerFactory, Environment environment) {
        final VertxHttpServerOptions.VertxHttpServerOptionsBuilder<?, ?> optionsBuilder;

        if (environment.getProperty("servers[0].type") != null) {
            // Use the first server from the server list.
            optionsBuilder = VertxDebugHttpServerOptions.builder().prefix("servers[0]").environment(environment);
        } else {
            // No server list configured, fallback to single http server configuration.
            optionsBuilder = VertxDebugHttpServerOptions.builder().prefix("http").environment(environment);
        }

        return debugHttpServerFactory.create(optionsBuilder.build());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Verticle debugVerticle(
        @Qualifier("debugServer") VertxHttpServer debugServer,
        @Qualifier("debugHttpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        return new DebugHttpProtocolVerticle(debugServer, requestDispatcher);
    }
}
