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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.debug.reactor.DebugReactor;
import io.gravitee.gateway.debug.reactor.processor.DebugResponseProcessorChainFactory;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessorFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.node.certificates.KeyStoreLoaderManager;
import io.gravitee.node.vertx.VertxHttpServerFactory;
import io.gravitee.node.vertx.configuration.HttpServerConfiguration;
import io.gravitee.repository.management.api.EventRepository;
import io.vertx.core.Vertx;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@Configuration
public class VertxDebugConfiguration {

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DebugReactorVerticle graviteeDebugVerticle() {
        return new DebugReactorVerticle();
    }

    @Bean
    public Reactor reactor() {
        return new DebugReactor();
    }

    @Bean
    public TransactionProcessorFactory transactionHandlerFactory() {
        return new TransactionProcessorFactory();
    }

    @Bean
    public TraceContextProcessorFactory traceContextHandlerFactory() {
        return new TraceContextProcessorFactory();
    }

    @Bean
    public RequestProcessorChainFactory requestProcessorChainFactory() {
        return new RequestProcessorChainFactory();
    }

    @Bean
    public ResponseProcessorChainFactory responseProcessorChainFactory(EventRepository eventRepository, ObjectMapper objectMapper) {
        return new DebugResponseProcessorChainFactory(eventRepository, objectMapper);
    }

    @Bean
    public NotFoundProcessorChainFactory notFoundProcessorChainFactory() {
        return new NotFoundProcessorChainFactory();
    }

    @Bean("debugHttpServerConfiguration")
    public HttpServerConfiguration debugHttpServerConfiguration(Environment environment) {
        return HttpServerConfiguration
            .builder()
            .withEnvironment(environment)
            .withPort(Integer.parseInt(environment.getProperty("debug.http.port", "8482")))
            .withHost(environment.getProperty("debug.http.host", "localhost"))
            .build();
    }

    @Bean("gatewayDebugHttpServer")
    public VertxHttpServerFactory vertxHttpServerFactory(
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
}
