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
package io.gravitee.gateway.core.http.client.spring;

import io.gravitee.gateway.api.http.client.HttpClient;
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
public class HttpClientBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(HttpClientBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOGGER.debug("Looking for an HTTP Client implementation");

        Set<String> httpClients = new HashSet<>(
                SpringFactoriesLoader.loadFactoryNames(HttpClient.class, beanFactory.getBeanClassLoader()));

        if (httpClients.isEmpty()) {
            LOGGER.error("No HTTP client implementation can be found !");
            throw new IllegalStateException("No HTTP client implementation can be found !");
        } else {
            int size = httpClients.size();
            LOGGER.debug("\tFound {} {} implementation(s)", size, HttpClient.class.getSimpleName());

            DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
            HttpClient httpClient = null;

            for (String httpClientClass : httpClients) {
                try {
                    Class<?> instanceClass = ClassUtils.forName(httpClientClass, beanFactory.getBeanClassLoader());
                    Assert.isAssignable(HttpClient.class, instanceClass);

                    httpClient = createInstance((Class<HttpClient>) instanceClass);
                } catch (Exception ex) {
                    LOGGER.error("Unable to instantiate HTTP client: {}", ex);
                    throw new IllegalStateException("Unable to instantiate HTTP client: " + httpClientClass, ex);
                }
            }

            if (httpClient != null) {
                    defaultListableBeanFactory.registerBeanDefinition(httpClient.getClass().getName(),
                            new RootBeanDefinition(httpClient.getClass().getName()));

                    LOGGER.info("Register HTTP client: {}", httpClient.getClass().getName());

            } else {
                LOGGER.error("HTTP Client implementation can not be found. Please add correct module in classpath");
                throw new IllegalStateException("HTTP Client implementation can not be found. Please add correct module in classpath");
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
