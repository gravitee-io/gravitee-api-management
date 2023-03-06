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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.services.sync.SyncManager;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.gateway.services.sync.cache.configuration.LocalCacheConfiguration;
import io.gravitee.gateway.services.sync.healthcheck.ApiSyncProbe;
import io.gravitee.gateway.services.sync.kubernetes.KubernetesSyncService;
import io.gravitee.gateway.services.sync.synchronizer.ApiSynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.DebugApiSynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.DictionarySynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.OrganizationSynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.api.EventToReactableApiAdapter;
import io.gravitee.gateway.services.sync.synchronizer.api.PlanFetcher;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.core.api.PluginRegistry;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Node node;

    @Autowired
    private PluginRegistry pluginRegistry;

    @Autowired
    private io.gravitee.node.api.configuration.Configuration configuration;

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
    public PlanFetcher planFetcher(ObjectMapper objectMapper, PlanRepository planRepository) {
        return new PlanFetcher(objectMapper, planRepository);
    }

    @Bean
    public EventToReactableApiAdapter eventToReactableApiAdapter(
        ObjectMapper objectMapper,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository
    ) {
        return new EventToReactableApiAdapter(objectMapper, environmentRepository, organizationRepository);
    }

    @Bean
    public ApiSynchronizer apiSynchronizer(
        @Qualifier("syncExecutor") ThreadPoolExecutor executor,
        @Value("${services.sync.bulk_items:100}") int bulkItems,
        EventLatestRepository eventLatestRepository,
        ApiKeysCacheService apiKeysCacheService,
        SubscriptionsCacheService subscriptionsCacheService,
        SubscriptionService subscriptionService,
        ApiManager apiManager,
        EventToReactableApiAdapter eventToReactableApiAdapter,
        PlanFetcher planFetcher,
        GatewayConfiguration gatewayConfiguration
    ) {
        return new ApiSynchronizer(
            eventLatestRepository,
            executor,
            bulkItems,
            apiKeysCacheService,
            subscriptionsCacheService,
            subscriptionService,
            apiManager,
            eventToReactableApiAdapter,
            planFetcher,
            gatewayConfiguration
        );
    }

    @Bean
    public DebugApiSynchronizer debugApiSynchronizer(EventRepository eventRepository) {
        return new DebugApiSynchronizer(eventRepository, eventManager, pluginRegistry, configuration, node);
    }

    @Bean
    public DictionarySynchronizer dictionarySynchronizer(
        ObjectMapper objectMapper,
        @Qualifier("syncExecutor") ThreadPoolExecutor executor,
        @Value("${services.sync.bulk_items:100}") int bulkItems,
        EventLatestRepository eventLatestRepository,
        DictionaryManager dictionaryManager
    ) {
        return new DictionarySynchronizer(eventLatestRepository, objectMapper, executor, bulkItems, dictionaryManager);
    }

    @Bean
    public OrganizationSynchronizer organizationSynchronizer(
        ObjectMapper objectMapper,
        @Qualifier("syncExecutor") ThreadPoolExecutor executor,
        @Value("${services.sync.bulk_items:100}") int bulkItems,
        EventLatestRepository eventLatestRepository,
        OrganizationManager organizationManager,
        GatewayConfiguration gatewayConfiguration
    ) {
        return new OrganizationSynchronizer(
            eventLatestRepository,
            objectMapper,
            executor,
            bulkItems,
            organizationManager,
            gatewayConfiguration
        );
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
