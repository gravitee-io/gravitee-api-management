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
import io.gravitee.gateway.core.plugin.spring.PluginConfiguration;
import io.gravitee.gateway.core.policy.spring.PolicyConfiguration;
import io.gravitee.gateway.core.reactor.AsyncGraviteeReactor;
import io.gravitee.gateway.core.reporter.spring.ReporterConfiguration;
import io.gravitee.gateway.core.repository.spring.RepositoryBeanFactoryPostProcessor;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.gateway.core.service.impl.ApiServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Properties;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({PluginConfiguration.class, PolicyConfiguration.class, ReporterConfiguration.class})
public class CoreConfiguration {

    protected final static Logger LOGGER = LoggerFactory.getLogger(CoreConfiguration.class);

    public final static String GRAVITEE_CONFIGURATION = "gravitee.conf";

    @Bean
    public Reactor reactor() {
        return new AsyncGraviteeReactor();
    }

    @Bean
    public static RepositoryBeanFactoryPostProcessor repositoryBeanFactoryPostProcessor() {
        return new RepositoryBeanFactoryPostProcessor();
    }

    @Bean
    public EventManager eventManager() {
        return new EventManagerImpl();
    }

    @Bean
    public ApiService apiService() {
        return new ApiServiceImpl();
    }

    @Bean(name = "errorHandler")
    public Handler errorHandler() {
        return new ErrorHandler();
    }

    @Bean(name = "gravityProperties")
    public static Properties gravityProperties() throws IOException {
        LOGGER.info("Loading Gravitee configuration.");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        String yamlConfiguration = System.getProperty(GRAVITEE_CONFIGURATION);
        Resource yamlResource = new FileSystemResource(yamlConfiguration);

        LOGGER.info("\tGravitee configuration loaded from {}", yamlResource.getURL().getPath());

        yaml.setResources(yamlResource);
        Properties properties = yaml.getObject();
        LOGGER.info("Loading Gravitee configuration. DONE");

        return properties;

    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() throws IOException {
        LOGGER.info("Loading Gravitee placeholder.");

        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setProperties(gravityProperties());

        LOGGER.info("Loading Gravitee placeholder. DONE");

        return propertySourcesPlaceholderConfigurer;
    }
}
