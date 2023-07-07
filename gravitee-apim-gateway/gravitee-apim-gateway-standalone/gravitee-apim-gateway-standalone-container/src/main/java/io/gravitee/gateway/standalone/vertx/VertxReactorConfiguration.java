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
package io.gravitee.gateway.standalone.vertx;

import static io.gravitee.node.vertx.server.http.VertxHttpServerOptions.HTTP_PREFIX;

import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.gateway.reactive.standalone.vertx.HttpProtocolVerticle;
import io.gravitee.node.api.server.DefaultServerManager;
import io.gravitee.node.api.server.ServerManager;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.server.VertxServer;
import io.gravitee.node.vertx.server.VertxServerFactory;
import io.gravitee.node.vertx.server.VertxServerOptions;
import io.gravitee.node.vertx.server.http.VertxHttpServerOptions;
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

    protected static final String SERVERS_PREFIX = "servers";

    @Bean
    public ServerManager serverManager(
        VertxServerFactory<VertxServer<?, VertxServerOptions>, VertxServerOptions> serverFactory,
        Environment environment,
        KeyStoreLoaderManager keyStoreLoaderManager
    ) {
        int counter = 0;

        final DefaultServerManager serverManager = new DefaultServerManager();
        if (environment.getProperty(SERVERS_PREFIX + "[" + counter + "].type") != null) {
            // There is, at least one server configured in the list.
            String prefix = SERVERS_PREFIX + "[" + counter++ + "]";

            while ((environment.getProperty(prefix + ".type")) != null) {
                final VertxServerOptions options = VertxServerOptions
                    .builder(environment, prefix, keyStoreLoaderManager)
                    .defaultPort(8082)
                    .build();
                serverManager.register(serverFactory.create(options));
                prefix = SERVERS_PREFIX + "[" + counter++ + "]";
            }
        } else {
            // No server list configured, fallback to single 'http' server configuration.
            final VertxHttpServerOptions options = VertxHttpServerOptions
                .builder()
                .defaultPort(8082)
                .prefix(HTTP_PREFIX)
                .keyStoreLoaderManager(keyStoreLoaderManager)
                .environment(environment)
                .id("http")
                .build();

            serverManager.register(serverFactory.create(options));
        }

        return serverManager;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HttpProtocolVerticle graviteeVerticle(
        ServerManager serverManager,
        @Qualifier("httpRequestDispatcher") HttpRequestDispatcher requestDispatcher
    ) {
        return new HttpProtocolVerticle(serverManager, requestDispatcher);
    }

    @Bean
    public VertxEmbeddedContainer container() {
        return new VertxEmbeddedContainer();
    }
}
