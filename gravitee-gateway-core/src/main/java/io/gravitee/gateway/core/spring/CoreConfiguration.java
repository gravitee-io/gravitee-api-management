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

import io.gravitee.common.event.EventManager;
import io.gravitee.common.event.impl.EventManagerImpl;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.manager.ApiManager;
import io.gravitee.gateway.core.manager.impl.ApiManagerImpl;
import io.gravitee.gateway.core.policy.spring.PolicyConfiguration;
import io.gravitee.gateway.core.reactor.AsyncGraviteeReactor;
import io.gravitee.gateway.core.reactor.handler.ContextHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.ErrorHandler;
import io.gravitee.gateway.core.reactor.handler.Handler;
import io.gravitee.gateway.core.reactor.handler.impl.ApiContextHandlerFactory;
import io.gravitee.gateway.core.reporter.spring.ReporterConfiguration;
import io.gravitee.gateway.core.repository.spring.RepositoryConfiguration;
import io.gravitee.gateway.core.sync.spring.SyncConfiguration;
import io.gravitee.plugin.spring.PluginConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;
import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({RepositoryConfiguration.class, PluginConfiguration.class, io.gravitee.gateway.core.plugin.spring.PluginConfiguration.class, PolicyConfiguration.class, ReporterConfiguration.class,
        PropertiesConfiguration.class, SyncConfiguration.class})
public class CoreConfiguration {

    protected final static Logger LOGGER = LoggerFactory.getLogger(CoreConfiguration.class);

    @Bean
    @SuppressWarnings("rawtypes")
    public Reactor reactor() {
        return new AsyncGraviteeReactor();
    }

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ApiManager apiManager() {
        return new ApiManagerImpl();
    }

    @Bean(name = "errorHandler")
    public Handler errorHandler() {
        return new ErrorHandler();
    }

    @Bean
    public ContextHandlerFactory handlerFactory() {
        return new ApiContextHandlerFactory();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties(@Qualifier("graviteeProperties") Properties graviteeProperties) throws IOException {
        LOGGER.info("Loading Gravitee placeholder.");

        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(graviteeProperties);

        LOGGER.info("Loading Gravitee placeholder. DONE");

        return propertySourcesPlaceholderConfigurer;
    }
}
