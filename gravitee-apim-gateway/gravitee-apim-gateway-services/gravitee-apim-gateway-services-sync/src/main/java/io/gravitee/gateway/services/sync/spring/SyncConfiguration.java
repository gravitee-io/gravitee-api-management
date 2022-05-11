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
package io.gravitee.gateway.services.sync.spring;

import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.cache.configuration.LocalCacheConfiguration;
import io.gravitee.gateway.services.sync.healthcheck.ApiSyncProbe;
import io.gravitee.gateway.services.sync.kubernetes.KubernetesSyncService;
import io.gravitee.gateway.services.sync.synchronizer.*;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.node.api.cache.CacheManager;
import io.reactivex.annotations.NonNull;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Import({ LocalCacheConfiguration.class })
public class SyncConfiguration {

    public static final int PARALLELISM = Runtime.getRuntime().availableProcessors() * 2;

    @Bean
    public SyncManager syncManager() {
        return new SyncManager();
    }

    @Bean("syncExecutor")
    public ThreadPoolExecutor syncExecutor() {
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            PARALLELISM,
            PARALLELISM,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private int counter = 0;

                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r, "gio.sync-" + counter++);
                }
            }
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }

    @Bean
    public ApiSynchronizer apiSynchronizer() {
        return new ApiSynchronizer();
    }

    @Bean
    public DebugApiSynchronizer debugApiSynchronizer() {
        return new DebugApiSynchronizer();
    }

    @Bean
    public DictionarySynchronizer dictionarySynchronizer() {
        return new DictionarySynchronizer();
    }

    @Bean
    public OrganizationSynchronizer organizationSynchronizer() {
        return new OrganizationSynchronizer();
    }

    @Bean
    public KubernetesSyncService kubernetesSyncService(KubernetesClient client, ApiSynchronizer apiSynchronizer) {
        return new KubernetesSyncService(client, apiSynchronizer);
    }

    @Bean
    public ApiSyncProbe apisProbe() {
        return new ApiSyncProbe();
    }
}
