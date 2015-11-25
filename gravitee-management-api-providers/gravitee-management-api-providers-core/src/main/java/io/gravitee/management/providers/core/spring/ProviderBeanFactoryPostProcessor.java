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
package io.gravitee.management.providers.core.spring;

import io.gravitee.management.providers.core.Provider;
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
public class ProviderBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderBeanFactoryPostProcessor.class);

    private ConfigurationClassPostProcessor configurationClassPostProcessor;

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOGGER.info("Loading Management providers {}", Provider.class.getSimpleName());

        Set<String> providers = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(Provider.class, beanFactory.getBeanClassLoader()));

            LOGGER.info("\tFound {} {} implementation(s)",
                    providers.size(), Provider.class.getSimpleName());

            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

            for (String provider : providers) {
                try {
                    Class<?> instanceClass = ClassUtils.forName(provider, beanFactory.getBeanClassLoader());
                    Assert.isAssignable(Provider.class, instanceClass);

                    Provider providerInstance =
                            createInstance((Class<Provider>) instanceClass);

                    LOGGER.info("Registering a provider {} [{}]", instanceClass.getSimpleName(),
                            providerInstance.type());

                    if (providerInstance.authenticationManager() != null) {
                        defaultListableBeanFactory.registerBeanDefinition(
                                providerInstance.authenticationManager().getName(),
                                new RootBeanDefinition(providerInstance.authenticationManager().getName()));
                    }

                    if (providerInstance.identityManager() != null) {
                        defaultListableBeanFactory.registerBeanDefinition(
                                providerInstance.identityManager().getName(),
                                new RootBeanDefinition(providerInstance.identityManager().getName()));
                    }

                    Class<?> extension = providerInstance.configuration();
                    if (extension != null) {
                        LOGGER.info("\tRegistering provider extension: {}", extension.getName());
                        defaultListableBeanFactory.registerBeanDefinition(extension.getName(),
                                new RootBeanDefinition(extension.getName()));

                        LOGGER.info("\tLoading @Configuration from previous extension {}", extension.getName());
                        configurationClassPostProcessor.processConfigBeanDefinitions(defaultListableBeanFactory);
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unable to instantiate provider: {}", ex);
                    throw new IllegalStateException("Unable to instantiate provider: " + provider, ex);
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
}