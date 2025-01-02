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
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.local.LocalSyncManager;
import io.gravitee.gateway.services.sync.process.local.LocalSynchronizer;
import io.gravitee.gateway.services.sync.process.local.synchronizer.LocalApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
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
    public LocalApiSynchronizer localApiSynchronizer(
        ObjectMapper objectMapper,
        EnvironmentService environmentService,
        ApiManager apiManager,
        ApiMapper apiMapper,
        PlanAppender planAppender,
        SubscriptionAppender subscriptionAppender,
        ApiKeyAppender apiKeyAppender,
        DeployerFactory deployerFactory,
        @Qualifier("syncLocalExecutor") ThreadPoolExecutor syncLocalExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new LocalApiSynchronizer(
            objectMapper,
            environmentService,
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncLocalExecutor,
            syncDeployerExecutor
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
