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
package io.gravitee.gateway.debug.vertx;

import static io.gravitee.gateway.env.GatewayConfiguration.JUPITER_MODE_ENABLED_BY_DEFAULT;
import static io.gravitee.gateway.env.GatewayConfiguration.JUPITER_MODE_ENABLED_KEY;

import io.gravitee.gateway.jupiter.debug.vertx.DebugHttpProtocolVerticle;
import io.gravitee.gateway.jupiter.reactor.HttpRequestDispatcher;
import io.gravitee.gateway.reactor.handler.EntrypointResolver;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.VertxHttpServerFactory;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@Configuration
public class VertxDebugConfiguration {

    @Value("${" + JUPITER_MODE_ENABLED_KEY + ":" + JUPITER_MODE_ENABLED_BY_DEFAULT + "}")
    private boolean jupiterMode;

    @Bean("debugHttpServerConfiguration")
    public HttpServerConfiguration debugHttpServerConfiguration(Environment environment) {
        return HttpServerConfiguration
            .builder()
            .withEnvironment(environment)
            .withPort(Integer.parseInt(environment.getProperty("debug.http.port", "8482")))
            .withHost(environment.getProperty("debug.http.host", "localhost"))
            .build();
    }

    @Bean("debugGatewayHttpServer")
    public VertxHttpServerFactory debugGatewayHttpServer(
        Vertx vertx,
        @Qualifier("debugHttpServerConfiguration") HttpServerConfiguration httpServerConfiguration,
        KeyStoreLoaderManager keyStoreLoaderManager
    ) {
        return new VertxHttpServerFactory(vertx, httpServerConfiguration, keyStoreLoaderManager);
    }

    @Bean("debugHttpClientConfiguration")
    public VertxDebugHttpClientConfiguration debugHttpClientConfiguration() {
        return new VertxDebugHttpClientConfiguration();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public Verticle debugVerticle(
        @Qualifier("debugGatewayHttpServer") HttpServer httpServer,
        @Qualifier("debugHttpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        if (jupiterMode) {
            return new DebugHttpProtocolVerticle(httpServer, requestDispatcher);
        } else {
            return new DebugReactorVerticle();
        }
    }
}
