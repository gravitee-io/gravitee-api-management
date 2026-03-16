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

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.stereotype.Component;

/**
 * Service for managing Gamma modules.
 * This service provides methods to list available modules, get module details, and serve module assets.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class GammaModuleService {

    static final Map<String, String> CONTENT_TYPES = Map.of("mjs", "application/javascript", "woff", "font/woff", "woff2", "font/woff2");
    public static final String UI_DIR = "ui";

    private final GammaModuleManager gammaModuleManager;

    public GammaModuleService(GammaModuleManager gammaModuleManager) {
        this.gammaModuleManager = gammaModuleManager;
    }

    /**
     * Get the REST resource class to be registered for a given Gamma module plugin ID.
     *
     * @param pluginId the plugin ID of the Gamma module.
     * @return the REST resource class to be registered for the specified Gamma module, or null if no REST resource should be registered.
     */
    public Class<?> getResourceClass(String pluginId) {
        return gammaModuleManager.getResourceClass(pluginId);
    }

    /**
     * List all available Gamma modules.
     *
     * @return a list of all available Gamma modules, with their details (ID, name, version, manifest content).
     */
    public List<GammaModuleDefinition> list() {
        return gammaModuleManager.findAll().stream().map(this::convert).collect(Collectors.toList());
    }

    /**
     * Get the details of a Gamma module by its plugin ID.
     *
     * @param pluginId the plugin ID of the module to retrieve.
     * @return the details of the Gamma module with the specified plugin ID.
     */
    public GammaModuleDefinition getById(String pluginId) {
        GammaModulePlugin module = gammaModuleManager.get(pluginId);
        if (module == null) {
            throw new GammaModuleNotFoundException(pluginId);
        }
        return convert(module);
    }

    /**
     * Serve an asset file for a given Gamma module plugin ID and asset path.
     *
     * @param pluginId the plugin ID of the Gamma module.
     * @param assetPath the path of the asset to serve, relative to the "ui" directory of the module.
     * @return a Response containing the asset content if found, or an appropriate error response if not found or if an error occurs.
     */
    public Response getAsset(String pluginId, String assetPath) {
        GammaModulePlugin module = gammaModuleManager.get(pluginId);
        if (module == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Path uiDir = module.path().resolve(UI_DIR);
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
            log.warn("Failed to read asset '{}' for Gamma module '{}'", assetPath, pluginId, e);
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

    private GammaModuleDefinition convert(GammaModulePlugin module) {
        String manifest = readManifest(module);
        return GammaModuleDefinition.builder()
            .id(module.id())
            .name(module.manifest().name())
            .version(module.manifest().version())
            .mfManifest(manifest)
            .build();
    }

    private String readManifest(GammaModulePlugin module) {
        Path manifestPath = module.path().resolve(UI_DIR).resolve("mf-manifest.json");
        if (Files.exists(manifestPath)) {
            try {
                return Files.readString(manifestPath);
            } catch (IOException e) {
                log.warn("Failed to read mf-manifest.json for Gamma module '{}'", module.id(), e);
            }
        }
        return null;
    }
}
