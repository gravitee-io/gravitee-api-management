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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.EnvironmentDictionaryTemplateVariableProvider;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.resource.internal.v4.DefaultResourceManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultApiProductResourceManagerFactoryTest {

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private DictionaryManager dictionaryManager;

    @Mock
    private ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager;

    private GenericApplicationContext applicationContext;
    private DefaultApiProductResourceManagerFactory factory;

    @BeforeEach
    void setUp() {
        applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(ComponentProvider.class, () -> globalComponentProvider);
        applicationContext.registerBean(DictionaryManager.class, () -> dictionaryManager);
        applicationContext.registerBean(DefaultClassLoader.class, () -> new DefaultClassLoader());
        applicationContext.registerBean(ResourceClassLoaderFactory.class, () -> mock(ResourceClassLoaderFactory.class));
        registerPluginManager("resourcePluginManager", ResourcePlugin.class, resourcePluginManager);
        applicationContext.refresh();
        factory = new DefaultApiProductResourceManagerFactory(applicationContext);
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void should_build_resource_manager_from_gateway_environment_beans() {
        ReactableApiProduct apiProduct = ReactableApiProduct.builder().id("product-1").environmentId("env-1").build();

        var manager = factory.create(apiProduct);

        assertThat(manager).isInstanceOf(DefaultResourceManager.class);
    }

    @Test
    void should_register_the_product_in_its_resource_manager_components() {
        ReactableApiProduct apiProduct = ReactableApiProduct.builder().id("product-1").environmentId("env-1").build();

        var manager = factory.create(apiProduct);

        assertThat(manager).extracting("reactable").isSameAs(apiProduct);
    }

    @Test
    void should_wire_environment_template_variable_providers_when_building_the_resource_manager() {
        var dictionaries = mock(EnvironmentDictionaryTemplateVariableProvider.class);
        when(dictionaryManager.createTemplateVariableProvider("env-1")).thenReturn(dictionaries);
        ReactableApiProduct apiProduct = ReactableApiProduct.builder().id("product-1").environmentId("env-1").build();

        factory.create(apiProduct);

        verify(dictionaryManager).createTemplateVariableProvider("env-1");
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerPluginManager(String name, Class<?> pluginType, ConfigurablePluginManager<?> pluginManager) {
        applicationContext.registerBean(
            name,
            ConfigurablePluginManager.class,
            () -> pluginManager,
            beanDefinition ->
                ((RootBeanDefinition) beanDefinition).setTargetType(
                    ResolvableType.forClassWithGenerics(ConfigurablePluginManager.class, pluginType)
                )
        );
    }
}
