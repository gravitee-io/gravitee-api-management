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
package io.gravitee.gateway.services.sync.process.distributed.spring;

import static io.gravitee.gateway.services.sync.SyncConfiguration.DEFAULT_BULK_ITEMS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.services.sync.process.common.deployer.DeployerFactory;
import io.gravitee.gateway.services.sync.process.distributed.fetcher.DistributedEventFetcher;
import io.gravitee.gateway.services.sync.process.distributed.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.service.DefaultDistributedSyncService;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.accesspoint.DistributedAccessPointSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.api.DistributedApiSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.apikey.DistributedApiKeySynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.dictionary.DistributedDictionarySynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.license.DistributedLicenseSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.node.DistributedNodeMetadataSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.organization.DistributedOrganizationSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.sharedpolicygroup.DistributedSharedPolicyGroupSynchronizer;
import io.gravitee.gateway.services.sync.process.distributed.synchronizer.subscription.DistributedSubscriptionSynchronizer;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Configuration
@Conditional(DistributedSyncEnabledCondition.class)
public class DistributedSyncConfiguration {

    @Bean
    public ApiMapper distributedApiMapper(ObjectMapper objectMapper, SubscriptionMapper subscriptionMapper, ApiKeyMapper apiKeyMapper) {
        return new ApiMapper(objectMapper, subscriptionMapper, apiKeyMapper);
    }

    @Bean
    public ApiKeyMapper distributedApiKeyMapper(ObjectMapper objectMapper) {
        return new ApiKeyMapper(objectMapper);
    }

    @Bean
    public SubscriptionMapper distributedSubscriptionMapper(ObjectMapper objectMapper) {
        return new SubscriptionMapper(objectMapper);
    }

    @Bean
    public DictionaryMapper distributedDictionaryMapper(ObjectMapper objectMapper) {
        return new DictionaryMapper(objectMapper);
    }

    @Bean
    public OrganizationMapper distributedOrganizationMapper(ObjectMapper objectMapper) {
        return new OrganizationMapper(objectMapper);
    }

    @Bean
    public AccessPointMapper distributedAccessPointMapper(ObjectMapper objectMapper) {
        return new AccessPointMapper(objectMapper);
    }

    @Bean
    public SharedPolicyGroupMapper distributedSharedPolicyGroupMapper(ObjectMapper objectMapper) {
        return new SharedPolicyGroupMapper(objectMapper);
    }

    @Bean
    public LicenseMapper distributedLicenseMapper() {
        return new LicenseMapper();
    }

    @Bean
    public NodeMetadataMapper distributedNodeMetadataMapper(ObjectMapper objectMapper) {
        return new NodeMetadataMapper(objectMapper);
    }

    @Bean
    public DistributedEventFetcher distributedEventFetcher(
        @Lazy DistributedEventRepository distributedEventRepository,
        @Value("${services.sync.bulk_items:" + DEFAULT_BULK_ITEMS + "}") int bulkItems
    ) {
        return new DistributedEventFetcher(distributedEventRepository, bulkItems);
    }

    @Bean
    public DistributedSubscriptionSynchronizer distributedSubscriptionSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        SubscriptionMapper subscriptionMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DistributedSubscriptionSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            subscriptionMapper
        );
    }

    @Bean
    public DistributedApiKeySynchronizer distributedApiKeySynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        ApiKeyMapper apiKeyMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DistributedApiKeySynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            apiKeyMapper
        );
    }

    @Bean
    public DistributedApiSynchronizer distributedApiSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        ApiMapper apiMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DistributedApiSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            apiMapper
        );
    }

    @Bean
    public DistributedDictionarySynchronizer dictionarySynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        DictionaryMapper dictionaryMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DistributedDictionarySynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            dictionaryMapper
        );
    }

    @Bean
    public DistributedOrganizationSynchronizer organizationSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        OrganizationMapper organizationMapper,
        DeployerFactory deployerFactory,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor
    ) {
        return new DistributedOrganizationSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            organizationMapper
        );
    }

    @Bean
    public DistributedLicenseSynchronizer distributedLicenseSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor,
        DeployerFactory deployerFactory,
        LicenseMapper licenseMapper
    ) {
        return new DistributedLicenseSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            licenseMapper
        );
    }

    @Bean
    public DistributedAccessPointSynchronizer distributedAccessPointSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor,
        DeployerFactory deployerFactory,
        AccessPointMapper accessPointMapper
    ) {
        return new DistributedAccessPointSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            accessPointMapper
        );
    }

    @Bean
    public DistributedSharedPolicyGroupSynchronizer distributedSharedPolicyGroupSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor,
        DeployerFactory deployerFactory,
        SharedPolicyGroupMapper sharedPolicyGroupMapper
    ) {
        return new DistributedSharedPolicyGroupSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            sharedPolicyGroupMapper
        );
    }

    @Bean
    public DistributedNodeMetadataSynchronizer distributedNodeMetadataSynchronizer(
        DistributedEventFetcher distributedEventFetcher,
        @Qualifier("syncFetcherExecutor") ThreadPoolExecutor syncFetcherExecutor,
        @Qualifier("syncDeployerExecutor") ThreadPoolExecutor syncDeployerExecutor,
        DeployerFactory deployerFactory,
        NodeMetadataMapper nodeMetadataMapper
    ) {
        return new DistributedNodeMetadataSynchronizer(
            distributedEventFetcher,
            syncFetcherExecutor,
            syncDeployerExecutor,
            deployerFactory,
            nodeMetadataMapper
        );
    }

    @Bean
    public DefaultDistributedSyncService distributedSyncService(
        final Node node,
        final ClusterManager clusterManager,
        @Value("${distributed-sync.type}") String distributedSyncRepoType,
        @Lazy final DistributedEventRepository distributedEventRepository,
        @Lazy final DistributedSyncStateRepository distributedSyncStateRepository,
        final ApiMapper apiMapper,
        final SubscriptionMapper subscriptionMapper,
        final ApiKeyMapper apiKeyMapper,
        final OrganizationMapper organizationMapper,
        final DictionaryMapper dictionaryMapper,
        final LicenseMapper licenseMapper,
        final AccessPointMapper accessPointMapper,
        final SharedPolicyGroupMapper sharedPolicyGroupMapper,
        final NodeMetadataMapper nodeMetadataMapper
    ) {
        return new DefaultDistributedSyncService(
            node,
            clusterManager,
            distributedSyncRepoType,
            distributedEventRepository,
            distributedSyncStateRepository,
            apiMapper,
            subscriptionMapper,
            apiKeyMapper,
            organizationMapper,
            dictionaryMapper,
            licenseMapper,
            accessPointMapper,
            sharedPolicyGroupMapper,
            nodeMetadataMapper
        );
    }
}
