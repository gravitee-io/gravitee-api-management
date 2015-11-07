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
package io.gravitee.management.rest.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;

import io.gravitee.repository.Scope;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class RepositoryConfiguration {

    @Bean
    public static RepositoryBeanFactoryPostProcessor repositoryManagementBeanFactoryPostProcessor(
            @Value("${management.type}") String repositoryType, ConfigurationClassPostProcessor configurationClassPostProcessor) {
        RepositoryBeanFactoryPostProcessor repositoryBeanFactoryPostProcessor = new RepositoryBeanFactoryPostProcessor();
        repositoryBeanFactoryPostProcessor.setRepositoryType(repositoryType);
        repositoryBeanFactoryPostProcessor.setRepositoryScope(Scope.MANAGEMENT);
        repositoryBeanFactoryPostProcessor.setConfigurationClassPostProcessor(configurationClassPostProcessor);
        return repositoryBeanFactoryPostProcessor;
    }

    @Bean
    public static RepositoryBeanFactoryPostProcessor repositoryAnalyticsBeanFactoryPostProcessor(
            @Value("${analytics.type}") String repositoryType, ConfigurationClassPostProcessor configurationClassPostProcessor) {
        RepositoryBeanFactoryPostProcessor repositoryBeanFactoryPostProcessor = new RepositoryBeanFactoryPostProcessor();
        repositoryBeanFactoryPostProcessor.setRepositoryType(repositoryType);
        repositoryBeanFactoryPostProcessor.setRepositoryScope(Scope.ANALYTICS);
        repositoryBeanFactoryPostProcessor.setConfigurationClassPostProcessor(configurationClassPostProcessor);
        return repositoryBeanFactoryPostProcessor;
    }
}
