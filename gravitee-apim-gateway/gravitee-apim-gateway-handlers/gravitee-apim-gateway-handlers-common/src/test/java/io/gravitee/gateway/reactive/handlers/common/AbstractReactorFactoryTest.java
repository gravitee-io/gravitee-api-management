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
import static org.mockito.Mockito.lenient;
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
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.resource.api.ResourceManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;

@ExtendWith(MockitoExtension.class)
class AbstractReactorFactoryTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ComponentProvider globalComponentProvider;

    @Mock
    private Reactable reactable;

    private TestReactorFactory cut;

    @BeforeEach
    void setUp() {
        lenient().when(applicationContext.getBean(ComponentProvider.class)).thenReturn(globalComponentProvider);
        lenient().when(applicationContext.getBean(DefaultClassLoader.class)).thenReturn(mock(DefaultClassLoader.class));
        lenient().when(applicationContext.getBean(ResourceClassLoaderFactory.class)).thenReturn(mock(ResourceClassLoaderFactory.class));
        lenient().when(applicationContext.getBean("resourcePluginManager")).thenReturn(mock(ConfigurablePluginManager.class));
        cut = new TestReactorFactory(applicationContext);
    }

    @Test
    void should_register_the_reactable_and_its_resources_in_its_components() {
        var components = cut.createComponents(reactable, Reactable.class, List.of());

        assertThat(components.componentProvider().getComponent(Reactable.class)).isSameAs(reactable);
        assertThat(components.componentProvider().getComponent(ResourceManager.class)).isSameAs(components.resourceLifecycleManager());
    }

    @Test
    void should_fall_back_on_the_global_components() {
        var global = new Object();
        when(globalComponentProvider.getComponent(Object.class)).thenReturn(global);

        var components = cut.createComponents(reactable, Reactable.class, List.of());

        assertThat(components.componentProvider().getComponent(Object.class)).isSameAs(global);
    }

    @Test
    void should_give_the_deployment_context_the_components_and_the_template_variable_providers() {
        var templateVariableProvider = mock(TemplateVariableProvider.class);

        var components = cut.createComponents(reactable, Reactable.class, List.of(templateVariableProvider));

        assertThat(components.deploymentContext().getComponent(Reactable.class)).isSameAs(reactable);
        components.deploymentContext().getTemplateEngine();
        verify(templateVariableProvider).provide(any());
    }

    @Test
    void should_provide_the_environment_dictionaries_and_the_api_scoped_template_variable_providers() {
        var dictionaryManager = mock(DictionaryManager.class);
        var dictionaries = mock(EnvironmentDictionaryTemplateVariableProvider.class);
        var apiScoped = mock(TemplateVariableProvider.class);
        var healthCheckScoped = mock(TemplateVariableProvider.class);
        when(applicationContext.getBean(DictionaryManager.class)).thenReturn(dictionaryManager);
        when(dictionaryManager.createTemplateVariableProvider("env")).thenReturn(dictionaries);
        when(applicationContext.getBeanNamesForType(TemplateVariableProviderFactory.class)).thenReturn(new String[] { "api", "hc" });
        when(applicationContext.getBean("api")).thenReturn(factory(TemplateVariableScope.API, apiScoped));
        when(applicationContext.getBean("hc")).thenReturn(factory(TemplateVariableScope.HEALTH_CHECK, healthCheckScoped));

        assertThat(cut.environmentTemplateVariableProviders("env")).containsExactly(dictionaries, apiScoped);
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

        TestReactorFactory(ApplicationContext applicationContext) {
            super(applicationContext);
        }

        @Override
        protected String[] getBeanNamesForType(ResolvableType resolvableType) {
            return new String[] { "resourcePluginManager" };
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
