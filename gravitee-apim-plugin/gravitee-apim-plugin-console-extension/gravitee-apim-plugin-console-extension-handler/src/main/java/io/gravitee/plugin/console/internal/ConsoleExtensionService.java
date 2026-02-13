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

import io.gravitee.plugin.console.ConsoleExtension;
import io.gravitee.plugin.console.ConsoleExtensionEntity;
import io.gravitee.plugin.console.ConsoleExtensionManager;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class ConsoleExtensionService {

    static final Map<String, String> CONTENT_TYPES = Map.of("mjs", "application/javascript", "woff", "font/woff", "woff2", "font/woff2");

    private final ConsoleExtensionManager consoleExtensionManager;

    public ConsoleExtensionService(ConsoleExtensionManager consoleExtensionManager) {
        this.consoleExtensionManager = consoleExtensionManager;
    }

    public Class<?> getResourceClass(String pluginId) {
        return consoleExtensionManager.getResourceClass(pluginId);
    }

    public List<ConsoleExtensionEntity> list() {
        return consoleExtensionManager.findAll().stream().map(this::convert).collect(Collectors.toList());
    }

    public ConsoleExtensionEntity getById(String pluginId) {
        ConsoleExtension extension = consoleExtensionManager.get(pluginId);
        if (extension == null) {
            throw new ConsoleExtensionNotFoundException(pluginId);
        }
        return convert(extension);
    }

    public Response getAsset(String pluginId, String assetPath) {
        ConsoleExtension extension = consoleExtensionManager.get(pluginId);
        if (extension == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Path uiDir = extension.path().resolve("ui");
        Path resolved = uiDir.resolve(assetPath).normalize();

        if (!resolved.startsWith(uiDir)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            byte[] content = Files.readAllBytes(resolved);
            String contentType = guessContentType(assetPath);
            return Response.ok(content).type(contentType).build();
        } catch (IOException e) {
            log.warn("Failed to read asset '{}' for console extension '{}'", assetPath, pluginId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static String guessContentType(String path) {
        String contentType = URLConnection.guessContentTypeFromName(path);
        if (contentType != null) {
            return contentType;
        }
        int dot = path.lastIndexOf('.');
        if (dot >= 0) {
            String ext = path.substring(dot + 1).toLowerCase();
            contentType = CONTENT_TYPES.get(ext);
            if (contentType != null) {
                return contentType;
            }
        }
        return "application/octet-stream";
    }

    private ConsoleExtensionEntity convert(ConsoleExtension extension) {
        String manifest = readManifest(extension);
        return ConsoleExtensionEntity.builder()
            .id(extension.id())
            .name(extension.manifest().name())
            .version(extension.manifest().version())
            .manifest(manifest)
            .build();
    }

    private String readManifest(ConsoleExtension extension) {
        Path manifestPath = extension.path().resolve("ui").resolve("manifest.json");
        if (Files.exists(manifestPath)) {
            try {
                return Files.readString(manifestPath);
            } catch (IOException e) {
                log.warn("Failed to read manifest.json for console extension '{}'", extension.id(), e);
            }
        }
        return null;
    }
}
