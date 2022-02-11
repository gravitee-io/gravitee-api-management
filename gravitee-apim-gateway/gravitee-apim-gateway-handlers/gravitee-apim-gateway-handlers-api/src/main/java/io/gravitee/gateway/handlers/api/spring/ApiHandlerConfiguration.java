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
package io.gravitee.gateway.handlers.api.spring;

import io.gravitee.common.util.DataEncryptor;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.core.component.spring.SpringComponentProvider;
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.handlers.api.ApiContextHandlerFactory;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.manager.endpoint.ApiManagementEndpoint;
import io.gravitee.gateway.handlers.api.manager.endpoint.ApisManagementEndpoint;
import io.gravitee.gateway.handlers.api.manager.endpoint.NodeApisEndpointInitializer;
import io.gravitee.gateway.handlers.api.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.policy.PolicyChainProviderLoader;
import io.gravitee.gateway.policy.PolicyFactoryCreator;
import io.gravitee.gateway.policy.PolicyPluginFactory;
import io.gravitee.gateway.policy.impl.PolicyFactoryCreatorImpl;
import io.gravitee.gateway.policy.impl.PolicyPluginFactoryImpl;
import io.gravitee.gateway.reactor.handler.ReactorHandlerFactory;
import io.gravitee.gateway.reactor.handler.context.ApiTemplateVariableProviderFactory;
import io.gravitee.node.api.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class ApiHandlerConfiguration {

    @Autowired
    private Environment environment;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Node node;

    @Autowired
    private io.gravitee.node.api.configuration.Configuration configuration;

    @Bean
    public ApiManager apiManager() {
        return new ApiManagerImpl();
    }

    @Bean
    public DefaultClassLoader classLoader() {
        return new DefaultClassLoader(this.getClass().getClassLoader());
    }

    @Bean
    public ApisManagementEndpoint apisManagementEndpoint() {
        return new ApisManagementEndpoint();
    }

    @Bean
    public ApiManagementEndpoint apiManagementEndpoint() {
        return new ApiManagementEndpoint();
    }

    @Bean
    public NodeApisEndpointInitializer nodeApisEndpointInitializer() {
        return new NodeApisEndpointInitializer();
    }

    @Bean
    public PolicyPluginFactory policyPluginFactory() {
        return new PolicyPluginFactoryImpl();
    }

    @Bean
    public PolicyFactoryCreator policyFactoryCreator(final PolicyPluginFactory policyPluginFactory) {
        return new PolicyFactoryCreatorImpl(configuration, policyPluginFactory, new ExpressionLanguageStringConditionEvaluator());
    }

    @Bean
    public ComponentProvider componentProvider() {
        return new SpringComponentProvider(applicationContext);
    }

    @Bean
    public DataEncryptor apiPropertiesEncryptor() {
        return new DataEncryptor(environment, "api.properties.encryption.secret", "vvLJ4Q8Khvv9tm2tIPdkGEdmgKUruAL6");
    }

    @Bean
    public ApiTemplateVariableProviderFactory apiTemplateVariableProviderFactory() {
        return new ApiTemplateVariableProviderFactory();
    }

    @Bean
    public ReactorHandlerFactory<Api> reactorHandlerFactory(
        PolicyFactoryCreator policyFactoryCreator,
        PolicyChainProviderLoader policyChainProviderLoader
    ) {
        return new ApiContextHandlerFactory(applicationContext, configuration, node, policyFactoryCreator, policyChainProviderLoader);
    }
}
