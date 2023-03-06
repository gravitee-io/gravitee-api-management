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
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_AFTER_DELAY;
import static io.gravitee.gateway.services.sync.SyncManager.TIMEFRAME_BEFORE_DELAY;

import io.gravitee.common.service.AbstractService;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractSynchronizer extends AbstractService<Synchronizer> implements Synchronizer {

    public static final int DEFAULT_BULK_SIZE = 100;

    protected EventLatestRepository eventLatestRepository;

    protected int bulkItems;

    protected AbstractSynchronizer(EventLatestRepository eventLatestRepository, int bulkItems) {
        this.eventLatestRepository = eventLatestRepository;
        this.bulkItems = bulkItems;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
    }

    protected Flowable<Event> searchLatestEvents(
        Long from,
        Long to,
        Event.EventProperties group,
        List<String> environments,
        EventType... eventTypes
    ) {
        return Flowable.create(
            emitter -> {
                try {
                    int size = getBulkSize();
                    long page = 0;
                    EventCriteria.Builder criteriaBuilder = new EventCriteria.Builder()
                        .types(eventTypes)
                        .from(from == null ? 0 : from - TIMEFRAME_BEFORE_DELAY)
                        .to(to == null ? 0 : to + TIMEFRAME_AFTER_DELAY)
                        .environments(environments);

                    List<Event> events;

                    do {
                        events = eventLatestRepository.search(criteriaBuilder.build(), group, page, (long) size);
                        events.forEach(emitter::onNext);
                        page++;
                    } while (!events.isEmpty() && events.size() == size);

                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(e);
                }
            },
            BackpressureStrategy.BUFFER
        );
    }

    public int getBulkSize() {
        return bulkItems > 0 ? bulkItems : DEFAULT_BULK_SIZE;
    }
}
