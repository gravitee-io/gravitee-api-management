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
package io.gravitee.gateway.services.sync.process.local.spring;

import static io.gravitee.gateway.services.sync.SyncConfiguration.POOL_SIZE;
import static io.gravitee.gateway.services.sync.SyncConfiguration.newThreadFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.local.LocalSyncManager;
import io.gravitee.gateway.services.sync.process.local.LocalSynchronizer;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.local.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.local.synchronizer.LocalApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(LocalSyncCondition.class)
public class LocalSyncConfiguration {

    /*
     * Local Synchronization
     */
    @Bean("syncLocalExecutor")
    public ThreadPoolExecutor syncLocalExecutor(@Value("${services.sync.local.threads:-1}") int syncLocal) {
        int poolSize = syncLocal != -1 ? syncLocal : POOL_SIZE;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            1,
            poolSize,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            newThreadFactory("gio.sync-local-")
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }

    @Bean
    public ApiMapper localApiMapper(ObjectMapper objectMapper, EnvironmentService environmentService) {
        return new ApiMapper(objectMapper, environmentService);
    }

    @Bean
    public SubscriptionMapper localSubscriptionMapper(ObjectMapper objectMapper, ApiProductRegistry apiProductRegistry) {
        return new SubscriptionMapper(objectMapper, apiProductRegistry);
    }

    @Bean
    public ApiKeyMapper localApiKeyMapper() {
        return new ApiKeyMapper();
    }

    @Bean
    public LocalApiSynchronizer localApiSynchronizer(
        ApiKeyMapper apiKeyMapper,
        ApiKeyService apiKeyService,
        ApiManager apiManager,
        ApiMapper apiMapper,
        EnvironmentService environmentService,
        ObjectMapper objectMapper,
        SubscriptionMapper subscriptionMapper,
        SubscriptionService subscriptionService,
        @Qualifier("syncLocalExecutor") ThreadPoolExecutor syncLocalExecutor
    ) {
        return new LocalApiSynchronizer(
            apiKeyMapper,
            apiKeyService,
            apiManager,
            apiMapper,
            environmentService,
            objectMapper,
            subscriptionMapper,
            subscriptionService,
            syncLocalExecutor
        );
    }

    @Bean
    public LocalSyncManager localSyncManager(
        @Value("${services.sync.local.path:${gravitee.home}/apis}") final String localRegistryPath,
        final List<LocalSynchronizer> localSynchronizers,
        final DistributedSyncService distributedSyncService
    ) {
        return new LocalSyncManager(localRegistryPath, localSynchronizers, distributedSyncService);
    }
}
