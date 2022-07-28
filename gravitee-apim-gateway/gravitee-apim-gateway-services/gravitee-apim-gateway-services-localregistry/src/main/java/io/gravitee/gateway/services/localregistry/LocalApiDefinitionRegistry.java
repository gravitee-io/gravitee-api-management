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
package io.gravitee.gateway.services.localregistry;

import io.gravitee.common.service.AbstractService;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class LocalApiDefinitionRegistry extends AbstractService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalApiDefinitionRegistry.class);

    private static final String JSON_EXTENSION = ".json";

    @Value("${services.local.enabled:false}")
    private boolean enabled;

    @Value("${services.local.path:${gravitee.home}/apis}")
    private String registryPath;

    @Autowired
    private ApiManager apiManager;

    private ExecutorService executor;

    private final Map<Path, Api> definitions = new HashMap<>();

    /**
     * Empty constructor is used to use a workspace directory defined from @Value annotation
     * on registryPath field.
     */
    public LocalApiDefinitionRegistry() {}

    public LocalApiDefinitionRegistry(String registryPath) {
        this.registryPath = registryPath;
    }

    private void init() {
        if (enabled) {
            if (registryPath == null || registryPath.isEmpty()) {
                LOGGER.error("Local API definitions registry path is not specified.");
                throw new RuntimeException("Local API definitions registry path is not specified.");
            }

            File registryDir = new File(registryPath);

            // Quick sanity check on the install root
            if (!registryDir.isDirectory()) {
                LOGGER.error("Invalid API definitions registry directory, {} is not a directory.", registryDir.getAbsolutePath());
                throw new RuntimeException("Invalid API definitions registry directory. Not a directory: " + registryDir.getAbsolutePath());
            }

            initRegistry(registryDir);
        } else {
            LOGGER.warn("Local registry for API definitions is disabled");
        }
    }

    private void initRegistry(File registryDir) {
        LOGGER.info("Loading API definitions from {}", registryDir.getAbsoluteFile());
        File[] definitionFiles = searchForDefinitions(registryDir);

        LOGGER.info("\t{} API definitions have been found.", definitionFiles.length);

        for (File definitionFile : definitionFiles) {
            try {
                Api api = loadDefinition(definitionFile);
                apiManager.register(api);
                definitions.put(Paths.get(definitionFile.toURI()), api);
            } catch (IOException e) {
                LOGGER.error("Unable to load API definition from {}", definitionFile, e);
            }
        }
    }

    private File[] searchForDefinitions(File registryDir) {
        return registryDir.listFiles((dir, name) -> name.endsWith(JSON_EXTENSION));
    }

    private Api loadDefinition(File apiDefinitionFile) throws IOException {
        Api api = new GraviteeMapper().readValue(apiDefinitionFile, Api.class);
        api.getDefinition().setId(IdGenerator.generate(api.getName()));

        return api;
    }

    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            super.doStart();

            this.init();

            executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "registry-monitor"));
            executor.execute(
                () -> {
                    Path registry = Paths.get(registryPath);
                    LOGGER.info("Start local registry monitor for directory {}", registry);

                    try {
                        WatchService watcher = registry.getFileSystem().newWatchService();
                        registry.register(
                            watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                        );

                        while (true) {
                            WatchKey key;
                            try {
                                key = watcher.take();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                return;
                            }

                            for (WatchEvent<?> event : key.pollEvents()) {
                                WatchEvent.Kind<?> kind = event.kind();

                                @SuppressWarnings("unchecked")
                                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                                Path fileName = registry.resolve(ev.context().getFileName());

                                LOGGER.info("An event occurs for file {}: {}", fileName, kind.name());

                                if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    Api loadedDefinition = loadDefinition(fileName.toFile());
                                    Api existingDefinition = definitions.get(fileName);
                                    if (existingDefinition != null) {
                                        if (apiManager.get(existingDefinition.getId()) != null) {
                                            apiManager.register(loadedDefinition);
                                        } else {
                                            apiManager.unregister(existingDefinition.getId());
                                            definitions.remove(fileName);
                                            definitions.put(fileName, loadedDefinition);
                                        }
                                    }
                                } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                    Api loadedDefinition = loadDefinition(fileName.toFile());
                                    boolean registered = apiManager.register(loadedDefinition);
                                    if (registered) {
                                        definitions.put(fileName, loadedDefinition);
                                    }
                                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                    Api existingDefinition = definitions.get(fileName);
                                    if (existingDefinition != null && apiManager.get(existingDefinition.getId()) != null) {
                                        apiManager.unregister(existingDefinition.getId());
                                        definitions.remove(fileName);
                                    }
                                }

                                boolean valid = key.reset();
                                if (!valid) {
                                    break;
                                }
                            }
                        }
                    } catch (IOException ioe) {
                        LOGGER.error("Unexpected error while looking for ÀPI definitions from filesystem", ioe);
                    }
                }
            );
        }
    }

    @Override
    protected void doStop() throws Exception {
        if (enabled) {
            super.doStop();

            executor.shutdownNow();
            executor = null;
        }
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
