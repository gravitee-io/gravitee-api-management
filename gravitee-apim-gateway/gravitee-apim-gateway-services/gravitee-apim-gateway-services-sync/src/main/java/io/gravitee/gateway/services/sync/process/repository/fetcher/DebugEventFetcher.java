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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import io.gravitee.gateway.services.sync.process.repository.DefaultSyncManager;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class DebugEventFetcher {

    private final EventRepository eventRepository;
    private final Node node;

    public Flowable<List<Event>> fetchLatest(final Long from, final Long to, final Set<String> environments) {
        return Flowable.generate(emitter -> {
            EventCriteria eventCriteria = EventCriteria.builder()
                .type(EventType.DEBUG_API)
                .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
                .property(Event.EventProperties.GATEWAY_ID.getValue(), node.id())
                .from(from == null ? 0 : from - DefaultSyncManager.TIMEFRAME_DELAY)
                .to(to == null ? 0 : to + DefaultSyncManager.TIMEFRAME_DELAY)
                .environments(environments)
                .build();
            try {
                List<Event> events = eventRepository.search(eventCriteria);
                if (events != null && !events.isEmpty()) {
                    emitter.onNext(events);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}
