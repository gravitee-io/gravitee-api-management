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
package io.gravitee.gateway.core.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.core.definition.ApiDefinition;
import io.gravitee.gateway.core.manager.ApiManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class LocalApiDefinitionRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(LocalApiDefinitionRegistry.class);

    private final static String JSON_EXTENSION = ".json";

    @Value("${local.enabled:false}")
    private boolean enabled;

    @Value("${local.path:${node.home}/apis}")
    private String registryPath;

    @Autowired
    private ApiManager apiManager;

    /**
     * Empty constructor is used to use a workspace directory defined from @Value annotation
     * on registryPath field.
     */
    public LocalApiDefinitionRegistry() {
    }

    public LocalApiDefinitionRegistry(String registryPath) {
        this.registryPath = registryPath;
    }

    public void init() {
        if (enabled) {
            if (registryPath == null || registryPath.isEmpty()) {
                LOGGER.error("Local API definitions registry path is not specified.");
                throw new RuntimeException("Local API definitions registry path is not specified.");
            }

            File registryDir = new File(registryPath);

            // Quick sanity check on the install root
            if (!registryDir.isDirectory()) {
                LOGGER.error("Invalid API definitions registry directory, {} is not a directory.", registryDir.getAbsolutePath());
                throw new RuntimeException("Invalid API definitions registry directory. Not a directory: "
                        + registryDir.getAbsolutePath());
            }

            LOGGER.info("Loading API definitions from {}", registryDir.getAbsoluteFile());
            File[] definitionFiles = searchForDefinitions(registryDir);

            LOGGER.info("\t{} API definitions have been found.", definitionFiles.length);

            for(File definitionFile : definitionFiles) {
                try {
                    ApiDefinition apiDefinition = load(definitionFile);
                    apiManager.add(apiDefinition);
                } catch (IOException e) {
                    LOGGER.error("Unable to load API definition from {}", definitionFile, e);
                }
            }
        } else {
            LOGGER.warn("Local registry for API definitions is disabled");
        }
    }

    private ApiDefinition load(File apiDefinitionFile) throws IOException {
        return new ObjectMapper().readValue(apiDefinitionFile, ApiDefinition.class);
    }

    private File[] searchForDefinitions(File registryDir) {
        return registryDir.listFiles((dir, name) -> {
            return name.endsWith(JSON_EXTENSION);
        });
    }

    public void setApiManager(ApiManager apiManager) {
        this.apiManager = apiManager;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRegistryPath(String registryPath) {
        this.registryPath = registryPath;
    }
}
