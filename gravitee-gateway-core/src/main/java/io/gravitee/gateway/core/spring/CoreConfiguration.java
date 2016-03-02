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
package io.gravitee.gateway.core.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.definition.validator.Validator;
import io.gravitee.gateway.core.definition.validator.ValidatorImpl;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.core.reactor.GraviteeReactor;
import io.gravitee.gateway.core.reactor.handler.ContextHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.NotFoundReactorHandler;
import io.gravitee.gateway.core.reactor.handler.ReactorHandler;
import io.gravitee.gateway.core.reactor.handler.impl.ApiContextHandlerFactory;
import io.gravitee.gateway.core.reporter.spring.ReporterConfiguration;
import io.gravitee.gateway.core.repository.spring.RepositoryPluginConfiguration;
import io.gravitee.gateway.core.service.spring.ServiceConfiguration;
import io.gravitee.plugin.core.spring.PluginConfiguration;
import io.gravitee.plugin.policy.spring.PolicyPluginConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({
        PropertiesConfiguration.class,
        PluginConfiguration.class,
        PolicyPluginConfiguration.class,
        RepositoryPluginConfiguration.class,
        ReporterConfiguration.class,
        ServiceConfiguration.class,
})
public class CoreConfiguration {

    @Bean
    @SuppressWarnings("rawtypes")
    public Reactor reactor() {
        return new GraviteeReactor();
    }

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new GraviteeMapper();
    }

    @Bean
    public ApiManager apiManager() {
        return new ApiManagerImpl();
    }

    @Bean
    public Validator validator() {
        return new ValidatorImpl();
    }

    @Bean(name = "notFoundHandler")
    public ReactorHandler errorHandler(Environment environment) {
        return new NotFoundReactorHandler(environment);
    }

    @Bean
    public ContextHandlerFactory handlerFactory() {
        return new ApiContextHandlerFactory();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(@Qualifier("graviteeProperties") Properties graviteeProperties) {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(graviteeProperties);
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(true);

        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public static PropertySourceBeanProcessor propertySourceBeanProcessor(@Qualifier("graviteeProperties") Properties graviteeProperties,
                                                                          Environment environment) {
        // Using this we are now able to use {@link org.springframework.core.env.Environment} in Spring beans
        return new PropertySourceBeanProcessor(graviteeProperties, environment);
    }
}
