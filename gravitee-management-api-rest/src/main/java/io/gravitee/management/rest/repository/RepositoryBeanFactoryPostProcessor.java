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

import io.gravitee.repository.Repository;
import io.gravitee.repository.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RepositoryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanFactoryPostProcessor.class);

    private ConfigurationClassPostProcessor configurationClassPostProcessor;

    private String repositoryType;

    private Scope repositoryScope;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (repositoryScope == null) {
            LOGGER.error("No repository scope provided.");
            throw new IllegalStateException("No repository scope provided.");
        }

        LOGGER.info("Looking for a repository [{}] implementation: {}", repositoryScope.getName(), repositoryType);

        if (repositoryType == null || repositoryType.isEmpty()) {
            LOGGER.error("No repository type defined in configuration for {}", repositoryScope.getName());

            // Management is required so we are throwing an exception while registering bean definitions
            if (repositoryScope == Scope.MANAGEMENT) {
                throw new IllegalStateException("No repository type defined in configuration for " + repositoryScope.getName());
            } else {
                return;
            }
        }

        Set<String> repositories = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(Repository.class, beanFactory.getBeanClassLoader()));

        if (repositories.isEmpty()) {
            LOGGER.error("No repository implementation can be found for {}", repositoryScope.getName());

            if (repositoryScope == Scope.MANAGEMENT) {
                throw new IllegalStateException("No repository implementation can be found for {}" + repositoryScope.getName());
            }
        } else {

            int size = repositories.size();
            LOGGER.info("\tFound {} {} implementation(s)", size, Repository.class.getSimpleName());

            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
            Repository repository = null;

            for (String repositoryClass : repositories) {
                try {
                    Class<?> instanceClass = ClassUtils.forName(repositoryClass, beanFactory.getBeanClassLoader());
                    Assert.isAssignable(Repository.class, instanceClass);

                    Repository repositoryInstance = createInstance((Class<Repository>) instanceClass);
                    if (repositoryType.equalsIgnoreCase(repositoryInstance.type())) {
                        repository = repositoryInstance;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unable to instantiate repository: {}", ex);
                    throw new IllegalStateException("Unable to instantiate repository: " + repositoryClass, ex);
                }
            }

            if (repository != null) {
                Class<?> extension = repository.configuration(repositoryScope);
                if (extension != null) {
                    defaultListableBeanFactory.registerBeanDefinition(extension.getName(),
                            new RootBeanDefinition(extension.getName()));

                    LOGGER.info("\tRegistering repository extension: {}", extension.getName());
                }

                configurationClassPostProcessor.processConfigBeanDefinitions(defaultListableBeanFactory);
            } else {
                LOGGER.error("Repository implementation for {} can not be found. Please add correct module in classpath", repositoryType);
                throw new IllegalStateException("Repository implementation for " + repositoryType + " can not be found. Please add correct module in classpath");
            }
        }
    }

    private <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOGGER.error("Unable to instantiate class: {}", ex);
            throw ex;
        }
    }

    public void setConfigurationClassPostProcessor(ConfigurationClassPostProcessor configurationClassPostProcessor) {
        this.configurationClassPostProcessor = configurationClassPostProcessor;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public void setRepositoryScope(Scope repositoryScope) {
        this.repositoryScope = repositoryScope;
    }
}