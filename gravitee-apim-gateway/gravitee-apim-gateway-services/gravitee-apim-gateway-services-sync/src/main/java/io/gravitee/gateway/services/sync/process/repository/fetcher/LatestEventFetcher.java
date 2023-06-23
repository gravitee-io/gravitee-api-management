/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class LatestEventFetcher {

    private final EventLatestRepository eventLatestRepository;

    @Getter
    @Accessors(fluent = true)
    private final int bulkItems;

    public Flowable<List<Event>> fetchLatest(
        Long from,
        Long to,
        Event.EventProperties group,
        List<String> environments,
        Set<EventType> eventTypes
    ) {
        return Flowable.<List<Event>, EventPageable>generate(
            () ->
                EventPageable
                    .builder()
                    .index(0)
                    .size(bulkItems)
                    .criteria(
                        EventCriteria
                            .builder()
                            .types(eventTypes)
                            .from(from == null ? -1 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                            .to(to == null ? -1 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                            .environments(environments)
                            .build()
                    )
                    .build(),
            (page, emitter) -> {
                try {
                    List<Event> events = eventLatestRepository.search(page.criteria, group, page.index, page.size);
                    if (events != null && !events.isEmpty()) {
                        emitter.onNext(events);
                        page.index++;
                    }
                    if (events == null || events.size() < page.size) {
                        emitter.onComplete();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        );
    }

    @Builder
    @AllArgsConstructor
    @Getter
    private static class EventPageable {

        private long index;
        private long size;
        private EventCriteria criteria;
    }
}
