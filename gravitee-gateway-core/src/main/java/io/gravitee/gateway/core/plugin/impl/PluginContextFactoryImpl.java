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
package io.gravitee.gateway.core.plugin.impl;

import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.plugin.PluginContextFactory;
import io.gravitee.plugin.api.Plugin;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PluginContextFactoryImpl implements PluginContextFactory, ApplicationContextAware {

    protected final Logger LOGGER = LoggerFactory.getLogger(PluginContextFactoryImpl.class);

    private final Map<Plugin, ApplicationContext> contexts = new HashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public ApplicationContext create(Plugin plugin) {
        LOGGER.info("Create Spring context for plugin: {}", plugin.id());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addClassLoader(plugin.clazz().getClassLoader())
                .setUrls(ClasspathHelper.forClass(plugin.clazz(), plugin.clazz().getClassLoader()))
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage(plugin.clazz().getPackage().getName())));

        LOGGER.info("Looking for @Configuration annotated class in package {}", plugin.clazz().getPackage().getName());
        Set<Class<?>> configurations =
                reflections.getTypesAnnotatedWith(Configuration.class);

        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext.setClassLoader(plugin.clazz().getClassLoader());
        pluginContext.setEnvironment((ConfigurableEnvironment) applicationContext.getEnvironment());

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setIgnoreUnresolvablePlaceholders(true);
        configurer.setEnvironment(applicationContext.getEnvironment());
        pluginContext.addBeanFactoryPostProcessor(configurer);

        if (configurations.isEmpty()) {
            LOGGER.info("\tNo @Configuration annotated class found for plugin {}", plugin.id());
        } else {
            LOGGER.info("\t{} Spring @Configuration annotated class found for plugin {}", configurations.size(), plugin.id());
            configurations.forEach(pluginContext::register);
        }

        // Only reporter can be inject by Spring
        if (Reporter.class.isAssignableFrom(plugin.clazz())) {
            BeanDefinition beanDefinition =
                    BeanDefinitionBuilder.rootBeanDefinition(plugin.clazz().getName()).getBeanDefinition();

            LOGGER.info("\tRegistering a new reporter bean definition: {}", plugin.clazz().getName());
            pluginContext.registerBeanDefinition(plugin.clazz().getName(), beanDefinition);
        }

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(plugin.clazz().getClassLoader());
            pluginContext.refresh();
        } catch (Exception ex) {
            LOGGER.error("Unable to refresh plugin Spring context", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        contexts.putIfAbsent(plugin, pluginContext);

        return pluginContext;
    }

    @Override
    public ApplicationContext get(Plugin plugin) {
        return contexts.get(plugin);
    }

    @Override
    public void remove(Plugin plugin) {
        ApplicationContext ctx =  contexts.remove(plugin);
        if (ctx != null) {
            ((ConfigurableApplicationContext)ctx).close();
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
