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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Set;
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
        cut = new DistributedEventFetcher(distributedEventRepository, clusterManager, 1);
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
        cut = new DistributedEventFetcher(distributedEventRepository, clusterManager, 50_000);
        when(distributedEventRepository.searchAll(any(), eq(50_000L))).thenReturn(Flowable.empty());
        cut.fetchLatest(null, null, DistributedEventType.API, Set.of()).test().assertComplete();
    }

    @Test
    void should_reject_non_positive_bulk_items() {
        assertThatThrownBy(() -> new DistributedEventFetcher(distributedEventRepository, clusterManager, 0)).isInstanceOf(
            IllegalArgumentException.class
        );
    }
}
