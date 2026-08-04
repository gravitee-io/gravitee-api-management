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
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.platform.organization.ReactableOrganization;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.services.sync.process.common.model.SyncAction;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.distributed.mapper.AccessPointMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiKeyMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.ApiProductMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.DictionaryMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.LicenseMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.NodeMetadataMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.OrganizationMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SharedPolicyGroupMapper;
import io.gravitee.gateway.services.sync.process.distributed.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.distributed.model.DistributedSyncException;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.accesspoint.AccessPointDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.api.ApiReactorDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apikey.SingleApiKeyDeployable;
import io.gravitee.gateway.services.sync.process.repository.synchronizer.apiproduct.ApiProductReactorDeployable;
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
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.gravitee.repository.distributedsync.model.DistributedSyncState;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        cut = new DefaultDistributedSyncService(
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
            new NodeMetadataMapper(objectMapper),
            new ApiProductMapper(objectMapper)
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
            cut = new DefaultDistributedSyncService(
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
                    AccessPointDeployable.builder()
                        .reactableAccessPoint(ReactableAccessPoint.builder().id("id").host("host").environmentId("environmentId").build())
                        .build()
                )
                .test()
                .assertComplete();
            verify(distributedEventRepository).createOrUpdate(any());
        }

        @Test
        void should_distribute_api_product() {
            ReactableApiProduct reactableApiProduct = ReactableApiProduct.builder()
                .id("product-id")
                .name("Test Product")
                .apiIds(Set.of("api-1"))
                .build();
            ApiProductReactorDeployable deployable = ApiProductReactorDeployable.builder()
                .apiProductId("product-id")
                .reactableApiProduct(reactableApiProduct)
                .syncAction(SyncAction.DEPLOY)
                .build();

            ArgumentCaptor<DistributedEvent> captor = ArgumentCaptor.forClass(DistributedEvent.class);
            cut.distributeIfNeeded(deployable).test().assertComplete();
            verify(distributedEventRepository).createOrUpdate(captor.capture());
            DistributedEvent event = captor.getValue();
            assertThat(event.getId()).isEqualTo("product-id");
            assertThat(event.getType()).isEqualTo(DistributedEventType.API_PRODUCT);
            assertThat(event.getSyncAction()).isEqualTo(DistributedSyncAction.DEPLOY);
            assertThat(event.getPayload()).isNotNull();
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
                    AccessPointDeployable.builder()
                        .reactableAccessPoint(ReactableAccessPoint.builder().id("id").host("host").environmentId("environmentId").build())
                        .build()
                )
                .test()
                .assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }

        @Test
        void should_not_call_repository_when_distributing_api_product() {
            cut.distributeIfNeeded(ApiProductReactorDeployable.builder().apiProductId("product-id").build()).test().assertComplete();
            verifyNoInteractions(distributedEventRepository);
        }
    }

    @Nested
    class DistributionResilience {

        @Test
        void should_cap_concurrent_event_writes() {
            List<Subscription> subscriptions = IntStream.range(0, 100)
                .mapToObj(i -> {
                    Subscription subscription = new Subscription();
                    subscription.setId("subscription-" + i);
                    return subscription;
                })
                .toList();
            ApiReactorDeployable deployable = ApiReactorDeployable.builder()
                .apiId("api-id")
                .syncAction(SyncAction.DEPLOY)
                .subscriptions(subscriptions)
                .build();
            AtomicInteger subscribed = new AtomicInteger();
            when(distributedEventRepository.createOrUpdate(any())).thenReturn(
                Completable.never().doOnSubscribe(disposable -> subscribed.incrementAndGet())
            );

            var observer = cut.distributeIfNeeded(deployable).test();

            observer.assertNotComplete();
            assertThat(subscribed.get()).isEqualTo(DefaultDistributedSyncService.WRITE_MAX_CONCURRENCY);
            observer.dispose();
        }

        @Test
        void should_fail_store_state_and_replay_window_after_a_distribution_failure() {
            when(distributedEventRepository.createOrUpdate(any())).thenReturn(
                Completable.error(new RuntimeException("Redis waiting queue is full"))
            );
            cut
                .distributeIfNeeded(SingleSubscriptionDeployable.builder().subscription(new Subscription()).build())
                .test()
                .assertError(DistributedSyncException.class);

            cut.storeState(1L, 2L).test().assertError(DistributedSyncException.class);
            verify(distributedSyncStateRepository, never()).createOrUpdate(any());

            // The failure flag is reset: the next cycle can store its state again
            when(distributedSyncStateRepository.createOrUpdate(any())).thenReturn(Completable.complete());
            cut.storeState(1L, 2L).test().assertComplete();
        }

        @Test
        void should_bound_concurrent_writes_across_parallel_distributions() {
            // WRITE_MAX_CONCURRENCY only caps a single deployable; the deployer distributes many in parallel.
            // The shared gate must bound the AGGREGATE in-flight writes, not just each call individually.
            cut.writeGate = new DefaultDistributedSyncService.WriteGate(8);

            AtomicInteger current = new AtomicInteger();
            AtomicInteger peak = new AtomicInteger();
            // Count via doOnSubscribe/doOnTerminate (which run before the terminal is propagated) so the
            // gauge reflects true concurrency; doFinally would over-count as the freed permit is handed to
            // the next write before the finishing write's decrement runs.
            when(distributedEventRepository.createOrUpdate(any())).thenAnswer(invocation ->
                Completable.timer(20, TimeUnit.MILLISECONDS)
                    .doOnSubscribe(disposable -> peak.accumulateAndGet(current.incrementAndGet(), Math::max))
                    .doOnTerminate(current::decrementAndGet)
            );

            List<Completable> distributions = IntStream.range(0, 100)
                .mapToObj(i ->
                    cut.distributeIfNeeded(SingleSubscriptionDeployable.builder().subscription(subscription("subscription-" + i)).build())
                )
                .toList();

            Completable.merge(distributions).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            verify(distributedEventRepository, times(100)).createOrUpdate(any());
            // Exactly 8: the gate must both cap concurrency at 8 AND actually reach it (100 writes are
            // submitted at once), so an over-serializing regression such as a permit leak is caught here.
            assertThat(peak.get()).isEqualTo(8);
        }

        @Test
        void write_gate_should_recover_the_permit_when_a_holder_is_disposed() {
            var gate = new DefaultDistributedSyncService.WriteGate(1);

            var holder = gate.runGated(Completable.never()).test();
            var parked = gate.runGated(Completable.complete()).test();
            parked.assertNotComplete(); // the single permit is held, so this run parks

            holder.dispose(); // frees the permit -> handed to the parked run
            parked.assertComplete();

            // permit fully returned to the pool afterwards: a fresh run completes immediately
            gate.runGated(Completable.complete()).test().assertComplete();
        }

        @Test
        void write_gate_should_not_release_a_permit_when_a_parked_waiter_is_disposed() {
            var gate = new DefaultDistributedSyncService.WriteGate(1);

            var holder = gate.runGated(Completable.never()).test(); // holds the only permit
            var parked = gate.runGated(Completable.never()).test(); // parks, never granted

            parked.dispose(); // disposing the WAITER must not free the holder's permit

            var next = gate.runGated(Completable.complete()).test();
            next.assertNotComplete(); // gate still full, holder keeps its permit

            holder.dispose(); // now a permit frees and is handed to next
            next.assertComplete();
        }

        @Test
        void write_gate_should_return_every_permit_under_concurrent_completions_errors_and_disposals() throws InterruptedException {
            var gate = new DefaultDistributedSyncService.WriteGate(4);
            int threads = 8;
            int opsPerThread = 200;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch done = new CountDownLatch(threads);
            for (int t = 0; t < threads; t++) {
                final int seed = t;
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            int kind = (seed + i) % 3;
                            Completable work = switch (kind) {
                                case 0 -> Completable.complete();
                                case 1 -> Completable.error(new RuntimeException("boom"));
                                default -> Completable.timer(2, TimeUnit.MILLISECONDS);
                            };
                            var observer = gate.runGated(work).test();
                            if (kind == 2 && i % 2 == 0) {
                                observer.dispose(); // race a disposal against the in-flight write
                            } else {
                                observer.awaitDone(5, TimeUnit.SECONDS);
                            }
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
            pool.shutdownNow();

            // Every permit must have been returned, whatever the interleaving of completes/errors/disposals.
            int inFlight = gate.inFlight();
            for (int i = 0; i < 200 && inFlight != 0; i++) {
                Thread.sleep(10);
                inFlight = gate.inFlight();
            }
            assertThat(inFlight).isZero();
            // ...and the gate still grants afterwards.
            gate.runGated(Completable.complete()).test().awaitDone(5, TimeUnit.SECONDS).assertComplete();
        }

        @Test
        void should_not_drop_events_when_redis_waiting_queue_would_overflow() {
            // Reproduces APIM-14672: the Redis client rejects writes with "Redis waiting queue is full" once
            // too many are in-flight, silently dropping subscription/api-key events during a bulk sync. The
            // global bound must keep in-flight under that ceiling so every event is still written.
            int waitingQueueCeiling = 8;
            cut.writeGate = new DefaultDistributedSyncService.WriteGate(waitingQueueCeiling);

            AtomicInteger current = new AtomicInteger();
            when(distributedEventRepository.createOrUpdate(any())).thenAnswer(invocation ->
                Completable.defer(() -> {
                    if (current.incrementAndGet() > waitingQueueCeiling) {
                        current.decrementAndGet();
                        return Completable.error(new RuntimeException("Redis waiting queue is full"));
                    }
                    // Decrement before the terminal is propagated (doOnTerminate, not doFinally) so the count
                    // is accurate when the freed permit is immediately handed to the next queued write.
                    return Completable.timer(20, TimeUnit.MILLISECONDS).doOnTerminate(current::decrementAndGet);
                })
            );

            List<Completable> distributions = IntStream.range(0, 200)
                .mapToObj(i ->
                    cut.distributeIfNeeded(SingleSubscriptionDeployable.builder().subscription(subscription("subscription-" + i)).build())
                )
                .toList();

            Completable.merge(distributions).test().awaitDone(10, TimeUnit.SECONDS).assertComplete();

            verify(distributedEventRepository, times(200)).createOrUpdate(any());
        }

        private Subscription subscription(final String id) {
            Subscription subscription = new Subscription();
            subscription.setId(id);
            return subscription;
        }
    }
}
