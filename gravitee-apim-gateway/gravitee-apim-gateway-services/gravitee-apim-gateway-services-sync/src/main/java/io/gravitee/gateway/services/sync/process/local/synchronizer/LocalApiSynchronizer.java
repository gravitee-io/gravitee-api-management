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
package io.gravitee.gateway.services.sync.process.local.synchronizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.local.LocalSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.AbstractApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
import io.gravitee.repository.management.model.LifecycleState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class LocalApiSynchronizer extends AbstractApiSynchronizer implements LocalSynchronizer {

    private final Map<Path, ReactableApi> definitions = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final EnvironmentService environmentService;

    public LocalApiSynchronizer(
        final ObjectMapper objectMapper,
        final EnvironmentService environmentService,
        final ApiManager apiManager,
        final ApiMapper apiMapper,
        final PlanAppender planAppender,
        final SubscriptionAppender subscriptionAppender,
        final ApiKeyAppender apiKeyAppender,
        final DeployerFactory deployerFactory,
        final ThreadPoolExecutor syncFetcherExecutor,
        final ThreadPoolExecutor syncDeployerExecutor
    ) {
        super(
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
        this.objectMapper = objectMapper;
        this.environmentService = environmentService;
    }

    public Completable synchronize(final File localRegistryDir) {
        return Flowable
            .fromArray(localRegistryDir.listFiles((dir, name) -> name.endsWith(".json")))
            .map(apiDefinitionFile -> {
                ReactableApi api = toReactableApi(apiDefinitionFile);

                this.apiManager.register(api);
                this.definitions.put(Paths.get(apiDefinitionFile.toURI()), api);

                return api;
            })
            .doOnNext(api -> log.debug("api {} synchronized from local registry", api.getId()))
            .ignoreElements();
    }

    @Override
    public Completable watch(final Path localRegistryPath, final WatchService watchService) {
        return Flowable
            .interval(5, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.from(this.syncFetcherExecutor))
            .map(t -> {
                WatchKey key = watchService.poll();
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = localRegistryPath.resolve(((Path) event.context()).getFileName());
                        log.debug("An event occurs for file {}: {}", fileName, kind.name());
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            ReactableApi loadedDefinition = this.toReactableApi(fileName.toFile());
                            Api existingDefinition = (Api) this.definitions.get(fileName);
                            if (existingDefinition != null) {
                                if (this.apiManager.get(existingDefinition.getId()) != null) {
                                    this.apiManager.register(loadedDefinition);
                                } else {
                                    this.apiManager.unregister(existingDefinition.getId());
                                    this.definitions.remove(fileName);
                                    this.definitions.put(fileName, loadedDefinition);
                                }
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            ReactableApi loadedDefinition = this.toReactableApi(fileName.toFile());
                            boolean registered = this.apiManager.register(loadedDefinition);
                            if (registered) {
                                this.definitions.put(fileName, loadedDefinition);
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            Api existingDefinition = (Api) this.definitions.get(fileName);
                            if (existingDefinition != null && this.apiManager.get(existingDefinition.getId()) != null) {
                                this.apiManager.unregister(existingDefinition.getId());
                                this.definitions.remove(fileName);
                            }
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                }
                return t;
            })
            .ignoreElements();
    }

    @Override
    protected int bulkEvents() {
        return 1;
    }

    private ReactableApi<?> toReactableApi(File apiDefinitionFile) {
        try {
            // Read API definition from event
            var api = objectMapper.readValue(apiDefinitionFile, io.gravitee.repository.management.model.Api.class);

            ReactableApi<?> reactableApi;

            // Check the version of the API definition to read the right model entity
            if (DefinitionVersion.V4 != api.getDefinitionVersion()) {
                var eventApiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class);

                // Update definition with required information for deployment phase
                reactableApi = new io.gravitee.gateway.handlers.api.definition.Api(eventApiDefinition);
            } else {
                if (api.getType() == ApiType.NATIVE) {
                    var eventApiDefinition = objectMapper.readValue(api.getDefinition(), NativeApi.class);

                    // Update definition with required information for deployment phase
                    reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.NativeApi(eventApiDefinition);
                } else if (api.getType() == ApiType.PROXY || api.getType() == ApiType.MESSAGE) {
                    var eventApiDefinition = objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.v4.Api.class);

                    // Update definition with required information for deployment phase
                    reactableApi = new io.gravitee.gateway.reactive.handlers.api.v4.Api(eventApiDefinition);
                } else {
                    throw new IllegalArgumentException("Unsupported ApiType [" + api.getType() + "] for api: " + api.getId());
                }
            }

            reactableApi.setEnabled(api.getLifecycleState() == LifecycleState.STARTED);
            reactableApi.setDeployedAt(api.getCreatedAt());
            reactableApi.setRevision("1");

            environmentService.fill(api.getEnvironmentId(), reactableApi);

            return reactableApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            log.error("Unable to extract api definition from file [{}].", apiDefinitionFile.getName(), e);
            return null;
        }
    }
}
