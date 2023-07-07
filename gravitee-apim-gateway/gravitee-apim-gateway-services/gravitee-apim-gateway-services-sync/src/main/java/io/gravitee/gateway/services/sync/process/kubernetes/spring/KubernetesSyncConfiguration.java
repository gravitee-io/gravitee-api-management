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
package io.gravitee.gateway.services.sync.process.kubernetes.spring;

import static io.gravitee.gateway.services.sync.SyncConfiguration.POOL_SIZE;
import static io.gravitee.gateway.services.sync.SyncConfiguration.newThreadFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.kubernetes.KubernetesSyncManager;
import io.gravitee.gateway.services.sync.process.kubernetes.KubernetesSynchronizer;
import io.gravitee.gateway.services.sync.process.kubernetes.fetcher.ConfigMapEventFetcher;
import io.gravitee.gateway.services.sync.process.kubernetes.synchronizer.KubernetesApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
import io.gravitee.kubernetes.client.KubernetesClient;
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
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(KubernetesSyncCondition.class)
public class KubernetesSyncConfiguration {

    /*
     * Kubernetes Synchronization
     */
    @Bean("syncKubernetesExecutor")
    public ThreadPoolExecutor syncKubernetesExecutor(@Value("${services.sync.kubernetes.threads:-1}") int syncKubernetes) {
        int poolSize = syncKubernetes != -1 ? syncKubernetes : POOL_SIZE;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            1,
            poolSize,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            newThreadFactory("gio.sync-kubernetes-")
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        return threadPoolExecutor;
    }

    @Bean
    public ConfigMapEventFetcher configMapEventFetcher(
        KubernetesClient kubernetesClient,
        @Value("${services.sync.kubernetes.namespaces:#{null}}") String[] namespaces,
        ObjectMapper objectMapper
    ) {
        return new ConfigMapEventFetcher(kubernetesClient, namespaces, objectMapper);
    }

    @Bean
    public KubernetesApiSynchronizer kubernetesApiSynchronizer(
        ConfigMapEventFetcher configMapEventFetcher,
        ApiManager apiManager,
        ApiMapper apiMapper,
        PlanAppender planAppender,
        SubscriptionAppender subscriptionAppender,
        ApiKeyAppender apiKeyAppender,
        DeployerFactory deployerFactory,
        @Qualifier("syncKubernetesExecutor") ThreadPoolExecutor syncKubernetesExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new KubernetesApiSynchronizer(
            configMapEventFetcher,
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncKubernetesExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public KubernetesSyncManager kubernetesSyncManager(
        List<KubernetesSynchronizer> kubernetesSynchronizers,
        DistributedSyncService distributedSyncService
    ) {
        return new KubernetesSyncManager(kubernetesSynchronizers, distributedSyncService);
    }
}
