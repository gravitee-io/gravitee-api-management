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
package io.gravitee.gateway.reactive.handlers.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.el.TemplateVariableProvider;
import io.gravitee.el.TemplateVariableProviderFactory;
import io.gravitee.el.TemplateVariableScope;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.dictionary.EnvironmentDictionaryTemplateVariableProvider;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import io.gravitee.resource.api.ResourceManager;
import java.util.List;
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
class AbstractReactorFactoryTest {

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private DictionaryManager dictionaryManager;

    @Mock
    private ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager;

    @Mock
    private ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager;

    @Mock
    private Reactable reactable;

    private GenericApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        applicationContext = new GenericApplicationContext();
        applicationContext.registerBean(ComponentProvider.class, () -> globalComponentProvider);
        applicationContext.registerBean(DictionaryManager.class, () -> dictionaryManager);
        applicationContext.registerBean(DefaultClassLoader.class, () -> mock(DefaultClassLoader.class));
        applicationContext.registerBean(ResourceClassLoaderFactory.class, () -> mock(ResourceClassLoaderFactory.class));
        registerPluginManager("resourcePluginManager", ResourcePlugin.class, resourcePluginManager);
        registerPluginManager("policyPluginManager", PolicyPlugin.class, policyPluginManager);
    }

    @AfterEach
    void tearDown() {
        applicationContext.close();
    }

    @Test
    void should_register_the_reactable_and_its_resources_in_its_components() {
        var components = cut().createComponents(reactable, Reactable.class, List.of());

        assertThat(components.componentProvider().getComponent(Reactable.class)).isSameAs(reactable);
        assertThat(components.componentProvider().getComponent(ResourceManager.class)).isSameAs(components.resourceLifecycleManager());
    }

    @Test
    void should_fall_back_on_the_global_components() {
        var global = new Object();
        when(globalComponentProvider.getComponent(Object.class)).thenReturn(global);

        var components = cut().createComponents(reactable, Reactable.class, List.of());

        assertThat(components.componentProvider().getComponent(Object.class)).isSameAs(global);
    }

    @Test
    void should_give_the_deployment_context_the_components_and_the_template_variable_providers() {
        var templateVariableProvider = mock(TemplateVariableProvider.class);

        var components = cut().createComponents(reactable, Reactable.class, List.of(templateVariableProvider));

        assertThat(components.deploymentContext().getComponent(Reactable.class)).isSameAs(reactable);
        components.deploymentContext().getTemplateEngine();
        verify(templateVariableProvider).provide(any());
    }

    @Test
    void should_resolve_the_policy_plugin_manager_among_the_plugin_managers() {
        assertThat(cut().policyPluginManager()).isSameAs(policyPluginManager);
    }

    @Test
    void should_provide_the_environment_dictionaries_then_the_reactable_then_the_api_scoped_template_variable_providers() {
        var dictionaries = mock(EnvironmentDictionaryTemplateVariableProvider.class);
        var reactableProvider = mock(TemplateVariableProvider.class);
        var apiScoped = mock(TemplateVariableProvider.class);
        when(dictionaryManager.createTemplateVariableProvider("env")).thenReturn(dictionaries);
        applicationContext.registerBean("api", TemplateVariableProviderFactory.class, () -> factory(TemplateVariableScope.API, apiScoped));
        applicationContext.registerBean("hc", TemplateVariableProviderFactory.class, () ->
            factory(TemplateVariableScope.HEALTH_CHECK, mock(TemplateVariableProvider.class))
        );

        assertThat(cut().environmentTemplateVariableProviders("env", List.of(reactableProvider))).containsExactly(
            dictionaries,
            reactableProvider,
            apiScoped
        );
    }

    private TestReactorFactory cut() {
        applicationContext.refresh();
        return new TestReactorFactory(applicationContext);
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

    private static TemplateVariableProviderFactory factory(TemplateVariableScope scope, TemplateVariableProvider provider) {
        return new TemplateVariableProviderFactory() {
            @Override
            public TemplateVariableScope getTemplateVariableScope() {
                return scope;
            }

            @Override
            public List<TemplateVariableProvider> getTemplateVariableProviders() {
                return List.of(provider);
            }
        };
    }

    private static class TestReactorFactory extends AbstractReactorFactory<Reactable> {

        TestReactorFactory(GenericApplicationContext applicationContext) {
            super(applicationContext);
        }

        @Override
        public boolean support(Class<? extends Reactable> clazz) {
            return true;
        }

        @Override
        public boolean canCreate(Reactable reactable) {
            return true;
        }

        @Override
        public ReactorHandler create(Reactable reactable) {
            return null;
        }
    }
}
