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
package io.gravitee.plugin.console.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.plugin.console.ConsoleExtension;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class ConsoleExtensionHandlerTest {

    private ConsoleExtensionHandler handler;
    private DefaultConsoleExtensionManager manager;

    @BeforeEach
    void setUp() throws Exception {
        handler = new ConsoleExtensionHandler();

        // Create a fresh manager instance for each test to avoid singleton state leaking
        Constructor<DefaultConsoleExtensionManager> ctor = DefaultConsoleExtensionManager.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        manager = ctor.newInstance();

        // Inject manager via reflection since @Autowired is not available in unit tests
        Field managerField = ConsoleExtensionHandler.class.getDeclaredField("consoleExtensionManager");
        managerField.setAccessible(true);
        managerField.set(handler, manager);
    }

    @Test
    void should_handle_console_extension_type() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("console-extension");

        assertThat(handler.canHandle(plugin)).isTrue();
    }

    @Test
    void should_handle_console_extension_type_case_insensitive() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.type()).thenReturn("Console-Extension");

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
        when(plugin.type()).thenReturn("console-extension");
        when(plugin.manifest()).thenReturn(manifest);
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);

        handler.handle(plugin);

        assertThat(manager.findAll()).hasSize(1);
        ConsoleExtension registered = manager.get("chat");
        assertThat(registered).isNotNull();
        assertThat(registered.id()).isEqualTo("chat");
    }

    @Test
    void should_register_resource_class_in_manager() {
        Plugin plugin = mock(Plugin.class);
        PluginManifest manifest = mock(PluginManifest.class);
        when(plugin.id()).thenReturn("test-plugin");
        when(plugin.type()).thenReturn("console-extension");
        when(plugin.manifest()).thenReturn(manifest);
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);
        when(plugin.clazz()).thenReturn(ConsoleExtensionHandlerTest.class.getName());
        when(plugin.dependencies()).thenReturn(
            new URL[] { ConsoleExtensionHandlerTest.class.getProtectionDomain().getCodeSource().getLocation() }
        );

        handler.handle(plugin);

        assertThat(manager.getPluginResourceClasses()).hasSize(1);
        assertThat(manager.getResourceClass("test-plugin").getName()).isEqualTo(ConsoleExtensionHandlerTest.class.getName());
    }

    @Test
    void should_not_register_resource_class_when_no_class_name() {
        Plugin plugin = mock(Plugin.class);
        PluginManifest manifest = mock(PluginManifest.class);
        when(plugin.id()).thenReturn("test-plugin");
        when(plugin.type()).thenReturn("console-extension");
        when(plugin.manifest()).thenReturn(manifest);
        when(manifest.version()).thenReturn("1.0.0");
        when(plugin.deployed()).thenReturn(true);
        when(plugin.clazz()).thenReturn(null);

        handler.handle(plugin);

        assertThat(manager.getResourceClass("test-plugin")).isNull();
    }
}
