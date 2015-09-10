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

import java.util.HashSet;
import java.util.Set;

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

import io.gravitee.repository.Repository;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RepositoryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    protected final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanFactoryPostProcessor.class);

    private String repositoryType;

    private ConfigurationClassPostProcessor configurationClassPostProcessor;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOGGER.info("Looking for a repository implementation");

        if (repositoryType == null || repositoryType.isEmpty()) {
            LOGGER.error("No repository.type defined in configuration");
            throw new IllegalStateException("No repository.type defined in configuration");
        }

        Set<String> repositories = new HashSet(
                SpringFactoriesLoader.loadFactoryNames(Repository.class, beanFactory.getBeanClassLoader()));

        if (repositories.isEmpty()) {
            throw new IllegalStateException("No repository implementation can be found");
        }

        int size = repositories.size();
        LOGGER.info("\tFound {} {} implementation(s):{}", size, Repository.class.getSimpleName(), repositories);

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
        Repository repository  = null;

        for(String repositoryClass : repositories) {
            try {
                Class<?> instanceClass = ClassUtils.forName(repositoryClass, beanFactory.getBeanClassLoader());
                Assert.isAssignable(Repository.class, instanceClass);

                Repository repositoryInstance = createInstance((Class<Repository>)instanceClass);
                if (repositoryType.equalsIgnoreCase(repositoryInstance.type())) {
                    repository = repositoryInstance;
                }
            }
            catch (Exception ex) {
                LOGGER.error("Unable to instantiate repository: {}", ex);
                throw new IllegalStateException("Unable to instantiate repository: " + repositoryClass, ex);
            }
        }

        if (repository != null) {
            Class<?> [] extensions = repository.configurations();
            if (extensions != null) {
                for(Class<?> extension : extensions) {
                    defaultListableBeanFactory.registerBeanDefinition(extension.getName(),
                            new RootBeanDefinition(extension.getName()));

                    LOGGER.info("\tRegistering repository extension: {}", extension.getName());
                }
            }

            configurationClassPostProcessor.processConfigBeanDefinitions(defaultListableBeanFactory);
        } else {
            LOGGER.error("Repository implementation for {} can not be found. Please add correct module in classpath", repositoryType);
            throw new IllegalStateException("Repository implementation for " + repositoryType + " can not be found. Please add correct module in classpath");
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
}