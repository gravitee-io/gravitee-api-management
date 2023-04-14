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
package io.gravitee.gateway.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.platform.manager.OrganizationManager;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.services.sync.healthcheck.SyncProcessProbe;
import io.gravitee.gateway.services.sync.kubernetes.KubernetesSyncManager;
import io.gravitee.gateway.services.sync.kubernetes.KubernetesSynchronizer;
import io.gravitee.gateway.services.sync.kubernetes.fetcher.ConfigMapEventFetcher;
import io.gravitee.gateway.services.sync.kubernetes.synchronizer.KubernetesApiSynchronizer;
import io.gravitee.gateway.services.sync.process.DefaultSyncManager;
import io.gravitee.gateway.services.sync.process.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.deployer.NoOpSubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.fetcher.ApiKeyFetcher;
import io.gravitee.gateway.services.sync.process.fetcher.DebugEventFetcher;
import io.gravitee.gateway.services.sync.process.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.fetcher.SubscriptionFetcher;
import io.gravitee.gateway.services.sync.process.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.mapper.DebugMapper;
import io.gravitee.gateway.services.sync.process.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.service.PlanService;
import io.gravitee.gateway.services.sync.process.synchronizer.Synchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.synchronizer.api.ApiSynchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.synchronizer.api.SubscriptionAppender;
import io.gravitee.gateway.services.sync.process.synchronizer.apikey.ApiKeySynchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.debug.DebugSynchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.dictionary.DictionarySynchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.organization.FlowAppender;
import io.gravitee.gateway.services.sync.process.synchronizer.organization.OrganizationSynchronizer;
import io.gravitee.gateway.services.sync.process.synchronizer.subscription.SubscriptionSynchronizer;
import io.gravitee.kubernetes.client.KubernetesClient;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.reactivex.rxjava3.annotations.NonNull;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
public class SyncConfiguration {

    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    public static final int DEFAULT_BULK_ITEMS = 100;

    @Bean("syncFetcherExecutor")
    public ThreadPoolExecutor syncFetcherExecutor(@Value("${services.sync.fetcher:-1}") int syncFetcher) {
        int poolSize = syncFetcher != -1 ? syncFetcher : POOL_SIZE;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            1,
            poolSize,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            newThreadFactory("gio.sync-fetcher-")
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);
        threadPoolExecutor.prestartCoreThread();

