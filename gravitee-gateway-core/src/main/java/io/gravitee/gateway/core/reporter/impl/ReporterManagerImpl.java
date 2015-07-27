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
package io.gravitee.gateway.core.reporter.impl;

import io.gravitee.gateway.api.reporter.Reporter;
import io.gravitee.gateway.core.plugin.Plugin;
import io.gravitee.gateway.core.plugin.PluginHandler;
import io.gravitee.gateway.core.reporter.ReporterManager;
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
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class ReporterManagerImpl implements ReporterManager, PluginHandler, ApplicationContextAware {

    protected final Logger LOGGER = LoggerFactory.getLogger(ReporterManagerImpl.class);

    private final Collection<Reporter> reporters = new ArrayList<>();
    private final Map<String, ApplicationContext> contexts = new HashMap<>();

    private ApplicationContext applicationContext;

    @Override
    public Collection<Reporter> getReporters() {
        return reporters;
    }

    @Override
    public boolean canHandle(Plugin plugin) {
        try {
            Assert.isAssignable(Reporter.class, plugin.clazz());
            return true;
        } catch (Exception iae) {
            return false;
        }
    }

    @Override
    public void handle(Plugin plugin) {
        try {
            Assert.isAssignable(Reporter.class, plugin.clazz());

            ApplicationContext context = createPluginContext(plugin);
            contexts.putIfAbsent(plugin.id(), context);
            reporters.add(context.getBean(Reporter.class));
        } catch (Exception iae) {
            LOGGER.error("Unexpected error while create reporter instance", iae);
            // Be sure that the context does not exist anymore.
            ApplicationContext ctx =  contexts.remove(plugin.id());
            if (ctx != null) {
                ((ConfigurableApplicationContext)ctx).close();
            }
        }
    }

    private ApplicationContext createPluginContext(Plugin plugin) {
        LOGGER.info("Create plugin Spring context for {}", plugin.id());

        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addClassLoader(plugin.clazz().getClassLoader())
                .setUrls(ClasspathHelper.forClass(plugin.clazz(), plugin.clazz().getClassLoader()))
                .setScanners(new SubTypesScanner(false), new TypeAnnotationsScanner())
                .filterInputsBy(new FilterBuilder().includePackage(plugin.clazz().getPackage().getName())));

        LOGGER.info("Looking for @Configuration annotated class in {}", plugin.clazz().getPackage().getName());
        Set<Class<?>> configurations =
                reflections.getTypesAnnotatedWith(Configuration.class);

        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext.setClassLoader(plugin.clazz().getClassLoader());
        pluginContext.setParent(applicationContext);

        LOGGER.info("\tNo @Configuration annotated class found for plugin {}", plugin.id());
        BeanDefinition beanDefinition =
                BeanDefinitionBuilder.rootBeanDefinition(plugin.clazz().getName()).getBeanDefinition();

        LOGGER.info("\tRegistering a new reporter: {}", plugin.clazz().getName());
        pluginContext.registerBeanDefinition(plugin.clazz().getName(), beanDefinition);

        LOGGER.info("\t{} Spring @Configuration annotated class found for plugin {}", configurations.size(), plugin.id());
        configurations.forEach(pluginContext::register);

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(plugin.clazz().getClassLoader());
            pluginContext.refresh();
        } catch (Exception ex) {
            LOGGER.error("Unable to refresh plugin Spring context", ex);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        return pluginContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
