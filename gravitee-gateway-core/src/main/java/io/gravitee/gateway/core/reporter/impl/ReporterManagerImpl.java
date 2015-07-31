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
import io.gravitee.gateway.core.plugin.PluginContextFactory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
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
public class ReporterManagerImpl implements ReporterManager, PluginHandler {

    protected final Logger LOGGER = LoggerFactory.getLogger(ReporterManagerImpl.class);

    private final Collection<Reporter> reporters = new ArrayList<>();
    private final Map<String, ApplicationContext> contexts = new HashMap<>();

    @Autowired
    private PluginContextFactory pluginContextFactory;

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

            ApplicationContext context = pluginContextFactory.create(plugin);
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
}