        return threadPoolExecutor;
    }

    @Bean("syncDeployerExecutor")
    public ThreadPoolExecutor syncDeployerExecutor(@Value("${services.sync.deployer:-1}") int syncDeployer) {
        int poolSize = syncDeployer != -1 ? syncDeployer : POOL_SIZE;
        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            1,
            poolSize,
            15L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            newThreadFactory("gio.sync-deployer-")
        );

        threadPoolExecutor.allowCoreThreadTimeOut(true);
        threadPoolExecutor.prestartCoreThread();

        return threadPoolExecutor;
    }

    private static ThreadFactory newThreadFactory(final String x) {
        return new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, x + counter++);
            }
        };
    }

    /*
     * Reactive synchronizer
     */

    @Bean
    public ApiMapper apiMapper(
        ObjectMapper objectMapper,
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository
    ) {
        return new ApiMapper(objectMapper, environmentRepository, organizationRepository);
    }

    @Bean
    public SubscriptionMapper subscriptionMapper(ObjectMapper objectMapper) {
        return new SubscriptionMapper(objectMapper);
    }

    @Bean
    public ApiKeyMapper apiKeyMapper() {
        return new ApiKeyMapper();
    }

    @Bean
    public DictionaryMapper dictionaryMapper(ObjectMapper objectMapper) {
        return new DictionaryMapper(objectMapper);
    }

    @Bean
    public OrganizationMapper organizationMapper(ObjectMapper objectMapper) {
        return new OrganizationMapper(objectMapper);
    }

    @Bean
    public DebugMapper debugMapper() {
        return new DebugMapper();
    }

    @Bean
    public LatestEventFetcher eventFetcher(
        EventLatestRepository eventLatestRepository,
        @Value("${services.sync.bulk_items:" + DEFAULT_BULK_ITEMS + "}") int bulkItems
    ) {
        return new LatestEventFetcher(eventLatestRepository, bulkItems);
    }

    @Bean
    public SubscriptionFetcher subscriptionFetcher(SubscriptionRepository subscriptionRepository) {
        return new SubscriptionFetcher(subscriptionRepository);
    }

    @Bean
    public ApiKeyFetcher apiKeyFetcher(ApiKeyRepository apiKeyRepository) {
        return new ApiKeyFetcher(apiKeyRepository);
    }

    @Bean
    public DebugEventFetcher debugEventFetcher(EventRepository eventRepository, Node node) {
        return new DebugEventFetcher(eventRepository, node);
    }

    @Bean
    public PlanService planCache() {
        return new PlanService();
    }

    @Bean
    public PlanAppender planAppender(ObjectMapper objectMapper, PlanRepository planRepository, GatewayConfiguration gatewayConfiguration) {
        return new PlanAppender(objectMapper, planRepository, gatewayConfiguration);
    }

    @Bean
    public SubscriptionAppender subscriptionAppender(SubscriptionRepository subscriptionRepository, SubscriptionMapper subscriptionMapper) {
        return new SubscriptionAppender(subscriptionRepository, subscriptionMapper);
    }

    @Bean
    public ApiKeyAppender apiKeyAppender(ApiKeyRepository apiKeyRepository, ApiKeyMapper apiKeyMapper) {
        return new ApiKeyAppender(apiKeyRepository, apiKeyMapper);
    }

    @Bean
    public FlowAppender flowAppender(GatewayConfiguration gatewayConfiguration) {
        return new FlowAppender(gatewayConfiguration);
    }

    @Bean
    public DeployerFactory deployerFactory(
        ApiKeyService apiKeyService,
        SubscriptionService subscriptionService,
        PlanService planCache,
        @Lazy SubscriptionDispatcher subscriptionDispatcher,
        CommandRepository commandRepository,
        Node node,
        ObjectMapper objectMapper,
        ApiManager apiManager,
        DictionaryManager dictionaryManager,
        OrganizationManager organizationManager,
        EventManager eventManager
    ) {
        Supplier<SubscriptionDispatcher> subscriptionDispatcherSupplier = provideSubscriptionDispatcher(subscriptionDispatcher);
        return new DeployerFactory(
            apiKeyService,
            subscriptionService,
            planCache,
            subscriptionDispatcherSupplier,
            commandRepository,
            node,
            objectMapper,
            apiManager,
            dictionaryManager,
            organizationManager,
            eventManager
        );
    }

    /**
     * When no SubscriptionDispatcher available in the context, use a no-op one
     * @param subscriptionDispatcher
     * @return a supplier of SubscriptionDispatcher
     */
    protected Supplier<SubscriptionDispatcher> provideSubscriptionDispatcher(SubscriptionDispatcher subscriptionDispatcher) {
        return () -> {
            SubscriptionDispatcher dispatcher = subscriptionDispatcher;
            try {
                // try to use the subscriptionDispatcher bean to check if it is present in spring context
                subscriptionDispatcher.lifecycleState();
            } catch (NoSuchBeanDefinitionException e) {
                // If absent, use a no-op subscription dispatcher
                dispatcher = new NoOpSubscriptionDispatcher();
            }
            return dispatcher;
        };
    }

    @Bean
    public ApiSynchronizer apiSynchronizer(
        LatestEventFetcher eventsFetcher,
        ApiManager apiManager,
        ApiMapper apiMapper,
        PlanAppender planAppender,
        SubscriptionAppender subscriptionAppender,
        ApiKeyAppender apiKeyAppender,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new ApiSynchronizer(
            eventsFetcher,
            apiManager,
            apiMapper,
            planAppender,
            subscriptionAppender,
            apiKeyAppender,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public SubscriptionSynchronizer subscriptionSynchronizer(
        SubscriptionFetcher subscriptionFetcher,
        SubscriptionMapper subscriptionMapper,
        PlanService planCache,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new SubscriptionSynchronizer(
            subscriptionFetcher,
            subscriptionMapper,
            deployerFactory,
            planCache,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public ApiKeySynchronizer apiKeySynchronizer(
        final ApiKeyFetcher apiKeyFetcher,
        final SubscriptionCacheService subscriptionService,
        final ApiKeyMapper apiKeyMapper,
        final DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new ApiKeySynchronizer(
            apiKeyFetcher,
            subscriptionService,
            apiKeyMapper,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public DictionarySynchronizer dictionarySynchronizer(
        LatestEventFetcher eventsFetcher,
        DictionaryMapper dictionaryMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DictionarySynchronizer(eventsFetcher, dictionaryMapper, deployerFactory, syncFetcherExecutor, syncDeployerExecutor);
    }

    @Bean
    public OrganizationSynchronizer organizationSynchronizer(
        LatestEventFetcher eventsFetcher,
        OrganizationMapper organizationMapper,
        FlowAppender flowAppender,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new OrganizationSynchronizer(
            eventsFetcher,
            organizationMapper,
            flowAppender,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public DebugSynchronizer debugSynchronizer(
        DebugEventFetcher debugEventFetcher,
        DebugMapper debugMapperMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DebugSynchronizer(debugEventFetcher, debugMapperMapper, deployerFactory, syncFetcherExecutor, syncDeployerExecutor);
    }

    @Bean
    public DefaultSyncManager syncManager(
        @Qualifier("managementRouter") Router router,
        final Node node,
        final List<Synchronizer> synchronizers,
        @Value("${services.sync.delay:5000}") int delay,
        @Value("${services.sync.unit:MILLISECONDS}") TimeUnit unit,
        @Value("${services.sync.retry:3}") final int retryAttempt
    ) {
        return new DefaultSyncManager(router, node, synchronizers, delay, unit, retryAttempt);
    }

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
        @Value("${services.sync.kubernetes.enabled:false}") boolean kubernetesEnabled
    ) {
        return new KubernetesSyncManager(kubernetesSynchronizers, kubernetesEnabled);
    }

    @Bean
    public SyncProcessProbe syncProcessProbe(List<SyncManager> syncManagers) {
        return new SyncProcessProbe(syncManagers);
    }
}
