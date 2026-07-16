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
package io.gravitee.repository.distributedsync.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DistributedEventRepositorySearchAllTest {

    private final AtomicInteger searchCalls = new AtomicInteger();

    private DistributedEventRepository repositoryOf(final List<DistributedEvent> events) {
        return new DistributedEventRepository() {
            @Override
            public Flowable<DistributedEvent> search(DistributedEventCriteria criteria, Long page, Long size) {
                searchCalls.incrementAndGet();
                return Flowable.fromIterable(events.stream().skip(page * size).limit(size).toList());
            }

            @Override
            public Completable createOrUpdate(DistributedEvent distributedEvent) {
                return Completable.complete();
            }

            @Override
            public Completable updateAll(
                String clusterId,
                DistributedEventType refType,
                String refId,
                DistributedSyncAction syncAction,
                Date updateAt
            ) {
                return Completable.complete();
            }
        };
    }

    @Test
    void should_page_through_all_events_until_an_empty_page() {
        List<DistributedEvent> events = IntStream.range(0, 5)
            .mapToObj(i -> DistributedEvent.builder().id(String.valueOf(i)).build())
            .toList();

        repositoryOf(events).searchAll(null, 2L).test().assertValueSequence(events).assertComplete();

        // pages of 2: [0,1], [2,3], [4] and the empty page ending the loop
        assertThat(searchCalls).hasValue(4);
    }

    @Test
    void should_complete_without_event_when_nothing_matches() {
        repositoryOf(List.of()).searchAll(null, 2L).test().assertNoValues().assertComplete();

        assertThat(searchCalls).hasValue(1);
    }
}
