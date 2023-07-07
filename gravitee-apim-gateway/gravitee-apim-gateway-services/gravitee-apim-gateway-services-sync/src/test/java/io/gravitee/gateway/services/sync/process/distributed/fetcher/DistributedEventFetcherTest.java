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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedEventFetcherTest {

    @Mock
    private DistributedEventRepository distributedEventRepository;

    private DistributedEventFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DistributedEventFetcher(distributedEventRepository, 1);
    }

    @Test
    void should_fetch_latest_event() {
        DistributedEvent distributedEvent = DistributedEvent.builder().build();
        when(distributedEventRepository.search(any(), any(), any()))
            .thenReturn(Flowable.just(distributedEvent))
            .thenReturn(Flowable.empty());
        cut.fetchLatest(null, null, DistributedEventType.API, Set.of()).test().assertValueCount(1).assertValue(distributedEvent);
    }

    @Test
    void should_fetch_latest_event_and_complete_when_no_more_page() {
        cut = new DistributedEventFetcher(distributedEventRepository, 1);
        DistributedEvent distributedEvent1 = DistributedEvent.builder().id("1").build();
        DistributedEvent distributedEvent2 = DistributedEvent.builder().id("2").build();
        when(distributedEventRepository.search(any(), eq(0L), eq(1L))).thenReturn(Flowable.just(distributedEvent1));
        when(distributedEventRepository.search(any(), eq(1L), eq(1L))).thenReturn(Flowable.just(distributedEvent2));
        when(distributedEventRepository.search(any(), eq(2L), eq(1L))).thenReturn(Flowable.empty());
        cut
            .fetchLatest(null, null, DistributedEventType.API, Set.of())
            .test()
            .assertValueAt(0, distributedEvent1)
            .assertValueAt(1, distributedEvent2)
            .assertComplete();
    }
}
