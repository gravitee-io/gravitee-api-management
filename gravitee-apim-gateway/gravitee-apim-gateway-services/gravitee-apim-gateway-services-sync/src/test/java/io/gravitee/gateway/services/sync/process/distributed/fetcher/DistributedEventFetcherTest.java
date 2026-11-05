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
package io.gravitee.gateway.services.sync.process.distributed.fetcher;

import static io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager.TIMEFRAME_DELAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedEventFetcherTest {

    private static final String CLUSTER_ID = "test-cluster";

    @Mock
    private DistributedEventRepository distributedEventRepository;

    @Mock
    private ClusterManager clusterManager;

    private DistributedEventFetcher cut;

    @BeforeEach
    public void beforeEach() {
        lenient().when(clusterManager.clusterId()).thenReturn(CLUSTER_ID);
        cut = new DistributedEventFetcher(distributedEventRepository, clusterManager, 1, Map.of());
    }

    @Test
    void should_fetch_latest_events() {
        DistributedEvent distributedEvent1 = DistributedEvent.builder().id("1").build();
        DistributedEvent distributedEvent2 = DistributedEvent.builder().id("2").build();
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.just(distributedEvent1, distributedEvent2));
        cut
            .fetchLatest(null, null, DistributedEventType.API, Set.of())
            .test()
            .assertValueAt(0, distributedEvent1)
            .assertValueAt(1, distributedEvent2)
            .assertComplete();
    }

    @Test
    void should_search_with_criteria_based_on_timeframe() {
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());
        cut
            .fetchLatest(1000L, 2000L, DistributedEventType.SUBSCRIPTION, Set.of(DistributedSyncAction.DEPLOY))
            .test()
            .assertNoValues()
            .assertComplete();

        ArgumentCaptor<DistributedEventCriteria> criteriaCaptor = ArgumentCaptor.forClass(DistributedEventCriteria.class);
        verify(distributedEventRepository).searchAll(criteriaCaptor.capture(), eq(1000L));
        DistributedEventCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.getClusterId()).isEqualTo(CLUSTER_ID);
        assertThat(criteria.getFrom()).isEqualTo(1000L - TIMEFRAME_DELAY);
        assertThat(criteria.getTo()).isEqualTo(2000L + TIMEFRAME_DELAY);
        assertThat(criteria.getType()).isEqualTo(DistributedEventType.SUBSCRIPTION);
        assertThat(criteria.getSyncActions()).containsExactly(DistributedSyncAction.DEPLOY);
    }

    @Test
    void should_fetch_with_bulk_items_when_above_the_minimum_batch_size() {
        cut = new DistributedEventFetcher(distributedEventRepository, clusterManager, 50_000, Map.of());
        when(distributedEventRepository.searchAll(any(), eq(50_000L))).thenReturn(Flowable.empty());
        cut.fetchLatest(null, null, DistributedEventType.API, Set.of()).test().assertComplete();
    }

    @Test
    void should_reject_non_positive_bulk_items() {
        assertThatThrownBy(() -> new DistributedEventFetcher(distributedEventRepository, clusterManager, 0, Map.of())).isInstanceOf(
            IllegalArgumentException.class
        );
        assertThatThrownBy(() -> new DistributedEventFetcher(distributedEventRepository, clusterManager, -1, Map.of())).isInstanceOf(
            IllegalArgumentException.class
        );
    }

    @Test
    void should_use_a_type_override_without_going_under_the_minimum_batch() {
        cut = new DistributedEventFetcher(
            distributedEventRepository,
            clusterManager,
            5_000,
            Map.of(DistributedEventType.API, 2_000, DistributedEventType.DICTIONARY, 10)
        );

        assertThat(cut.requestBatchSize(DistributedEventType.API)).isEqualTo(2_000);
        assertThat(cut.requestBatchSize(DistributedEventType.DICTIONARY)).isEqualTo(1_000);
        assertThat(cut.requestBatchSize(DistributedEventType.SUBSCRIPTION)).isEqualTo(5_000);
    }

    @Test
    void should_fetch_the_authoritative_deploy_set_without_a_time_window() {
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        cut.fetchAuthoritative(DistributedEventType.API, Set.of(DistributedSyncAction.DEPLOY)).test().assertComplete();

        ArgumentCaptor<DistributedEventCriteria> criteriaCaptor = ArgumentCaptor.forClass(DistributedEventCriteria.class);
        verify(distributedEventRepository).searchAll(criteriaCaptor.capture(), eq(1000L));
        assertThat(criteriaCaptor.getValue().getFrom()).isEqualTo(-1L);
        assertThat(criteriaCaptor.getValue().getTo()).isEqualTo(-1L);
        assertThat(criteriaCaptor.getValue().getType()).isEqualTo(DistributedEventType.API);
    }

    @Test
    void should_skip_an_incremental_fetch_when_the_change_sequence_has_not_moved() {
        when(distributedEventRepository.serverTime()).thenReturn(Single.just(5_000L));
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of(DistributedSyncAction.DEPLOY)).test().assertComplete();
        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of(DistributedSyncAction.DEPLOY)).test().assertComplete();

        verify(distributedEventRepository, times(1)).searchAll(any(), eq(1000L));
    }

    @Test
    void should_fetch_again_when_the_change_sequence_moves() {
        AtomicLong clock = new AtomicLong(5_000L);
        when(distributedEventRepository.serverTime()).thenAnswer(invocation -> Single.just(clock.get()));
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of()).test().assertComplete();
        clock.set(6_000L);
        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of()).test().assertComplete();

        verify(distributedEventRepository, times(2)).searchAll(any(), eq(1000L));
    }

    @Test
    void should_force_a_fetch_after_the_bounded_number_of_unchanged_cycles() {
        when(distributedEventRepository.serverTime()).thenReturn(Single.just(5_000L));
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        for (int i = 0; i < 12; i++) {
            cut.fetchLatest(1_000L, 2_000L, DistributedEventType.SUBSCRIPTION, Set.of()).test().assertComplete();
        }

        // The first cycle fetches, the next ten skip, and the twelfth is a forced check.
        verify(distributedEventRepository, times(2)).searchAll(any(), eq(1000L));
    }

    @Test
    void should_fetch_when_the_change_sequence_clock_cannot_be_read() {
        when(distributedEventRepository.serverTime()).thenReturn(Single.error(new IllegalStateException("redis down")));
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of()).test().assertComplete();

        verify(distributedEventRepository).searchAll(any(), eq(1000L));
    }

    @Test
    void should_fetch_again_when_the_clock_returns_no_time() {
        Single<Long> noTime = org.mockito.Mockito.mock(Single.class);
        when(noTime.blockingGet()).thenReturn(null);
        when(distributedEventRepository.serverTime()).thenReturn(noTime);
        when(distributedEventRepository.searchAll(any(), eq(1000L))).thenReturn(Flowable.empty());

        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of()).test().assertComplete();
        cut.fetchLatest(1_000L, 2_000L, DistributedEventType.API, Set.of()).test().assertComplete();

        verify(distributedEventRepository, times(2)).searchAll(any(), eq(1000L));
    }
}
