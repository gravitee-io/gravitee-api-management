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

import io.gravitee.repository.distributedsync.api.DistributedEventRepository;
import io.gravitee.repository.distributedsync.api.search.DistributedEventCriteria;
import io.gravitee.repository.distributedsync.model.DistributedEvent;
import io.gravitee.repository.distributedsync.model.DistributedEventType;
import io.gravitee.repository.distributedsync.model.DistributedSyncAction;
import io.reactivex.rxjava3.core.Flowable;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DistributedEventFetcher {

    private final DistributedEventRepository distributedEventRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public Flowable<DistributedEvent> fetchLatest(
        final Long from,
        final Long to,
        final DistributedEventType type,
        final Set<DistributedSyncAction> syncActions
    ) {
        AtomicBoolean lastPage = new AtomicBoolean();
        AtomicLong page = new AtomicLong(0L);
        DistributedEventCriteria distributedEventCriteria = DistributedEventCriteria.builder()
            .from(from == null ? -1 : from - TIMEFRAME_DELAY)
            .to(to == null ? -1 : to + TIMEFRAME_DELAY)
            .type(type)
            .syncActions(syncActions)
            .build();
        return Flowable.just(page)
            .map(AtomicLong::getAndIncrement)
            .flatMap(nextPage -> distributedEventRepository.search(distributedEventCriteria, nextPage, (long) bulkItems))
            .switchIfEmpty(Flowable.fromAction(() -> lastPage.set(true)))
            .repeatUntil(lastPage::get);
    }
}
