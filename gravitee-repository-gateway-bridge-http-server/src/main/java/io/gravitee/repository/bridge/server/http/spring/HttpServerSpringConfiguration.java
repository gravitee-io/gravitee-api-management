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
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Router;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class HttpServerSpringConfiguration {

    @Bean("bridgeRouter")
    public Router router(Vertx vertx) {
        return Router.router(vertx);
    }

    @Bean("vertxBridgeHttpServer")
    public HttpServer httpServer(Vertx vertx, HttpServerConfiguration configuration) {
        HttpServerOptions options =
            new HttpServerOptions()
                .setPort(configuration.getPort())
                .setHost(configuration.getHost());

        return vertx.createHttpServer(options);
    }

    @Bean("bridgeAuthProvider")
    public AuthProvider nodeAuthProvider() {
        return new BasicAuthProvider();
    }

    @Bean
    public HttpServerConfiguration nodeHttpServerConfiguration() {
        return new HttpServerConfiguration();
    }
}
