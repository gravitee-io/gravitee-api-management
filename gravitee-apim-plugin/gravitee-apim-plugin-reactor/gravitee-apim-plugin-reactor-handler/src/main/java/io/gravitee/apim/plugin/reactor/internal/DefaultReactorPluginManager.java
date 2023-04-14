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
package io.gravitee.apim.plugin.reactor.internal;

import io.gravitee.apim.plugin.reactor.ReactorClassLoaderFactory;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.apim.plugin.reactor.ReactorPluginManager;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.plugin.core.api.AbstractPluginManager;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.core.api.PluginMoreInformation;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class DefaultReactorPluginManager extends AbstractPluginManager<ReactorPlugin<?>> implements ReactorPluginManager {

    private final ReactorClassLoaderFactory classLoaderFactory;
    private final ApplicationContext applicationContext;

    public DefaultReactorPluginManager(ApplicationContext applicationContext, ReactorClassLoaderFactory reactorClassLoaderFactory) {
        this.classLoaderFactory = reactorClassLoaderFactory;
        this.applicationContext = applicationContext;
    }

    @Override
    public void register(ReactorPlugin<?> plugin) {
        super.register(plugin);

        final ReactorFactoryManager factoryManager = applicationContext.getBean(ReactorFactoryManager.class);

        // Create engine
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<ReactorFactory<?>> engineFactoryClass = (Class<ReactorFactory<?>>) pluginClassLoader.loadClass(plugin.clazz());
            ReactorFactory<?> factory = registerFactoryBean(engineFactoryClass);
            factoryManager.register(factory);
        } catch (Exception e) {
            log.error("Unexpected error while loading engine plugin: {}", plugin.clazz(), e);
        }
    }

    private ReactorFactory<?> registerFactoryBean(final Class<ReactorFactory<?>> reactorFactoryClass) {
        return applicationContext.getAutowireCapableBeanFactory().createBean(reactorFactoryClass);
    }

    @Override
    public String getIcon(String s) throws IOException {
        return null;
    }

    @Override
    public String getDocumentation(String s) throws IOException {
        return null;
    }

    @Override
    public String getCategory(String s) throws IOException {
        return "reactor";
    }

    @Override
    public PluginMoreInformation getMoreInformation(String s) throws IOException {
        return null;
    }
}
