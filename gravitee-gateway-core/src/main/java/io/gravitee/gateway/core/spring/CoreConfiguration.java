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

import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.event.EventManager;
import io.gravitee.gateway.core.event.impl.EventManagerImpl;
import io.gravitee.gateway.core.handler.ErrorHandler;
import io.gravitee.gateway.core.handler.Handler;
import io.gravitee.gateway.core.handler.spring.HandlerConfiguration;
import io.gravitee.gateway.core.plugin.spring.PluginConfiguration;
import io.gravitee.gateway.core.policy.spring.PolicyConfiguration;
import io.gravitee.gateway.core.reactor.AsyncGraviteeReactor;
import io.gravitee.gateway.core.reporter.spring.ReporterConfiguration;
import io.gravitee.gateway.core.repository.spring.RepositoryConfiguration;
import io.gravitee.gateway.core.sync.spring.SyncConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({RepositoryConfiguration.class, PluginConfiguration.class, PolicyConfiguration.class, ReporterConfiguration.class,
        PropertiesConfiguration.class, SyncConfiguration.class, HandlerConfiguration.class})
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

    @Bean(name = "errorHandler")
    public Handler errorHandler() {
        return new ErrorHandler();
    }


    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() throws IOException {
        LOGGER.info("Loading Gravitee placeholder.");

        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(PropertiesConfiguration.graviteeProperties());

        LOGGER.info("Loading Gravitee placeholder. DONE");

        return propertySourcesPlaceholderConfigurer;
    }
}
