/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.plugin.reactor.internal;

import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.common.service.AbstractService;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.node.plugins.service.ServiceManager;
import io.gravitee.plugin.core.api.AbstractPluginManager;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultReactorPluginManager extends AbstractPluginManager<ReactorPlugin<?>> implements ReactorPluginManager {

    private final ApplicationContext applicationContext;
    private final PluginContextFactory pluginContextFactory;
    private final ReactorFactoryManager factoryManager;
    private final ServiceManager serviceManager;

    public DefaultReactorPluginManager(
        ApplicationContext applicationContext,
        PluginContextFactory pluginContextFactory,
        ReactorFactoryManager factoryManager,
        ServiceManager serviceManager
    ) {
        this.applicationContext = applicationContext;
        this.pluginContextFactory = pluginContextFactory;
        this.factoryManager = factoryManager;
        this.serviceManager = serviceManager;
    }

    @Override
    public void register(ReactorPlugin<?> plugin) {
        super.register(plugin);

        // Create spring application context for the plugin
        final ApplicationContext pluginContext = pluginContextFactory.create(plugin);

        // Retrieve current ReactorPlugin and register it as a singleton in parent application context
        final ReactorFactory<?> reactorFactory = (ReactorFactory<?>) pluginContext.getBean(plugin.clazz());

        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) (
            (ConfigurableApplicationContext) applicationContext
        ).getBeanFactory();
        beanFactory.registerSingleton(plugin.clazz(), reactorFactory);

        factoryManager.register(reactorFactory);
        registerServices(pluginContext, beanFactory);
    }

    private void registerServices(ApplicationContext pluginContext, DefaultListableBeanFactory beanFactory) {
        final Map<String, AbstractService> beans = pluginContext.getBeansOfType(AbstractService.class);
        beans.forEach((name, bean) -> {
            beanFactory.registerSingleton(name, bean);
            serviceManager.register(bean);
        });
    }

    @Override
    public String getIcon(String s) throws IOException {
        return null;
    }

    @Override
    public String getIcon(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String s) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String s, String s1, boolean b, boolean b1) throws IOException {
        return "";
    }

    @Override
    public String getCategory(String s) throws IOException {
        return "reactor";
    }

    @Override
    public String getCategory(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String s) throws IOException {
        return null;
    }

    @Override
    public PluginMoreInformation getMoreInformation(String pluginId, boolean includeNotDeployed) throws IOException {
        return null;
    }
}
