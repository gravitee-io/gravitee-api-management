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

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiSynchronizer extends AbstractSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(DebugApiSynchronizer.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Node node;

    @Override
    public void synchronize(Long lastRefreshAt, Long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        if (lastRefreshAt != -1) {
            EventCriteria.Builder criteriaBuilder = new EventCriteria.Builder()
                .types(EventType.DEBUG_API)
                .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
                .from(lastRefreshAt == null ? 0 : lastRefreshAt - TIMEFRAME_BEFORE_DELAY)
                .to(nextLastRefreshAt == null ? 0 : nextLastRefreshAt + TIMEFRAME_AFTER_DELAY)
                .environments(environments);

            List<String> events = eventRepository
                .search(criteriaBuilder.build())
                .stream()
                .filter(event -> node.id().equals(event.getProperties().get(Event.EventProperties.GATEWAY_ID.getValue())))
                .map(
                    event -> {
                        eventManager.publishEvent(ReactorEvent.DEBUG, new ReactableWrapper(event));
                        return event.getId();
                    }
                )
                .collect(Collectors.toList());
            logger.debug("{} debug apis synchronized in {}ms", events.size(), (System.currentTimeMillis() - start));
        }
    }
}
