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
package io.gravitee.gateway.handlers.api;

import io.gravitee.gateway.handlers.api.http.client.spring.HttpClientConfiguration;
import io.gravitee.gateway.handlers.api.http.client.spring.HttpClientConfigurationImportSelector;
import io.gravitee.gateway.handlers.api.impl.PathResolverImpl;
import io.gravitee.gateway.policy.*;
import io.gravitee.gateway.policy.impl.*;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
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
    public PolicyManager scopedPolicyManager() {
        return new DefaultPolicyManager(apiReactorHandler());
    }

    @Bean
    public PolicyClassLoaderFactory policyClassLoaderFactory() {
        return new PolicyClassLoaderFactoryImpl();
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
    public ReactorHandler apiReactorHandler() {
        return new ApiReactorHandler();
    }
}
