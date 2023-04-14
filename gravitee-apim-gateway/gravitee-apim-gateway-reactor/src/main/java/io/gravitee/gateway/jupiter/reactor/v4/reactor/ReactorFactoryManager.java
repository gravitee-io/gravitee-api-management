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
package io.gravitee.gateway.jupiter.reactor.v4.reactor;

import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.plugin.core.api.PluginClassLoader;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class ReactorFactoryManager {

    private final ReactorClassLoaderFactory classLoaderFactory;
    private final Map<String, ReactorFactory> factories = new ConcurrentHashMap<>();
    private final ApplicationContext context;

    public ReactorFactoryManager(ApplicationContext context) {
        this.context = context;
        this.classLoaderFactory = new ReactorClassLoaderFactory();
    }

    public void register(final ReactorPlugin<?> plugin) {
        // Create endpoint
        PluginClassLoader pluginClassLoader = classLoaderFactory.getOrCreateClassLoader(plugin);
        try {
            final Class<ReactorFactory<?>> connectorFactoryClass = (Class<ReactorFactory<?>>) pluginClassLoader.loadClass(plugin.clazz());
            ReactorFactory<?> factory = createFactory(connectorFactoryClass);
            factories.put(plugin.id(), factory);
        } catch (Exception ex) {
            log.error("Unexpected error while loading endpoint plugin: {}", plugin.clazz(), ex);
        }
    }

    private ReactorFactory<?> createFactory(final Class<ReactorFactory<?>> factoryClass)
        throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
       return context.getAutowireCapableBeanFactory().createBean(factoryClass);
    }

    public List<ReactorHandler> create(final Reactable reactable) {
        if (reactable != null) {
            return factories
                .values()
                .stream()
                .filter(reactorFactory -> reactorFactory.support(reactable.getClass()))
                .filter(reactorFactory -> reactorFactory.canCreate(reactable))
                .map(reactorFactory -> reactorFactory.create(reactable))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
