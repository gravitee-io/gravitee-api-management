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
package io.gravitee.gateway.services.sync.process.repository.spring;

import static io.gravitee.gateway.services.sync.SyncConfiguration.DEFAULT_BULK_ITEMS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.DistributedSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.service.DistributedSyncService;
import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.gateway.services.sync.process.repository.RepositorySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.fetcher.AccessPointFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.ApiKeyFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.DebugEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.InstallationIdFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LatestEventFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.LicenseFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.OrganizationIdsFetcher;
import io.gravitee.gateway.services.sync.process.repository.fetcher.SubscriptionFetcher;
import io.gravitee.gateway.services.sync.process.repository.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.DebugMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.repository.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.service.EnvironmentService;
import io.gravitee.gateway.services.sync.process.repository.service.PlanService;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiKeyAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.PlanAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.SubscriptionAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.ApiKeySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.debug.DebugSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionarySynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.node.NodeMetadataSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.FlowAppender;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupSynchronizer;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SubscriptionSynchronizer;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.InstallationRepository;
import io.gravitee.repository.management.api.LicenseRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.vertx.ext.web.Router;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(RepositorySyncCondition.class)
public class RepositorySyncConfiguration {

    @Bean
    public DictionaryMapper dictionaryMapper(ObjectMapper objectMapper) {
        return new DictionaryMapper(objectMapper);
    }

    @Bean
    public OrganizationMapper organizationMapper(ObjectMapper objectMapper) {
        return new OrganizationMapper(objectMapper);
    }

    @Bean
    public DebugMapper debugMapper(EnvironmentService environmentService) {
        return new DebugMapper(environmentService);
    }

    @Bean
    public AccessPointMapper accessPointMapper() {
        return new AccessPointMapper();
    }

    @Bean
    public SharedPolicyGroupMapper sharedPolicyGroupMapper(ObjectMapper objectMapper, EnvironmentService environmentService) {
        return new SharedPolicyGroupMapper(objectMapper, environmentService);
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
    public LicenseFetcher licenseFetcher(
        LicenseRepository licenseRepository,
        @Value("${services.sync.bulk_items:" + DEFAULT_BULK_ITEMS + "}") int bulkItems
    ) {
        return new LicenseFetcher(licenseRepository, bulkItems);
    }

    @Bean
    public AccessPointFetcher accessPointFetcher(
        AccessPointRepository accessPointRepository,
        @Value("${services.sync.bulk_items:" + DEFAULT_BULK_ITEMS + "}") int bulkItems
    ) {
        return new AccessPointFetcher(accessPointRepository, bulkItems);
    }

    @Bean
    public OrganizationIdsFetcher organizationIdsFetcher(EnvironmentRepository environmentRepository, GatewayConfiguration configuration) {
        return new OrganizationIdsFetcher(environmentRepository, configuration);
    }

    @Bean
    public InstallationIdFetcher installationIdFetcher(InstallationRepository installationRepository) {
        return new InstallationIdFetcher(installationRepository);
    }

    @Bean
    public FlowAppender flowAppender(GatewayConfiguration gatewayConfiguration) {
        return new FlowAppender(gatewayConfiguration);
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
    public LicenseSynchronizer licenseSynchronizer(
        Node node,
        LicenseFetcher licenseFetcher,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new LicenseSynchronizer(node, licenseFetcher, deployerFactory, syncFetcherExecutor, syncDeployerExecutor);
    }

    @Bean
    public AccessPointSynchronizer accessPointSynchronizer(
        AccessPointFetcher accessPointFetcher,
        AccessPointMapper accessPointMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new AccessPointSynchronizer(
            accessPointFetcher,
            accessPointMapper,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public SharedPolicyGroupSynchronizer sharedPolicyGroupSynchronizer(
        LatestEventFetcher eventsFetcher,
        SharedPolicyGroupMapper sharedPolicyGroupMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new SharedPolicyGroupSynchronizer(
            eventsFetcher,
            sharedPolicyGroupMapper,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public NodeMetadataSynchronizer nodeMetadataSynchronizer(
        OrganizationIdsFetcher organizationIdsFetcher,
        InstallationIdFetcher installationIdFetcher,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new NodeMetadataSynchronizer(
            organizationIdsFetcher,
            installationIdFetcher,
            deployerFactory,
            syncFetcherExecutor,
            syncDeployerExecutor
        );
    }

    @Bean
    public DefaultSyncManager syncManager(
        @Qualifier("managementRouter") Router router,
        final Node node,
        final List<RepositorySynchronizer> synchronizers,
        @Autowired(required = false) final List<DistributedSynchronizer> distributedSynchronizers,
        final DistributedSyncService distributedSyncService,
        @Value("${services.sync.delay:5000}") int delay,
        @Value("${services.sync.unit:MILLISECONDS}") TimeUnit unit,
        @Value("${services.sync.retry:3}") final int retryAttempt
    ) {
        return new DefaultSyncManager(
            router,
            node,
            synchronizers,
            distributedSynchronizers,
            distributedSyncService,
            delay,
            unit,
            retryAttempt
        );
    }
}
