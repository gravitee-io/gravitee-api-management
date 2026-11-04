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
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.manager.ApiProductResourceManagerFactory;
import io.gravitee.gateway.reactive.core.context.DefaultDeploymentContext;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.gateway.resource.internal.ResourceConfigurationFactoryImpl;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ResolvableType;

@RequiredArgsConstructor
public class DefaultApiProductResourceManagerFactory implements ApiProductResourceManagerFactory {

    private final ApplicationContext applicationContext;
    private final ComponentProvider componentProvider;

    @Override
    @SuppressWarnings("unchecked")
    public ResourceLifecycleManager create(ReactableApiProduct apiProduct) {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            ((ConfigurableApplicationContext) applicationContext).getBeanFactory(),
            ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, ResourcePlugin.class)
        );

        ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager = (ConfigurablePluginManager<
            ResourcePlugin<?>
        >) applicationContext.getBean(beanNamesForType[0]);

        DefaultDeploymentContext deploymentContext = new DefaultDeploymentContext();
        deploymentContext.componentProvider(componentProvider);

        return new DefaultResourceManager(
            applicationContext.getBean(DefaultClassLoader.class),
            apiProduct,
            resourcePluginManager,
            applicationContext.getBean(ResourceClassLoaderFactory.class),
            new ResourceConfigurationFactoryImpl(),
            applicationContext,
            deploymentContext
        );
    }
}
