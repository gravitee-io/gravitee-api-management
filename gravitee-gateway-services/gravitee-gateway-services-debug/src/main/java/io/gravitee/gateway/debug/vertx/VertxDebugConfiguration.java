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

import io.gravitee.gateway.debug.reactor.DebugReactor;
import io.gravitee.gateway.http.vertx.VertxHttpServerFactory;
import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessorFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class VertxDebugConfiguration {

    @Bean
    public VertxDebugHttpClientConfiguration httpServerConfiguration() {
        return new VertxDebugHttpClientConfiguration();
    }

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
    public ResponseProcessorChainFactory responseProcessorChainFactory() {
        return new ResponseProcessorChainFactory();
    }

    @Bean
    public NotFoundProcessorChainFactory notFoundProcessorChainFactory() {
        return new NotFoundProcessorChainFactory();
    }

    @Bean("gatewayDebugHttpServer")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public VertxHttpServerFactory vertxHttpServerFactory() {
        return new VertxHttpServerFactory();
    }
}
