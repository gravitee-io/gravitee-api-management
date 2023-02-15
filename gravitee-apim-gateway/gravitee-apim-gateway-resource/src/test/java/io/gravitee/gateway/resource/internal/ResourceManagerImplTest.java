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
package io.gravitee.gateway.resource.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.gateway.core.classloader.DefaultClassLoader;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.resource.ResourceConfigurationFactory;
import io.gravitee.gateway.resource.internal.v4.fake.ApplicationAwareFake;
import io.gravitee.gateway.resource.internal.v4.fake.Fake;
import io.gravitee.gateway.resource.internal.v4.fake.FakeConfiguration;
import io.gravitee.gateway.resource.internal.v4.fake.FakeWithDeploymentContext;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourceClassLoaderFactory;
import io.gravitee.plugin.resource.ResourcePlugin;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ResourceManagerImplTest {

    private DefaultClassLoader classLoader;

    @Mock
    private Reactable reactable;

    @Mock
    private ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager;

    @Mock
    private ResourceClassLoaderFactory resourceClassLoaderFactory;

    @Mock
    private ResourceConfigurationFactory resourceConfigurationFactory;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DeploymentContext deploymentContext;

    private ResourceManagerImpl cut;

    @BeforeEach
    void init() {
        classLoader = new DefaultClassLoader(this.classLoader);

        cut =
            new ResourceManagerImpl(
                false,
                classLoader,
                reactable,
                resourcePluginManager,
                resourceClassLoaderFactory,
                resourceConfigurationFactory,
                applicationContext,
                deploymentContext
            );
    }

    @Test
    void should_not_load_any_resource_when_reactable_does_not_have_resource() {
        when(reactable.dependencies(Resource.class)).thenReturn(Collections.emptySet());
        cut.initialize();

        verifyNoInteractions(resourcePluginManager, resourceClassLoaderFactory, resourceConfigurationFactory, applicationContext);
    }

    @Test
    void should_throw_an_illegal_state_exception_when_plugin_not_found_in_the_registry() {
        final Resource resource = buildResource();
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));

        assertThrows(IllegalStateException.class, () -> cut.initialize());

        assertThat(cut.containsResource(resource.getName())).isFalse();
        verify(resourcePluginManager).get(resource.getType());
        verifyNoInteractions(resourceClassLoaderFactory, resourceConfigurationFactory, applicationContext);
    }

    @Test
    void should_ignore_and_return_null_when_class_not_found_occurred() {
        final Resource resource = buildResource();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        // Set up an NPE.
        when(resourcePlugin.resource()).thenReturn(Fake.class, null, Fake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        cut.initialize();
        assertThat(cut.containsResource(resource.getName())).isFalse();
    }

    @Test
    void should_ignore_and_return_null_when_failing_to_remove_class_loader_after_an_exception() throws IOException {
        final Resource resource = buildResource();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        // Set up an NPE.
        when(resourcePlugin.resource()).thenReturn(Fake.class, null, Fake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        final DefaultClassLoader spy = spy(classLoader);

        doThrow(new IOException("Mock exception")).when(spy).removeClassLoader(anyString());

        cut =
            new ResourceManagerImpl(
                false,
                spy,
                reactable,
                resourcePluginManager,
                resourceClassLoaderFactory,
                resourceConfigurationFactory,
                applicationContext,
                deploymentContext
            );

        cut.initialize();
        assertThat(cut.containsResource(resource.getName())).isFalse();
    }

    @Test
    void should_not_ignore_when_resource_is_disabled() {
        final Resource resource = buildResource();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);
        resource.setEnabled(false);

        when(resourcePlugin.resource()).thenReturn(Fake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        cut.initialize();

        assertThat(cut.containsResource(resource.getName())).isTrue();
        assertThat(cut.getResource(resource.getName())).isExactlyInstanceOf(Fake.class);
    }

    @Test
    void should_initialize_resource() {
        final Resource resource = buildResource();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        when(resourcePlugin.resource()).thenReturn(Fake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        cut.initialize();

        assertThat(cut.containsResource(resource.getName())).isTrue();
        assertThat(cut.getResource(resource.getName())).isExactlyInstanceOf(Fake.class);
    }

    @Test
    void should_initialize_resource_with_configuration() {
        final Resource resource = buildResource();
        final FakeConfiguration fakeConfiguration = new FakeConfiguration();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        when(resourcePlugin.resource()).thenReturn(Fake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);
        when(resourcePlugin.configuration()).thenReturn(FakeConfiguration.class);
        when(resourceConfigurationFactory.create(FakeConfiguration.class, resource.getConfiguration())).thenReturn(fakeConfiguration);

        cut.initialize();

        assertThat(cut.containsResource(resource.getName())).isTrue();

        final Object r = cut.getResource(resource.getName());
        assertThat(r).isExactlyInstanceOf(Fake.class);

        assertThat(((Fake) r).configuration()).isSameAs(fakeConfiguration);
    }

    @Test
    void should_initialize_application_aware_resource() {
        final Resource resource = buildResource();
        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        when(resourcePlugin.resource()).thenReturn(ApplicationAwareFake.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        cut.initialize();
        assertThat(cut.containsResource(resource.getName())).isTrue();

        final Object r = cut.getResource(resource.getName());
        assertThat(r).isExactlyInstanceOf(ApplicationAwareFake.class);

        assertThat(((ApplicationAwareFake) r).getContext()).isSameAs(applicationContext);
    }

    @Test
    void should_initialize_resource_that_use_deployment_context() {
        final Resource resource = buildResource();

        final ResourcePlugin resourcePlugin = mock(ResourcePlugin.class);

        when(resourcePlugin.resource()).thenReturn(FakeWithDeploymentContext.class);
        when(reactable.dependencies(Resource.class)).thenReturn(Set.of(resource));
        when(resourcePluginManager.get(resource.getType())).thenReturn(resourcePlugin);

        cut.initialize();
        assertThat(cut.containsResource(resource.getName())).isTrue();

        final Object r = cut.getResource(resource.getName());
        assertThat(r).isExactlyInstanceOf(FakeWithDeploymentContext.class);

        assertThat(((FakeWithDeploymentContext) r).getDeploymentContext()).isNotNull().isSameAs(deploymentContext);
    }

    private Resource buildResource() {
        final Resource resource = new Resource();
        resource.setType("test");
        resource.setConfiguration("{}");
        resource.setName("My resource");
        resource.setEnabled(true);
        return resource;
    }
}
