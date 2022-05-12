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

import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.gateway.reactive.standalone.vertx.HttpProtocolVerticle;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.VertxHttpServerFactory;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class VertxReactorConfiguration {

    @Bean("httpServerConfiguration")
    public HttpServerConfiguration httpServerConfiguration(Environment environment) {
        return HttpServerConfiguration.builder().withEnvironment(environment).withDefaultPort(8082).build();
    }

    @Bean("gatewayHttpServer")
    public VertxHttpServerFactory vertxHttpServerFactory(
        Vertx vertx,
        @Qualifier("httpServerConfiguration") HttpServerConfiguration httpServerConfiguration,
        KeyStoreLoaderManager keyStoreLoaderManager
    ) {
        return new VertxHttpServerFactory(vertx, httpServerConfiguration, keyStoreLoaderManager);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ReactorVerticle v3GraviteeVerticle() {
        // V3 ReactorVerticle bean must be kept while we are still supporting v3 execution mode.
        return new ReactorVerticle();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HttpProtocolVerticle graviteeVerticle(
        @Qualifier("gatewayHttpServer") HttpServer httpServer,
        @Qualifier("httpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        return new HttpProtocolVerticle(httpServer, requestDispatcher);
    }

    @Bean
    public VertxEmbeddedContainer container() {
        return new VertxEmbeddedContainer();
    }
}
