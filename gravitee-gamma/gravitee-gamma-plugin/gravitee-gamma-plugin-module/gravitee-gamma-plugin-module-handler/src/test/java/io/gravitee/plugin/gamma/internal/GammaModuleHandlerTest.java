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
package io.gravitee.plugin.gamma.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.plugin.api.PluginDeploymentContext;
import io.gravitee.plugin.api.PluginDeploymentContextFactory;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginClassLoader;
import io.gravitee.plugin.core.api.PluginClassLoaderFactory;
import io.gravitee.plugin.core.api.PluginContextFactory;
import io.gravitee.plugin.core.api.PluginManifest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class GammaModuleHandlerTest {

    private GammaModulePluginHandler handler;
    private GammaModuleManager manager;
    private PluginContextFactory pluginContextFactory;
    private PluginClassLoaderFactory<Plugin> pluginClassLoaderFactory;
    private GenericApplicationContext applicationContext;
    private Environment environment;
    private PluginDeploymentContextFactory pluginDeploymentContextFactory;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        handler = new GammaModulePluginHandler();
        handler.gammaEnabled = true;

        // Create a fresh manager instance for each test to avoid singleton state leaking
        Constructor<GammaModuleManager> ctor = GammaModuleManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        manager = ctor.newInstance();

        pluginContextFactory = mock(PluginContextFactory.class);
        pluginClassLoaderFactory = mock(PluginClassLoaderFactory.class);
        applicationContext = mock(GenericApplicationContext.class);
        environment = mock(Environment.class);
        pluginDeploymentContextFactory = mock(PluginDeploymentContextFactory.class);

        // Inject GammaModuleHandler fields
        setField(GammaModulePluginHandler.class, "gammaModuleManager", manager);
        setField(GammaModulePluginHandler.class, "pluginContextFactory", pluginContextFactory);
        setField(GammaModulePluginHandler.class, "pluginClassLoaderFactory", pluginClassLoaderFactory);
        setField(GammaModulePluginHandler.class, "applicationContext", applicationContext);

        // Inject AbstractPluginHandler fields
        setField(handler.getClass().getSuperclass(), "environment", environment);
        setField(handler.getClass().getSuperclass(), "pluginDeploymentContextFactory", pluginDeploymentContextFactory);

        // Default: plugins are enabled
        when(environment.getProperty(anyString(), eq(Boolean.class), eq(true))).thenReturn(true);

        // Default: plugins are deployable
        PluginDeploymentContext deploymentContext = mock(PluginDeploymentContext.class);
        when(deploymentContext.isPluginDeployable(any())).thenReturn(true);
        when(pluginDeploymentContextFactory.create()).thenReturn(deploymentContext);

        // Default: managementMongoTemplate bean provides a classloader for plugin classloader factory.
        // Use a non-JDK object so getClass().getClassLoader() returns a non-null classloader.
        when(applicationContext.getBean("managementMongoTemplate")).thenReturn(applicationContext);
        PluginClassLoader pluginClassLoader = mock(PluginClassLoader.class);
        when(pluginClassLoaderFactory.getOrCreateClassLoader(any(Plugin.class), any(ClassLoader.class))).thenReturn(pluginClassLoader);
        when(pluginClassLoader.loadClass(anyString())).thenAnswer(invocation ->
            getClass().getClassLoader().loadClass(invocation.getArgument(0))
        );
    }

    private void setField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(handler, value);
    }

    @Test
    void should_handle_gamma_module_type() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("gamma-module");

        assertThat(handler.canHandle(plugin)).isTrue();
    }

    @Test
    void should_handle_gamma_module_type_case_insensitive() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("Gamma-Module");

        assertThat(handler.canHandle(plugin)).isTrue();
    }

    @Test
    void should_not_handle_other_plugin_types() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("policy");

        assertThat(handler.canHandle(plugin)).isFalse();
    }

    @Test
    void should_not_handle_endpoint_connector_type() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("endpoint-connector");

        assertThat(handler.canHandle(plugin)).isFalse();
    }

    @Test
    void should_register_plugin_in_manager() {
        Plugin plugin = mock(Plugin.class);
        PluginManifest manifest = mock(PluginManifest.class);
        when(plugin.id()).thenReturn("chat");
        when(plugin.type()).thenReturn("gamma-module");
        when(plugin.manifest()).thenReturn(manifest);
        when(plugin.clazz()).thenReturn(AGammaModule.class.getName());
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);

        handler.handle(plugin);

        assertThat(manager.findAll()).hasSize(1);
        GammaModulePlugin registered = manager.get("chat");
        assertThat(registered).isNotNull();
        assertThat(registered.id()).isEqualTo("chat");
    }

    @Test
    void should_register_resource_class_in_manager() {
        Plugin plugin = mock(Plugin.class);
        PluginManifest manifest = mock(PluginManifest.class);
        when(plugin.id()).thenReturn("test-plugin");
        when(plugin.type()).thenReturn("gamma-module");
        when(plugin.manifest()).thenReturn(manifest);
        when(plugin.clazz()).thenReturn(AGammaModule.class.getName());
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);
        when(plugin.dependencies()).thenReturn(
            new URL[] { GammaModuleHandlerTest.class.getProtectionDomain().getCodeSource().getLocation() }
        );

        ApplicationContext pluginCtx = mock(ApplicationContext.class);
        when(pluginCtx.getBeanDefinitionNames()).thenReturn(new String[0]);
        when(pluginContextFactory.create(any(GammaModulePluginHandler.GammaModulePluginContextConfigurer.class))).thenReturn(pluginCtx);

        handler.handle(plugin);

        assertThat(manager.getRestResourceClasses()).hasSize(1);
        assertThat(manager.getResourceClass("test-plugin").getName()).isEqualTo(AGammaModuleRestClass.class.getName());
        verify(pluginContextFactory).create(any(GammaModulePluginHandler.GammaModulePluginContextConfigurer.class));
    }

    @Test
    void should_not_register_resource_class_when_no_class_name() {
        Plugin plugin = mock(Plugin.class);
        PluginManifest manifest = mock(PluginManifest.class);
        when(plugin.id()).thenReturn("test-plugin");
        when(plugin.type()).thenReturn("gamma-module");
        when(plugin.manifest()).thenReturn(manifest);
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);
        when(plugin.clazz()).thenReturn(AGammaModuleWithoutRestResource.class.getName());

        ApplicationContext pluginCtx = mock(ApplicationContext.class);
        when(pluginCtx.getBeanDefinitionNames()).thenReturn(new String[0]);
        when(pluginContextFactory.create(any(GammaModulePluginHandler.GammaModulePluginContextConfigurer.class))).thenReturn(pluginCtx);

        handler.handle(plugin);

        assertThat(manager.getResourceClass("test-plugin")).isNull();
    }
}
