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
import io.gravitee.gateway.core.policy.ClassLoaderFactory;
import io.gravitee.gateway.core.policy.impl.ClassLoaderFactoryImpl;
import io.gravitee.gateway.core.policy.spring.PolicyConfiguration;
import io.gravitee.gateway.core.reactor.AsyncGraviteeReactor;
import io.gravitee.gateway.core.service.ApiService;
import io.gravitee.gateway.core.service.impl.ApiServiceImpl;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
@Import({PolicyConfiguration.class})
public class CoreConfiguration {

    private final static String GRAVITEE_CONFIGURATION = "gravitee.conf";

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

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();

        String yamlConfiguration = System.getProperty(GRAVITEE_CONFIGURATION);
        Resource yamlResource = null;

        if (yamlConfiguration == null || yamlConfiguration.isEmpty()) {
            // Load the default (empty) configuration just to avoid undefined bean with Spring
            yamlResource = new ClassPathResource("/gravitee.yml");
        } else {
            yamlResource = new FileSystemResource(yamlConfiguration);
        }

        yaml.setResources(yamlResource);
        propertySourcesPlaceholderConfigurer.setProperties(yaml.getObject());
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public ClassLoaderFactory classLoaderFactory() {
        return new ClassLoaderFactoryImpl();
    }
}
