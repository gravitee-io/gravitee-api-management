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

import io.gravitee.gateway.reactor.Reactor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.ReactorHandlerResolver;
import io.gravitee.gateway.reactor.handler.context.provider.NodeTemplateVariableProvider;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerResolver;
import io.gravitee.gateway.reactor.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.reactor.processor.transaction.TransactionProcessorFactory;
import io.gravitee.gateway.reactor.impl.DefaultReactor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ReactorConfiguration {

    @Bean
    public Reactor reactor() {
        return new DefaultReactor();
    }

    @Bean
    public ReactorHandlerResolver reactorHandlerResolver() {
        return new DefaultReactorHandlerResolver();
    }

    @Bean
    public ReactorHandlerRegistry reactorHandlerManager() {
        return new DefaultReactorHandlerRegistry();
    }

    @Bean
    public TransactionProcessorFactory transactionHandlerFactory() {
        return new TransactionProcessorFactory();
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
    public NodeTemplateVariableProvider nodeTemplateVariableProvider() {
        return new NodeTemplateVariableProvider();
    }
}
