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
package io.gravitee.gateway.core.reactor.handler.impl;

import io.gravitee.gateway.core.http.client.spring.HttpClientConfiguration;
import io.gravitee.gateway.core.policy.PathResolver;
import io.gravitee.gateway.core.policy.PolicyConfigurationFactory;
import io.gravitee.gateway.core.policy.PolicyFactory;
import io.gravitee.gateway.core.policy.PolicyResolver;
import io.gravitee.gateway.core.policy.impl.*;
import io.gravitee.gateway.core.reactor.handler.ContextReactorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({HttpClientConfiguration.class})
public class ApiHandlerConfiguration {

    @Bean
    public PolicyFactory policyFactory() {
        return new PolicyFactoryImpl();
    }

    @Bean
    public PolicyResolver policyResolver() {
        return new PolicyResolverImpl();
    }

    @Bean
    public PathResolver pathResolver() {
        return new PathResolverImpl();
    }

    @Bean
    public PolicyConfigurationFactory policyConfigurationFactory() {
        return new PolicyConfigurationFactoryImpl();
    }

    @Bean
    public RequestPolicyChainBuilder requestPolicyChainBuilder() {
        return new RequestPolicyChainBuilder();
    }

    @Bean
    public ResponsePolicyChainBuilder responsePolicyChainBuilder() {
        return new ResponsePolicyChainBuilder();
    }

    @Bean
    public ContextReactorHandler handler() {
        return new ApiReactorHandler();
    }
}
