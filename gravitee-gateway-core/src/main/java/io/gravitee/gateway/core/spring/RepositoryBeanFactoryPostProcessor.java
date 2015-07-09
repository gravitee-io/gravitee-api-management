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

import io.gravitee.gateway.api.Repository;
import io.gravitee.gateway.core.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class RepositoryBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    protected final Logger LOGGER = LoggerFactory.getLogger(RepositoryBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOGGER.info("Looking for a repository {} implementation...", Repository.class.getSimpleName());

        Set<String> initializerNames = new HashSet(
                SpringFactoriesLoader.loadFactoryNames(Repository.class, beanFactory.getBeanClassLoader()));

        int size = initializerNames.size();
        LOGGER.info("   Found {} {} implementations.", size, Repository.class.getSimpleName());

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

        if (size == 0 || size == 1) {
            String repositoryClass = null;

            switch (size) {
                case 0:
                    LOGGER.info("   No repository implementation found, registering default file implementation.");
                    repositoryClass = FileRepository.class.getName();
                    break;
                case 1:
                    repositoryClass = initializerNames.iterator().next();
            }

            try {
                Class<?> instanceClass = ClassUtils.forName(repositoryClass, beanFactory.getBeanClassLoader());
                Assert.isAssignable(Repository.class, instanceClass);

                BeanDefinition beanDefinition =
                        BeanDefinitionBuilder.rootBeanDefinition(instanceClass.getName()).getBeanDefinition();

                LOGGER.info("   Registering repository implementation: {}", instanceClass.getName());
                defaultListableBeanFactory.registerBeanDefinition("repository", beanDefinition);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate Repository: " + repositoryClass, e);
            }
        }

    }
}
