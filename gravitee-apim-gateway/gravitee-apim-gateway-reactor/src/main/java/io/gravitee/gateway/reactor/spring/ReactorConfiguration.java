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
package io.gravitee.gateway.reactor.spring;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.utils.Hex;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.reactor.DefaultHttpRequestDispatcher;
import io.gravitee.gateway.reactive.reactor.HttpRequestDispatcher;
import io.gravitee.gateway.reactive.reactor.handler.DefaultEntrypointResolver;
import io.gravitee.gateway.reactive.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactoryManager;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.context.provider.NodeTemplateVariableProvider;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import io.gravitee.gateway.reactor.processor.NotFoundProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.transaction.TraceContextProcessorFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.node.api.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ReactorConfiguration {

    private static final String HEX_FORMAT = "hex";

    @Bean
    public IdGenerator idGenerator(@Value("${handlers.request.format:uuid}") String requestFormat) {
        if (HEX_FORMAT.equals(requestFormat)) {
            return new Hex();
        } else {
            return new UUID();
        }
    }

    @Bean
    public HttpRequestDispatcher httpRequestDispatcher(IdGenerator idGenerator) {
        return new DefaultHttpRequestDispatcher(idGenerator);
    }

    @Bean
    public EntrypointResolver entrypointResolver(ReactorHandlerRegistry reactorHandlerRegistry) {
        return new DefaultEntrypointResolver(reactorHandlerRegistry);
    }

    @Bean
    public ReactorHandlerRegistry reactorHandlerManager(ReactorHandlerFactoryManager reactorHandlerFactoryManager) {
        return new DefaultReactorHandlerRegistry(reactorHandlerFactoryManager);
    }

    @Bean
    public ReactorHandlerFactoryManager reactorHandlerFactoryManager(ReactorHandlerFactory reactorHandlerFactory) {
        return new ReactorHandlerFactoryManager(reactorHandlerFactory);
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

    @Bean
    public NodeTemplateVariableProvider nodeTemplateVariableProvider(Node node, GatewayConfiguration gatewayConfiguration) {
        return new NodeTemplateVariableProvider(node, gatewayConfiguration);
    }
}
