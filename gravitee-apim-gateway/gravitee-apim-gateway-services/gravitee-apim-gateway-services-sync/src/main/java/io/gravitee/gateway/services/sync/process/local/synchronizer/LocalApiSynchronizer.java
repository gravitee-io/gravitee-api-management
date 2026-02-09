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
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.local.LocalSynchronizer;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.local.model.LocalSyncFileDefinition;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class LocalApiSynchronizer implements LocalSynchronizer {

    private final Map<Path, ReactableApi> definitions = new HashMap<>();

    private final ApiKeyMapper apiKeyMapper;
    private final ApiKeyService apiKeyService;
    private final ApiManager apiManager;
    private final ApiMapper apiMapper;
    private final EnvironmentService environmentService;
    private final ObjectMapper objectMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionService subscriptionService;
    private final ThreadPoolExecutor syncLocalExecutor;

    public LocalApiSynchronizer(
        final ApiKeyMapper apiKeyMapper,
        final ApiKeyService apiKeyService,
        final ApiManager apiManager,
        final ApiMapper apiMapper,
        final EnvironmentService environmentService,
        final ObjectMapper objectMapper,
        final SubscriptionMapper subscriptionMapper,
        final SubscriptionService subscriptionService,
        final ThreadPoolExecutor syncLocalExecutor
    ) {
        this.apiKeyMapper = apiKeyMapper;
        this.apiKeyService = apiKeyService;
        this.apiManager = apiManager;
        this.apiMapper = apiMapper;
        this.environmentService = environmentService;
        this.objectMapper = objectMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.subscriptionService = subscriptionService;
        this.syncLocalExecutor = syncLocalExecutor;
    }

    public Completable synchronize(final File localRegistryDir) {
        return Flowable.fromArray(localRegistryDir.listFiles((dir, name) -> name.endsWith(".json")))
            .map(this::deployApi)
            .doOnError(throwable -> log.error("Error synchronizing API", throwable))
            .doOnNext(api -> log.debug("api {} synchronized from local registry", api.getId()))
            .ignoreElements();
    }

    private ReactableApi<?> deployApi(File apiDefinitionFile) throws IOException {
        LocalSyncFileDefinition fileDefinition = objectMapper.readValue(apiDefinitionFile, LocalSyncFileDefinition.class);
        ReactableApi<?> apiToDeploy;
        if (fileDefinition.getRepositoryApiEvent() != null) {
            apiToDeploy = apiMapper.to(fileDefinition.getRepositoryApiEvent());
            var successfulRegistration = this.apiManager.register(apiToDeploy);
            if (successfulRegistration) {
                this.definitions.put(Paths.get(apiDefinitionFile.toURI()), apiToDeploy);

                if (fileDefinition.getRepositorySubscriptionList() != null && !fileDefinition.getRepositorySubscriptionList().isEmpty()) {
                    Set<Subscription> subscriptionsToDeploy = fileDefinition
                        .getRepositorySubscriptionList()
                        .stream()
                        .flatMap(sub -> subscriptionMapper.to(sub).stream())
                        .collect(Collectors.toSet());
                    subscriptionsToDeploy.forEach(subscriptionService::register);

                    if (fileDefinition.getRepositoryApiKeyList() != null && !fileDefinition.getRepositoryApiKeyList().isEmpty()) {
                        for (io.gravitee.repository.management.model.ApiKey apiKeyModel : fileDefinition.getRepositoryApiKeyList()) {
                            apiKeyService.register(apiKeyMapper.to(apiKeyModel, subscriptionsToDeploy));
                        }
                    }
                }
            } else {
                throw new IllegalStateException("Error during registration");
            }
        } else {
            throw new IllegalStateException("File to be synced cannot be empty");
        }
        return apiToDeploy;
    }

    @Override
    public Completable watch(final Path localRegistryPath, final WatchService watchService) {
        return Flowable.interval(5, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.from(this.syncLocalExecutor))
            .map(t -> {
                WatchKey key = watchService.poll();
                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path fileName = localRegistryPath.resolve(((Path) event.context()).getFileName());
                        log.debug("An event occurs for file {}: {}", fileName, kind.name());
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Api existingDefinition = (Api) this.definitions.get(fileName);
                            if (existingDefinition != null) {
                                this.apiManager.unregister(existingDefinition.getId());
                                this.subscriptionService.unregisterByApiId(existingDefinition.getId());
                                this.apiKeyService.unregisterByApiId(existingDefinition.getId());
                                this.definitions.remove(fileName);
                                deployApi(fileName.toFile());
                            }
                        } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                            deployApi(fileName.toFile());
                        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                            Api existingDefinition = (Api) this.definitions.get(fileName);
                            if (existingDefinition != null && this.apiManager.get(existingDefinition.getId()) != null) {
                                this.apiManager.unregister(existingDefinition.getId());
                                this.subscriptionService.unregisterByApiId(existingDefinition.getId());
                                this.apiKeyService.unregisterByApiId(existingDefinition.getId());
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
}
