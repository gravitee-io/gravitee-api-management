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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.plugin.console.ConsoleExtension;
import io.gravitee.plugin.console.ConsoleExtensionEntity;
import io.gravitee.plugin.console.ConsoleExtensionManager;
import io.gravitee.plugin.core.api.PluginManifest;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ConsoleExtensionServiceTest {

    @Mock
    private ConsoleExtensionManager consoleExtensionManager;

    private ConsoleExtensionService service;

    @TempDir
    Path pluginDir;

    @BeforeEach
    void setUp() {
        service = new ConsoleExtensionService(consoleExtensionManager);
    }

    // -- list() --

    @Test
    void should_return_empty_list_when_no_extensions() {
        when(consoleExtensionManager.findAll()).thenReturn(List.of());

        List<ConsoleExtensionEntity> result = service.list();

        assertThat(result).isEmpty();
    }

    @Test
    void should_list_all_extensions() throws IOException {
        ConsoleExtension ext1 = anExtension("chat", "Chat", "1.0.0");
        ConsoleExtension ext2 = anExtension("otel", "OpenTelemetry", "2.0.0");
        when(consoleExtensionManager.findAll()).thenReturn(List.of(ext1, ext2));

        List<ConsoleExtensionEntity> result = service.list();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConsoleExtensionEntity::getId).containsExactly("chat", "otel");
        assertThat(result).extracting(ConsoleExtensionEntity::getName).containsExactly("Chat", "OpenTelemetry");
        assertThat(result).extracting(ConsoleExtensionEntity::getVersion).containsExactly("1.0.0", "2.0.0");
    }

    @Test
    void should_include_manifest_when_present() throws IOException {
        String manifestContent = "{\"entry\":\"main.js\"}";
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        Files.createDirectories(extension.path().resolve("ui"));
        Files.writeString(extension.path().resolve("ui/manifest.json"), manifestContent);
        when(consoleExtensionManager.findAll()).thenReturn(List.of(extension));

        List<ConsoleExtensionEntity> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getManifest()).isEqualTo(manifestContent);
    }

    @Test
    void should_return_null_manifest_when_missing() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.findAll()).thenReturn(List.of(extension));

        List<ConsoleExtensionEntity> result = service.list();

        assertThat(result.get(0).getManifest()).isNull();
    }

    // -- getById() --

    @Test
    void should_get_extension_by_id() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);

        ConsoleExtensionEntity result = service.getById("chat");

        assertThat(result.getId()).isEqualTo("chat");
        assertThat(result.getName()).isEqualTo("Chat");
        assertThat(result.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void should_throw_when_extension_not_found() {
        when(consoleExtensionManager.get("unknown")).thenReturn(null);

        assertThatThrownBy(() -> service.getById("unknown"))
            .isInstanceOf(ConsoleExtensionNotFoundException.class)
            .hasMessageContaining("unknown");
    }

    // -- getResourceClass() --

    @Test
    void should_delegate_get_resource_class_to_manager() {
        doReturn(String.class).when(consoleExtensionManager).getResourceClass("chat");

        assertThat(service.getResourceClass("chat")).isEqualTo(String.class);
    }

    @Test
    void should_return_null_when_no_resource_class() {
        when(consoleExtensionManager.getResourceClass("unknown")).thenReturn(null);

        assertThat(service.getResourceClass("unknown")).isNull();
    }

    // -- getAsset() --

    @Test
    void should_return_404_when_extension_not_found_for_asset() {
        when(consoleExtensionManager.get("unknown")).thenReturn(null);

        Response response = service.getAsset("unknown", "main.js");

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void should_return_asset_content() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Path uiDir = Files.createDirectories(extension.path().resolve("ui"));
        Files.writeString(uiDir.resolve("main.js"), "console.log('hello');");

        Response response = service.getAsset("chat", "main.js");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(new String((byte[]) response.getEntity())).isEqualTo("console.log('hello');");
    }

    @Test
    void should_return_asset_in_subdirectory() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Path assetsDir = Files.createDirectories(extension.path().resolve("ui/assets"));
        Files.writeString(assetsDir.resolve("style.css"), "body { color: red; }");

        Response response = service.getAsset("chat", "assets/style.css");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(new String((byte[]) response.getEntity())).isEqualTo("body { color: red; }");
    }

    @Test
    void should_return_404_when_asset_does_not_exist() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Files.createDirectories(extension.path().resolve("ui"));

        Response response = service.getAsset("chat", "nonexistent.js");

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void should_return_404_when_asset_is_a_directory() throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Files.createDirectories(extension.path().resolve("ui/subdir"));

        Response response = service.getAsset("chat", "subdir");

        assertThat(response.getStatus()).isEqualTo(404);
    }

    // -- path traversal --

    @ParameterizedTest(name = "should reject path traversal: {0}")
    @ValueSource(strings = { "../secret.txt", "assets/../../secret.txt", "../../../../../../etc/passwd", "../ui/../../../etc/shadow" })
    void should_return_400_for_path_traversal(String maliciousPath) throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Files.createDirectories(extension.path().resolve("ui"));

        Response response = service.getAsset("chat", maliciousPath);

        assertThat(response.getStatus()).isEqualTo(400);
    }

    // -- content type detection --

    static Stream<Arguments> asset_content_type_cases() {
        return Stream.of(
            Arguments.of("app.js", "text/javascript"),
            Arguments.of("app.mjs", "application/javascript"),
            Arguments.of("style.css", "text/css"),
            Arguments.of("icon.svg", "image/svg+xml"),
            Arguments.of("font.woff", "font/woff"),
            Arguments.of("font.woff2", "font/woff2"),
            Arguments.of("data.xyz", "application/octet-stream")
        );
    }

    @ParameterizedTest(name = "should resolve {0} to {1}")
    @MethodSource("asset_content_type_cases")
    void should_resolve_content_type(String filename, String expectedContentType) throws IOException {
        ConsoleExtension extension = anExtension("chat", "Chat", "1.0.0");
        when(consoleExtensionManager.get("chat")).thenReturn(extension);
        Path uiDir = Files.createDirectories(extension.path().resolve("ui"));
        Files.write(uiDir.resolve(filename), new byte[] { 0 });

        Response response = service.getAsset("chat", filename);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getMediaType().toString()).isEqualTo(expectedContentType);
    }

    // -- CONTENT_TYPES map validation --

    @Test
    void should_only_contain_types_not_resolved_by_jdk() {
        for (String ext : ConsoleExtensionService.CONTENT_TYPES.keySet()) {
            String jdkType = URLConnection.guessContentTypeFromName("file." + ext);
            assertThat(jdkType)
                .as("Extension '%s' is already resolved by the JDK to '%s' and should be removed from CONTENT_TYPES", ext, jdkType)
                .isNull();
        }
    }

    // -- helpers --

    private ConsoleExtension anExtension(String id, String name, String version) throws IOException {
        Path path = Files.createDirectories(pluginDir.resolve(id));
        PluginManifest manifest = new PluginManifest() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return null;
            }

            @Override
            public String category() {
                return null;
            }

            @Override
            public String version() {
                return version;
            }

            @Override
            public String plugin() {
                return null;
            }

            @Override
            public String type() {
                return ConsoleExtension.PLUGIN_TYPE;
            }

            @Override
            public int priority() {
                return 0;
            }

            @Override
            public String feature() {
                return null;
            }
        };

        return new ConsoleExtension() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String clazz() {
                return null;
            }

            @Override
            public Path path() {
                return path;
            }

            @Override
            public PluginManifest manifest() {
                return manifest;
            }

            @Override
            public java.net.URL[] dependencies() {
                return new java.net.URL[0];
            }

            @Override
            public boolean deployed() {
                return true;
            }
        };
    }
}
