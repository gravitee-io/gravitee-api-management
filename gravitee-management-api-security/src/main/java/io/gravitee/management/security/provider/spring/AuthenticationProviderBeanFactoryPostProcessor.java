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
package io.gravitee.management.security.provider.spring;

import io.gravitee.management.security.provider.AuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AuthenticationProviderBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationProviderBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Set<String> authenticationProviders = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(AuthenticationProvider.class, beanFactory.getBeanClassLoader()));

            LOGGER.info("\tFound {} {} implementation(s)",
                    authenticationProviders.size(), AuthenticationProvider.class.getSimpleName());

            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

            for (String authenticationProviderClass : authenticationProviders) {
                try {
                    Class<?> instanceClass = ClassUtils.forName(authenticationProviderClass, beanFactory.getBeanClassLoader());
                    Assert.isAssignable(AuthenticationProvider.class, instanceClass);

                    AuthenticationProvider authenticationProviderInstance =
                            createInstance((Class<AuthenticationProvider>) instanceClass);

                    LOGGER.info("Registering an authentication provider {} [{}]", instanceClass.getSimpleName(),
                            authenticationProviderInstance.type());

                    defaultListableBeanFactory.registerBeanDefinition(authenticationProviderInstance.getClass().getName(),
                            new RootBeanDefinition(authenticationProviderInstance.getClass().getName()));
                } catch (Exception ex) {
                    LOGGER.error("Unable to instantiate authentication provider: {}", ex);
                    throw new IllegalStateException("Unable to instantiate authentication provider: " + authenticationProviderClass, ex);
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
}