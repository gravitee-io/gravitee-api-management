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
package io.gravitee.gateway.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.dictionary.DictionaryManager;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.sharedpolicygroup.manager.SharedPolicyGroupManager;
import io.gravitee.gateway.platform.organization.manager.OrganizationManager;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.services.sync.healthcheck.SyncProcessProbe;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.deployer.NoOpSubscriptionDispatcher;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.distributed.service.NoopDistributedSyncService;
import io.gravitee.gateway.services.sync.process.distributed.spring.DistributedSyncDisabledCondition;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.license.LicenseFactory;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.reactivex.rxjava3.annotations.NonNull;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
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

    public static ThreadFactory newThreadFactory(final String x) {
        return new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, x + counter++);
            }
        };
    }

    @Bean
    public ApiMapper apiMapper(ObjectMapper objectMapper, EnvironmentService environmentService) {
        return new ApiMapper(objectMapper, environmentService);
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
    public PlanService planService() {
        return new PlanService();
    }

    @Bean
    public EnvironmentService environmentEnhanceService(
        EnvironmentRepository environmentRepository,
        OrganizationRepository organizationRepository
    ) {
        return new EnvironmentService(environmentRepository, organizationRepository);
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
    @Conditional(DistributedSyncDisabledCondition.class)
    public DistributedSyncService distributedSyncService() {
        return new NoopDistributedSyncService();
    }

    @Bean
    public DeployerFactory deployerFactory(
        ApiKeyService apiKeyService,
        SubscriptionService subscriptionService,
        PlanService planCache,
        @Lazy SubscriptionDispatcher subscriptionDispatcher,
        CommandRepository commandRepository,
        Node node,
        GatewayConfiguration gatewayConfiguration,
        ObjectMapper objectMapper,
        ApiManager apiManager,
        DictionaryManager dictionaryManager,
        OrganizationManager organizationManager,
        EventManager eventManager,
        LicenseManager licenseManager,
        LicenseFactory licenseFactory,
        AccessPointManager accessPointManager,
        SharedPolicyGroupManager sharedPolicyGroupManager,
        DistributedSyncService distributedSyncService
    ) {
        Supplier<SubscriptionDispatcher> subscriptionDispatcherSupplier = provideSubscriptionDispatcher(subscriptionDispatcher);
        return new DeployerFactory(
            apiKeyService,
            subscriptionService,
            planCache,
            subscriptionDispatcherSupplier,
            commandRepository,
            node,
            gatewayConfiguration,
            objectMapper,
            apiManager,
            dictionaryManager,
            organizationManager,
            eventManager,
            licenseManager,
            licenseFactory,
            accessPointManager,
            sharedPolicyGroupManager,
            distributedSyncService
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
    public SyncProcessProbe syncProcessProbe(List<SyncManager> syncManagers) {
        return new SyncProcessProbe(syncManagers);
    }
}
