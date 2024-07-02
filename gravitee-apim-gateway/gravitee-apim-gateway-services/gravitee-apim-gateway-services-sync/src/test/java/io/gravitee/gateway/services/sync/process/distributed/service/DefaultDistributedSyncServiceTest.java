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
package io.gravitee.gateway.services.sync.process.distributed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.Organization;
import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.dictionary.DictionaryDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.license.LicenseDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.organization.OrganizationDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.sharedpolicygroup.SharedPolicyGroupReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.subscription.SingleSubscriptionDeployable;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.node.api.cluster.Member;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.DistributedSyncStateRepository;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DefaultDistributedSyncServiceTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    Node node;

    @Mock
    ClusterManager clusterManager;

    @Mock
    DistributedEventRepository distributedEventRepository;

    @Mock
    DistributedSyncStateRepository distributedSyncStateRepository;

    private DefaultDistributedSyncService cut;
    private Member member;

    @BeforeEach
    public void beforeEach() {
        member = mock(Member.class);
        lenient().when(member.primary()).thenReturn(true);
        lenient().when(clusterManager.self()).thenReturn(member);
        lenient().when(clusterManager.clusterId()).thenReturn("clusterId");
        lenient().when(distributedSyncStateRepository.ready()).thenReturn(Completable.complete());

        SubscriptionMapper subscriptionMapper = new SubscriptionMapper(objectMapper);
        ApiKeyMapper apiKeyMapper = new ApiKeyMapper(objectMapper);
        ApiMapper apiMapper = new ApiMapper(objectMapper, subscriptionMapper, apiKeyMapper);
        cut =
            new DefaultDistributedSyncService(
                node,
                clusterManager,
                "type",
                distributedEventRepository,
                distributedSyncStateRepository,
                apiMapper,
                subscriptionMapper,
                apiKeyMapper,
                new OrganizationMapper(objectMapper),
                new DictionaryMapper(objectMapper),
                new LicenseMapper(),
                new AccessPointMapper(objectMapper),
                new SharedPolicyGroupMapper(objectMapper),
                new NodeMetadataMapper(objectMapper)
            );
    }

    @Nested
    class PrimaryNode {

        @BeforeEach
        public void beforeEach() {
            lenient().when(member.primary()).thenReturn(true);
            lenient().when(distributedSyncStateRepository.createOrUpdate(any())).thenReturn(Completable.complete());
            lenient().when(distributedEventRepository.createOrUpdate(any())).thenReturn(Completable.complete());
        }

        @Test
        void should_be_validate_with_repo_type() {
            assertDoesNotThrow(() -> cut.validate());
        }

        @Test
        void should_not_be_validate_with_repo_type() {
            cut =
                new DefaultDistributedSyncService(
                    node,
                    clusterManager,
                    null,
                    distributedEventRepository,
                    distributedSyncStateRepository,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                );
            assertThrows(SyncException.class, () -> cut.validate());
        }

        @Test
        void should_be_enabled() {
            assertThat(cut.isEnabled()).isTrue();
        }

        @Test
        void should_be_ready() {
            cut.ready().test().assertComplete();
        }

        @Test
        void should_be_primary_node() {
            assertThat(cut.isPrimaryNode()).isTrue();
        }

        @Test
        void should_return_state() {
            DistributedSyncState distributedSyncState = new DistributedSyncState();
            when(distributedSyncStateRepository.findByClusterId(anyString())).thenReturn(Maybe.just(distributedSyncState));
            cut.state().test().assertValue(distributedSyncState);
        }

        @Test
        void should_store_state() {
            cut.storeState(50L, 1000L).test().assertComplete();
            verify(distributedSyncStateRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_api() {
            cut.distributeIfNeeded(ApiReactorDeployable.builder().build()).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_api_key() {
            cut.distributeIfNeeded(SingleApiKeyDeployable.builder().apiKey(new ApiKey()).build()).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_subscription() {
            cut.distributeIfNeeded(SingleSubscriptionDeployable.builder().subscription(new Subscription()).build()).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_dictionary() {
            cut.distributeIfNeeded(DictionaryDeployable.builder().id("id").build()).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_environment_fow() {
            cut.distributeIfNeeded(SharedPolicyGroupReactorDeployable.builder().sharedPolicyGroupId("id").build()).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_organization() {
            cut
                .distributeIfNeeded(
                    OrganizationDeployable.builder().reactableOrganization(new ReactableOrganization(new Organization())).build()
                )
                .test()
                .assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_access_point() {
            cut
                .distributeIfNeeded(
                    AccessPointDeployable
                        .builder()
                        .reactableAccessPoint(ReactableAccessPoint.builder().id("id").host("host").environmentId("environmentId").build())
                        .build()
                )
                .test()
                .assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }
    }

    @Nested
    class NotPrimaryNode {

        @BeforeEach
        public void beforeEach() {
            lenient().when(member.primary()).thenReturn(false);
        }

        @Test
        void should_be_enabled() {
            assertThat(cut.isEnabled()).isTrue();
        }

        @Test
        void should_not_be_primary_node() {
            assertThat(cut.isPrimaryNode()).isFalse();
        }

        @Test
        void should_not_call_repository_when_getting_state() {
            cut.state().test().assertComplete();
            verifyNoInteractions(distributedSyncStateRepository);
        }

        @Test
        void should_not_call_repository_when_storing_state() {
            cut.storeState(-1L, -1L).test().assertComplete();
            verifyNoInteractions(distributedSyncStateRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_api() {
            cut.distributeIfNeeded(ApiReactorDeployable.builder().build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_api_key() {
            cut.distributeIfNeeded(SingleApiKeyDeployable.builder().apiKey(new ApiKey()).build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_subscription() {
            cut.distributeIfNeeded(SingleSubscriptionDeployable.builder().subscription(new Subscription()).build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_dictionary() {
            cut.distributeIfNeeded(DictionaryDeployable.builder().id("id").build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_organization() {
            cut
                .distributeIfNeeded(
                    OrganizationDeployable.builder().reactableOrganization(new ReactableOrganization(new Organization())).build()
                )
                .test()
                .assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_license() {
            cut.distributeIfNeeded(LicenseDeployable.builder().id("id").license("license").build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_access_point() {
            cut
                .distributeIfNeeded(
                    AccessPointDeployable
                        .builder()
                        .reactableAccessPoint(ReactableAccessPoint.builder().id("id").host("host").environmentId("environmentId").build())
                        .build()
                )
                .test()
                .assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }
    }
}
