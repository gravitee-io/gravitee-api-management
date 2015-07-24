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
package io.gravitee.gateway.core.reporter.spring;

import io.gravitee.gateway.api.reporter.Reporter;
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
public class ReporterBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

    protected final Logger LOGGER = LoggerFactory.getLogger(ReporterBeanFactoryPostProcessor.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        LOGGER.info("Loading {} implementation from classpath", Reporter.class.getName());

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;

        Set<String> initializerNames = new HashSet(
                SpringFactoriesLoader.loadFactoryNames(Reporter.class, beanFactory.getBeanClassLoader()));

        int implementations = initializerNames.size();
        if (implementations == 0) {
            LOGGER.info("\tNo {} implementation found.", Reporter.class.getName());
        } else {
            LOGGER.info("\tFound {} {} implementations.", initializerNames.size(), Reporter.class.getName());
        }

        for(String reporterClass: initializerNames) {
            try {
                Class<?> instanceClass = ClassUtils.forName(reporterClass, beanFactory.getBeanClassLoader());
                Assert.isAssignable(Reporter.class, instanceClass);

                BeanDefinition beanDefinition =
                        BeanDefinitionBuilder.rootBeanDefinition(instanceClass.getName()).getBeanDefinition();

                LOGGER.info("\tRegistering a new reporter: {}", instanceClass.getName());
                defaultListableBeanFactory.registerBeanDefinition(instanceClass.getName(), beanDefinition);
            } catch (Exception ex) {
                LOGGER.error("Unable to instantiate reporter: {}", ex);
                throw new IllegalStateException("Unable to instantiate reporter: " + reporterClass, ex);
            }
        }
    }
}
