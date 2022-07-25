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
package io.gravitee.apim.gateway.tests.sdk.plugin;

import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.core.api.PluginManifestFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Load {@link PluginManifest} from file "plugin.properties"
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PluginManifestLoader {

    public static final Logger LOGGER = LoggerFactory.getLogger(PluginManifestLoader.class);

    private PluginManifestLoader() {
        throw new IllegalStateException("Utility class");
    }

    public static PluginManifest readManifest() {
        try {
            final Path resources = Path.of("src/main/resources");
            final PluginManifestVisitor visitor = new PluginManifestVisitor();
            Files.walkFileTree(resources, visitor);

            Path pluginManifestPath = visitor.getPluginManifest();
            if (pluginManifestPath != null) {
                try (InputStream manifestInputStream = Files.newInputStream(pluginManifestPath)) {
                    Properties properties = new Properties();
                    properties.load(manifestInputStream);

                    return PluginManifestFactory.create(properties);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to find a 'plugin.properties' file in src/main/resources folder", e);
        }

        return null;
    }

    /**
     * This {@link java.nio.file.FileVisitor<Path>} will be used to visit the file tree until it finds the "plugin.properties" file.
     */
    static class PluginManifestVisitor extends SimpleFileVisitor<Path> {

        private Path pluginManifest = null;

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (file.getFileName().toString().equals("plugin.properties")) {
                pluginManifest = file;
                return FileVisitResult.TERMINATE;
            }

            return super.visitFile(file, attrs);
        }

        public Path getPluginManifest() {
            return pluginManifest;
        }
    }
}
